package gov.va.med.srcalc.vista;

import java.io.StringReader;

import gov.va.med.srcalc.domain.HealthFactor;
import gov.va.med.srcalc.domain.Patient;
import gov.va.med.srcalc.domain.ReferenceNotes;
import gov.va.med.srcalc.domain.VistaLabs;
import gov.va.med.srcalc.domain.calculation.RetrievedValue;
import gov.va.med.srcalc.vista.vistalink.VistaLinkUtil;

import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.xml.sax.InputSource;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

/**
 * Implementation of {@link VistaPatientDao} using remote procedures. Each
 * instance is tied to a particular user to avoid having to specify the user
 * when calling each method.
 */
public class RpcVistaPatientDao implements VistaPatientDao
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcVistaPatientDao.class);
    private static final String NO_WEIGHT = "0^NO WEIGHT ENTERED WITHIN THIS PERIOD";
    private static final String VITALS_SPLIT_REGEX = "[\\s]+";
    private static final String WEIGHT_UNITS = "lbs.";
    private static final String HEIGHT_UNITS = "inches";
    private static final String ADL_ENTERPRISE_TITLE = "NURSING ADMISSION EVALUATION NOTE";
    
    /**
     * The expected date format for information received from VistA.
     */
    public static final SimpleDateFormat VISTA_DATE_OUTPUT_FORMAT = new SimpleDateFormat("MM/dd/yy@HH:mm");
    
    private static final ImmutableMap<String, Patient.Gender> TRANSLATION_MAP = ImmutableMap.of(
                    "M", Patient.Gender.Male,
                    "F", Patient.Gender.Female);
    
    private final VistaProcedureCaller fProcedureCaller;
    
    private final String fDuz;
    
    /**
     * Constructs an instance.
     * 
     * @param procedureCaller for making the procedure calls
     * @param duz the user DUZ under which to perform the procedure calls. Must identify a
     * valid VistA user.
     */
    public RpcVistaPatientDao(
            final VistaProcedureCaller procedureCaller, final String duz)
    {
        fProcedureCaller = procedureCaller;
        fDuz = duz;
    }
    
    /**
     * {@inheritDoc}
     * <p>This method will eager load all available information about the patient including vitals,
     * basic information, and lab results. Eager loading is done to sacrifice efficiency for
     * simplicity. The number of eager remote procedure calls is comparable to VistA CPRS.</p>
     * @param dfn the dfn identifying the specified patient
     */
    @Override
    public Patient getPatient(final int dfn)
    {
        try
        {
            final List<String> basicResults = fProcedureCaller.doRpc(
                    fDuz, RemoteProcedure.GET_PATIENT, String.valueOf(dfn));
            final List<String> vitalResults = fProcedureCaller.doRpc(
                    fDuz, RemoteProcedure.GET_RECENT_VITALS, String.valueOf(dfn));
            // Fields are separated by '^'
            // Basic patient demographics (age, gender)
            final List<String> basicArray = Splitter.on('^').splitToList(basicResults.get(0));
            final String patientName = basicArray.get(0);
            final int patientAge = Integer.parseInt(basicArray.get(1));
            final Patient.Gender patientGender = translateFromVista(basicArray.get(2));
            final Patient patient = new Patient(dfn, patientName, patientGender, patientAge);
            // Patient vitals information (including but not limited to BMI, height,
            // weight, weight 6 months ago). If there are no results, a single line with
            // an error message is returned.
            LOGGER.debug("Patient Vital Results: {}", vitalResults);
            if (vitalResults.size() > 1)
            {
                // Parse the returned data and put it into the patient data
                // This will include the most recent height, current weight, and BMI
                parseRecentVitalResults(patient, vitalResults);
            }
            
            // We have to get the current weight before we do this
            // If there was no current weight, no need to retrieve other weight
            if (patient.getWeight() != null)
            {
                final List<String> weightResults = retrieveWeight6MonthsAgo(patient);
                LOGGER.debug("Weight Results: {}", weightResults);
                // A line begging with "0^NO" means that no results were retrieved
                // The actual line varies depending on the vital requested.
                if (weightResults.size() > 0 && !weightResults.get(0).equals(NO_WEIGHT))
                {
                    LOGGER.debug("Patient Vital Results: {}", weightResults);
                    // Parse the returned data and put it into the patient data
                    // This includes weight and BMI currently.
                    parseWeightResults(patient, weightResults);
                }
            }
            
            // Retrieve all labs from VistA
            retrieveLabs(dfn, patient);
            // Retrieve all health factors in the last year from VistA and filter
            // by the list given to us by the NSO.
            retrieveHealthFactors(dfn, patient);
            // Retrieve only medications with the "Active" status and not "Pending"
            retrieveActiveMedications(dfn, patient);
            // Retrieve the patient's nursing notes from VistA
            retrieveAdlNotes(dfn, patient);
            // Retrieve any notes with DNR in the title.
            retrieveDnrNotes(dfn, patient);
            
            LOGGER.debug("Loaded {} from VistA.", patient);
            return patient;
        }
        catch (final GeneralSecurityException e)
        {
            // Translate Exception per method contract. The below block could handle this,
            // but is clearer.
            throw new PermissionDeniedDataAccessException("VistA security error", e);
        }
        catch (final Exception e)
        {
            // There are many DataAccessExceptions, but this seems like the most
            // appropriate exception to throw here.
            throw new NonTransientDataAccessResourceException(e.getMessage(), e);
        }
    }

    private static Patient.Gender translateFromVista(final String vistaField)
    {
        if(TRANSLATION_MAP.containsKey(vistaField))
        {
            return TRANSLATION_MAP.get(vistaField);
        }
        return Patient.Gender.Unknown;
    }
    
    private List<String> retrieveWeight6MonthsAgo(final Patient patient)
            throws GeneralSecurityException
    {
        // Our range for weight 6 months ago is 3-12 months prior to the
        // most recent weight.
        final Calendar cal = Calendar.getInstance();
        cal.setTime(patient.getWeight().getMeasureDate());
        cal.add(Calendar.MONTH, -6);
        final String endDateString = String.format("%03d%02d%02d", (cal.get(Calendar.YEAR) - 1700),
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        cal.setTime(patient.getWeight().getMeasureDate());
        cal.add(Calendar.YEAR, -1);
        final String startDateString = String.format("%03d%02d%02d", (cal.get(Calendar.YEAR) - 1700),
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        final String rpcParameter =
                String.valueOf(patient.getDfn()) + "^" + endDateString + "^WT^" + startDateString;
        LOGGER.debug("Weight 6 Months Ago Parameter: {}", rpcParameter);
        return fProcedureCaller.doRpc(fDuz, RemoteProcedure.GET_VITAL, rpcParameter);
    }
    
    private void parseWeightResults(
            final Patient patient, final List<String> weightResults)
            throws ParseException
    {
        /*-
         * The last entries are the most recent so we use those. Get the most recent
         * weight measurement within the already specified range. The format expected from
         * VistA is:
         * 
         * 21557^04/17/09@12:00 Wt: 185.00 lb (84.09 kg) _NURSE,ONE
         *        @12:00 Body Mass Index: 25.86
         * 22296^08/24/09@14:00 Wt: 190.00 lb (86.36 kg) _NURSE,ONE
         *        @14:00 Body Mass Index: 26.56
         *        
         * Where the most recent weight is the last result and each result consists of two
         * lines. The first line is a measurement identifier, the date and time, the
         * weight in pounds and kilograms and the person providing the measurement. The
         * second line is the time on the same date as the weight measurement, along with
         * the BMI for the patient.
         */
        final List<String> weightLineTokens = Splitter.on(Pattern.compile("[\\s\\^]+"))
                .splitToList(weightResults.get(weightResults.size()-2));
        // Get the date of the measurement
        LOGGER.debug("weight line tokens: {}", weightResults);
        final Date measurementDate = VISTA_DATE_OUTPUT_FORMAT.parse(weightLineTokens.get(1));
        patient.setWeight6MonthsAgo(new RetrievedValue(
                Double.parseDouble(weightLineTokens.get(3)), measurementDate, WEIGHT_UNITS));
        LOGGER.debug("Weight 6 months ago: {}", patient.getWeight6MonthsAgo());
    }
    
    private void parseRecentVitalResults(final Patient patient, final List<String> vitalResults)
            throws ParseException
    {
        // The date inside of returned vitals is inside of parentheses.
        // For example, pulse is returned as: "Pulse:       (03/05/10@09:00)  74  _NURSE,ONE_Vitals"
        final SimpleDateFormat dateFormat = new SimpleDateFormat(
                "(" + VISTA_DATE_OUTPUT_FORMAT.toPattern() + ")");
        final Pattern compliedPattern = Pattern.compile(VITALS_SPLIT_REGEX);
        // Each entry comes with an accompanying date and time.
        final List<String> heightLineTokens = Splitter.on(compliedPattern).splitToList(vitalResults.get(5));
        final int feet = Integer.parseInt(heightLineTokens.get(2));
        patient.setHeight(new RetrievedValue(
                (feet * 12.0) + Double.parseDouble(heightLineTokens.get(4)),
                dateFormat.parse(heightLineTokens.get(1)),
                HEIGHT_UNITS));
        final List<String> weightLineTokens = Splitter.on(compliedPattern).splitToList(vitalResults.get(6));
        patient.setWeight(new RetrievedValue(
                Double.parseDouble(weightLineTokens.get(2)),
                dateFormat.parse(weightLineTokens.get(1)),
                WEIGHT_UNITS));
        final List<String> bmiLineTokens = Splitter.on(compliedPattern).splitToList(vitalResults.get(7));
        // The BMI value is the second to last token on its line
        patient.setBmi(new RetrievedValue(
            Double.parseDouble(bmiLineTokens.get(bmiLineTokens.size()-2)),
            patient.getWeight().getMeasureDate(),
            ""));
    }
    
    private void retrieveLabs(final int dfn, final Patient patient) throws ParseException
    {
        final VistaLabs[] enumValues = VistaLabs.values();
        for(final VistaLabs labRetrievalEnum: enumValues)
        {
            try
            {
                final String rpcResultString = fProcedureCaller.doRetrieveLabsCall(
                        fDuz,
                        String.valueOf(dfn),
                        labRetrievalEnum.getPossibleLabNames());
                // If the resultString is a success, add it to the patient's lab data.
                // Else, we don't need to do anything.
                if(!rpcResultString.isEmpty())
                {
                    List<String> rpcSplit = Splitter.on('^').splitToList(rpcResultString);
                    final double labValue = Double.parseDouble(rpcSplit.get(1));
                    final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy@HH:mm:ss");
                    patient.getLabs().put(labRetrievalEnum,
                            new RetrievedValue(labValue, format.parse(rpcSplit.get(2)), rpcSplit.get(3)));
                }
            }
            catch(final Exception e)
            {
                // If an exception occurs for any reason, move to the next lab so that as much patient
                // data as possible can still be retrieved.
                LOGGER.warn("Unable to retrieve lab {}. {}", labRetrievalEnum.name(), e.toString());
            }
        }
    }

    private void retrieveHealthFactors(final int dfn, final Patient patient)
    {
        try
        {
            patient.getHealthFactors().clear();
            final List<String> rpcResults = fProcedureCaller.doRpc(
                    fDuz, RemoteProcedure.GET_HEALTH_FACTORS, String.valueOf(dfn));
            // Now that we have all of the health factors, filter out any that are not present
            // in the list provided by the NSO.
            final Iterator<String> iter = rpcResults.iterator();
            final DateTimeFormatter format = DateTimeFormat.forPattern("MM/dd/yy");
            while(iter.hasNext())
            {
                final String healthFactor = iter.next();
                final String[] factorSplitArray = healthFactor.split("\\^");
                if(HEALTH_FACTORS_SET.contains(factorSplitArray[1]))
                {
                    patient.getHealthFactors().add(new HealthFactor(
                            format.parseLocalDate(factorSplitArray[0]),
                            factorSplitArray[1]));
                }
            }
            LOGGER.debug("Retrieved Health factors: {} ", patient.getHealthFactors());
        }
        catch(final Exception e)
        {
            LOGGER.warn("Unable to retrieve health factors. {}", e);
        }
    }
    
    private void retrieveActiveMedications(final int dfn, final Patient patient)
    {
        try
        {
            patient.getActiveMedications().clear();
            final List<String> rpcResults = fProcedureCaller.doRpc(
                    fDuz, RemoteProcedure.GET_ACTIVE_MEDICATIONS, String.valueOf(dfn));
            for(final String medResult: rpcResults)
            {
                // The expected format is "<identifier>^<medication name>^<date>^^^<dose per day>"
                // for example, "403962R;O^METOPROLOL TARTRATE 50MG TAB^3110228^^^3"
                final List<String> medInfo = Splitter.on('^').splitToList(medResult);
                patient.getActiveMedications().add(medInfo.get(1));
            }
            LOGGER.debug("Retrieved Active Medications: {} ", patient.getActiveMedications());
        }
        catch(final Exception e)
        {
            LOGGER.warn("Unable to retrieve active medications. {}", e);
        }
    }
    
    private void retrieveAdlNotes(final int dfn, final Patient patient)
    {
        try
        {
            final List<String> rpcResults = fProcedureCaller.doRpc(
                    fDuz,
                    RemoteProcedure.GET_ADL_STATUS,
                    String.valueOf(dfn),
                    ADL_ENTERPRISE_TITLE);
            // If the resultString is a success, add it to the patient's adl notes.
            // Else, we don't need to do anything.
            if(!rpcResults.isEmpty())
            {
                final ReferenceNotes adlNotes = getReferenceNotes(rpcResults);
                patient.getAdlNotes().clear();
                patient.getAdlNotes().addAll(adlNotes.getAllNotes());
            }
        }
        catch(final Exception e)
        {
            // If an exception occurs for any reason, log a warning but allow the application
            // to continue without failure.
            LOGGER.warn("Unable to retrieve patient's ADL status. {}", e);
        }
    }
    
    private void retrieveDnrNotes(final int dfn, final Patient patient)
    {
        try
        {
            final List<String> rpcResults = fProcedureCaller.doRpc(
                    fDuz,
                    RemoteProcedure.GET_NOTES_WITH_SUBSTRING,
                    String.valueOf(dfn),
                    "DNR");
            // If the resultString is a success, add it to the patient's dnr notes.
            // Else, we don't need to do anything.
            if(!rpcResults.isEmpty())
            {
                final ReferenceNotes dnrNotes = getReferenceNotes(rpcResults);
                patient.getDnrNotes().clear();
                patient.getDnrNotes().addAll(dnrNotes.getAllNotes());
            }
        }
        catch(final Exception e)
        {
            // If an exception occurs for any reason, log a warning but allow the application
            // to continue without failure.
            LOGGER.warn("Unable to retrieve patient's DNR notes. {}", e);
        }
    }
    
    /**
     * Translates the results of the RPC call into a {@link ReferenceNotes} object.
     * @param rpcResults the original results returned from the RPC. In order for this method
     *          to translate the results properly, they need to be valid XML.
     * @throws JAXBException if there is a problem unmarshalling the input
     */
    private ReferenceNotes getReferenceNotes(final List<String> rpcResults) throws JAXBException
    {
        // Parse the String as XML and format it into separate notes
        final InputSource input = new InputSource();
        input.setCharacterStream(new StringReader(Joiner.on("\n").join(rpcResults)));
        final JAXBContext context = JAXBContext.newInstance(ReferenceNotes.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        return (ReferenceNotes) unmarshaller.unmarshal(input);
    }
    
    @Override
    public SaveNoteCode saveRiskCalculationNote(final int patientDfn, final String electronicSignature,
            final String noteBody)
    {
        // Split on line feed or carriage return
        // Wrap any lines that are too long so that users do not have to
        // scroll when viewing the note in CPRS.
        final List<String> bodyArray = Splitter.on(Pattern.compile("\\r?\\n")).splitToList(noteBody);
        final StringBuilder wrappedNote = new StringBuilder();
        for (final String line : bodyArray)
        {
            wrappedNote.append(WordUtils.wrap(line, VistaPatientDao.MAX_LINE_LENGTH, "\n    ", false) + "\n");
        }
        try
        {
            final String rpcResultString = fProcedureCaller.doSaveProgressNoteCall(fDuz,
                    VistaLinkUtil.encrypt(electronicSignature), String.valueOf(patientDfn),
                    // Use Guava Splitter to get a List.
                    Splitter.on('\n').splitToList(wrappedNote));
            
            final VistaOperationResult rpcResult = VistaOperationResult.fromString(rpcResultString);
            if (rpcResult.getCode().equals("1"))
            {
                return SaveNoteCode.SUCCESS;
            }
            else
            {
                return SaveNoteCode.INVALID_SIGNATURE;
            }
        }
        catch (final Exception e)
        {
            // An Exception means an invalid DUZ or a problem with VistALink/VistA
            // Translate the exception into a status message
            // We've tested that this works so any failure at this point is probably recoverable.
            throw new RecoverableDataAccessException(e.getMessage(), e);
        }
    }
}
