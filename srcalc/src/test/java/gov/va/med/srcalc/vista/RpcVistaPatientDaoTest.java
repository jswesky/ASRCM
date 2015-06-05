package gov.va.med.srcalc.vista;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import gov.va.med.srcalc.domain.*;
import gov.va.med.srcalc.domain.calculation.RetrievedValue;
import gov.va.med.srcalc.vista.VistaPatientDao.SaveNoteCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.joda.time.DateTime;
import org.junit.Test;

public class RpcVistaPatientDaoTest
{
    private final static String RADIOLOGIST_DUZ = "11716";
    private final static String PATIENT_RPC_RETURN = "TESTPATIENT^50^M";
    private final static String INVALID_SIGNATURE_RETURN = "0^Incorrect Electronic Signature Code Entered.";
    private final static String ELECTRONIC_SIGNATURE = "TESTSIG";
    private final static String DUMMY_BODY = "Note Body";
    private final static String ALBUMIN_SUCCESS = "ALBUMIN^3.0^02/02/2015@14:35:12^g/dl";
    private final static String INVALID_LAB = "This is invalid. ~@!#$";
    private final static String VALID_LAB_NO_UNITS = "ALBUMIN^3.0^02/02/2015@14:35:12^";
    
    private final static int PATIENT_DFN = 500;

    private static VistaProcedureCaller mockVistaProcedureCaller()
    {
        final VistaProcedureCaller caller = mock(VistaProcedureCaller.class);
        // Anything besides a valid measurement is returned as an empty string
        when(caller.doRetrieveLabs(eq(RADIOLOGIST_DUZ), eq(String.valueOf(PATIENT_DFN)), anyListOf(String.class)))
            .thenReturn("");
        // Setup the necessary actions for getting patient data.
        when(caller.doRpc(RADIOLOGIST_DUZ, RemoteProcedure.GET_PATIENT, String.valueOf(PATIENT_DFN)))
            .thenReturn(Arrays.asList(PATIENT_RPC_RETURN));
        // Return empty vitals
        when(caller.doRpc(RADIOLOGIST_DUZ, RemoteProcedure.GET_RECENT_VITALS, String.valueOf(PATIENT_DFN)))
            .thenReturn(new ArrayList<String>());
        when(caller.doRpc(RADIOLOGIST_DUZ, RemoteProcedure.GET_VITAL, ""))
            .thenReturn(new ArrayList<String>());
        return caller;
    }
    
    @Test
    public final void testLoadVistaPatientValid()
    {
        // Setup
        final VistaProcedureCaller caller = mockVistaProcedureCaller();
        
        final RpcVistaPatientDao dao = new RpcVistaPatientDao(caller, RADIOLOGIST_DUZ);

        // Behavior verification
        final Patient patient = dao.getPatient(PATIENT_DFN);
        assertEquals(PATIENT_DFN, patient.getDfn());
        assertEquals("TESTPATIENT", patient.getName());
        assertEquals("Male", patient.getGender());
        assertEquals(50, patient.getAge());
    }
    
    @Test
    public final void testSaveNoteInvalidSignature() throws Exception
    {
    	final VistaProcedureCaller caller = mockVistaProcedureCaller();
        when(caller.doSaveProgressNoteCall(anyString(), anyString(), anyString(), anyListOf(String.class)))
            .thenReturn(INVALID_SIGNATURE_RETURN);
        final RpcVistaPatientDao dao = new RpcVistaPatientDao(caller, RADIOLOGIST_DUZ);
    	
    	// The note body being used here should not matter since the doRpc() call is being
    	// mocked and is told what to return.
    	assertEquals(SaveNoteCode.INVALID_SIGNATURE,
    			dao.saveRiskCalculationNote(PATIENT_DFN, "BadSig", DUMMY_BODY));
    	assertEquals(SaveNoteCode.INVALID_SIGNATURE,
    			dao.saveRiskCalculationNote(PATIENT_DFN, "AlsoBad", DUMMY_BODY));
    }
    
    @Test
    public final void testSaveNoteSuccess() throws Exception
    {
    	final VistaProcedureCaller caller = mockVistaProcedureCaller();
        when(caller.doSaveProgressNoteCall(anyString(), anyString(), anyString(), anyListOf(String.class)))
            .thenReturn(RemoteProcedure.VALID_SIGNATURE_RETURN);
        final RpcVistaPatientDao dao = new RpcVistaPatientDao(caller, RADIOLOGIST_DUZ);
    	
    	assertEquals(
    	        SaveNoteCode.SUCCESS,
    	        dao.saveRiskCalculationNote(PATIENT_DFN, ELECTRONIC_SIGNATURE, DUMMY_BODY));
    }
    
    @Test
    public final void testLabRetrievalFailure() throws Exception
    {
        final VistaProcedureCaller caller = mockVistaProcedureCaller();
        final RpcVistaPatientDao dao = new RpcVistaPatientDao(caller, RADIOLOGIST_DUZ);
        // Behavior verification
        final Patient patient = dao.getPatient(PATIENT_DFN);
        assertEquals(new HashMap<String, RetrievedValue>(), patient.getLabs());
    }
    
    @Test
    public final void testLabRetrievalSuccess() throws Exception
    {
        final VistaProcedureCaller caller = mockVistaProcedureCaller();
        when(caller.doRetrieveLabs(
                RADIOLOGIST_DUZ, 
                String.valueOf(PATIENT_DFN),
                VistaLabs.ALBUMIN.getPossibleLabNames()))
            .thenReturn(ALBUMIN_SUCCESS);
        final RpcVistaPatientDao dao = new RpcVistaPatientDao(caller, RADIOLOGIST_DUZ);
        // Behavior verification
        final Patient patient = dao.getPatient(PATIENT_DFN);
        assertEquals(1, patient.getLabs().size());
        final RetrievedValue value = patient.getLabs().get("ALBUMIN");
        assertEquals(3.0, value.getValue(), .0001);
        final DateTime expectedTime = new DateTime(2015, 2, 2, 14, 35, 12, 0);

        assertEquals(expectedTime.toDate(), value.getMeasureDate());
    }
    
    @Test
    public final void testInvalidLabResponse()
    {
        final VistaProcedureCaller caller = mockVistaProcedureCaller();
        when(caller.doRetrieveLabs(
                RADIOLOGIST_DUZ, 
                String.valueOf(PATIENT_DFN),
                VistaLabs.ALBUMIN.getPossibleLabNames()))
            .thenReturn(INVALID_LAB);
        final RpcVistaPatientDao dao = new RpcVistaPatientDao(caller, RADIOLOGIST_DUZ);
        // Behavior verification
        final Patient patient = dao.getPatient(PATIENT_DFN);
        // Insure the lab was not added and no exceptions occurred.
        assertEquals(0, patient.getLabs().size());
        // Insure other information was still added.
        assertEquals(patient.getAge(), 50);
        assertEquals(patient.getName(), "TESTPATIENT");
    }
    
    @Test
    public final void testLabResponseNoUnits()
    {
        final VistaProcedureCaller caller = mockVistaProcedureCaller();
        when(caller.doRetrieveLabs(
                RADIOLOGIST_DUZ, 
                String.valueOf(PATIENT_DFN),
                VistaLabs.ALBUMIN.getPossibleLabNames()))
            .thenReturn(VALID_LAB_NO_UNITS);
        final RpcVistaPatientDao dao = new RpcVistaPatientDao(caller, RADIOLOGIST_DUZ);
        // Behavior verification
        final Patient patient = dao.getPatient(PATIENT_DFN);
        // Insure the lab was not added and no exceptions occurred.
        assertEquals(1, patient.getLabs().size());
    }
}
