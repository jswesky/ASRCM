package gov.va.med.srcalc.vista;

import gov.va.med.srcalc.domain.Calculation;
import gov.va.med.srcalc.domain.Patient;
import gov.va.med.srcalc.domain.SampleObjects;

/**
 * A mock {@link VistaPatientDao} just for tests.
 */
public class MockVistaPatientDao implements VistaPatientDao
{
    @Override
    public Patient getPatient(final int dfn)
    {
    	return SampleObjects.dummyPatient(dfn);
    }

	@Override
	public String saveRiskCalculationNote(final Calculation calculation,
			final String electronicSignature)
	{
		return "Dummy Result";
	}
    
}
