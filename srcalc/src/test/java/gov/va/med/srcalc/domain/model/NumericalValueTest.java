package gov.va.med.srcalc.domain.model;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

public class NumericalValueTest
{
    @Test
    public final void testBasic() throws Exception
    {
        final NumericalVariable var = SampleModels.ageVariable();
        final NumericalValue val = new NumericalValue(var, 1.2f);
        // getVariable()
        assertSame(var, val.getVariable());
        // toString()
        assertThat(val.toString(), startsWith("Age = 1.20")); // may have more 0's
        // getDisplayString()
        assertEquals("1.2", val.getDisplayString());
    }
    
    @Test(expected = ValueTooHighException.class)
    public final void testValueTooHigh() throws Exception
    {
        final NumericalVariable var = SampleModels.ageVariable();
        new NumericalValue(var, 1000);
    }
    
    @Test(expected = ValueTooLowException.class)
    public final void testValueTooLow() throws Exception
    {
        final NumericalVariable var = SampleModels.ageVariable();
        new NumericalValue(var, -1);
    }
    
    @Test(expected = ValueTooHighException.class)
    public final void testNumericalUpperBoundInclusiveFail() throws Exception
    {
    	// There is an accompanying test for lower bound in {@link DiscreteNumericalValueTest}
    	final DiscreteNumericalVariable var = SampleModels.wbcVariable();
    	var.setMaxInclusive(false);
        DiscreteNumericalValue.fromNumerical(var, 50.0f);
    }
    
    @Test
    public final void testNumericalUpperBoundInclusivePass() throws Exception
    {
    	// There is an accompanying test for lower bound in {@link DiscreteNumericalValueTest}
    	final DiscreteNumericalVariable var = SampleModels.wbcVariable();
    	// Should not throw an exception
    	DiscreteNumericalValue.fromNumerical(var, 50.0f);
    }
}
