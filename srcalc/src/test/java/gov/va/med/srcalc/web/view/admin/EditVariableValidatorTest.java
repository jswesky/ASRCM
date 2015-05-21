package gov.va.med.srcalc.web.view.admin;

import static org.junit.Assert.*;
import gov.va.med.srcalc.domain.model.*;
import gov.va.med.srcalc.service.MockModelService;
import gov.va.med.srcalc.test.util.TestHelpers;

import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

public final class EditVariableValidatorTest
{
    /**
     * Instantiates a basic {@link EditVariable} instance.
     */
    private EditVariable makeEditVariable()
    {
        // Use EditBooleanVariable as a basic implementation of EditVariable.
        return new EditBooleanVariable(
                SampleModels.dnrVariable(), new MockModelService());
    }
    
    /**
     * Validates the given object and returns the binding result.
     */
    private BeanPropertyBindingResult validate(final EditVariable ev)
    {
        final BeanPropertyBindingResult errors =
                new BeanPropertyBindingResult(ev, "variable");
        new EditVariableValidator().validate(ev, errors);
        return errors;
    }
    
    @Test
    public final void testValid()
    {
        final BindingResult errors = validate(makeEditVariable());

        assertEquals("error count", 0, errors.getErrorCount());
    }
    
    @Test
    public final void testKeyTooLong()
    {
        final EditVariable ev = makeEditVariable();
        ev.setKey(TestHelpers.stringOfLength(Variable.KEY_MAX + 1));
        final BindingResult errors = validate(ev);
        
        assertEquals(1, errors.getErrorCount());
        assertEquals(
                EditVariableValidator.ERROR_TOO_LONG,
                errors.getFieldError("key").getCode());
    }
    
    @Test
    public final void testKeyInvalidCharacters()
    {
        final EditVariable ev = makeEditVariable();
        ev.setKey("foo!");
        final BindingResult errors = validate(ev);
        
        assertEquals("error count", 1, errors.getErrorCount());
        assertEquals(
                EditVariableValidator.ERROR_INVALID_CONTENTS,
                errors.getFieldError("key").getCode());
    }

    @Test
    public final void testDisplayNameTooLong()
    {
        final EditVariable ev = makeEditVariable();
        ev.setDisplayName(TestHelpers.stringOfLength(Variable.DISPLAY_NAME_MAX + 1));
        final BindingResult errors = validate(ev);
        
        assertEquals("error count", 1, errors.getErrorCount());
        assertEquals(
                EditVariableValidator.ERROR_TOO_LONG,
                errors.getFieldError("displayName").getCode());
    }
    
    @Test
    public final void testDisplayNameInvalidCharacters()
    {
        final EditVariable ev = makeEditVariable();
        ev.setDisplayName("\t");
        final BindingResult errors = validate(ev);
        
        assertEquals("error count", 1, errors.getErrorCount());
        assertEquals(
                EditVariableValidator.ERROR_INVALID_CONTENTS,
                errors.getFieldError("displayName").getCode());
    }
    
    @Test
    public final void testInvalidGroup()
    {
        final EditVariable ev = makeEditVariable();
        ev.setGroupId(80);
        final BindingResult errors = validate(ev);
        
        assertEquals("error count", 1, errors.getErrorCount());
        assertEquals(
                EditVariableValidator.ERROR_INVALID_OPTION,
                errors.getFieldError("groupId").getCode());
    }
    
    @Test
    public final void testHelpTextTooLong()
    {
        final EditVariable ev = makeEditVariable();
        ev.setHelpText(TestHelpers.stringOfLength(Variable.HELP_TEXT_MAX + 1));
        final BindingResult errors = validate(ev);
        
        assertEquals("error count", 1, errors.getErrorCount());
        assertEquals(
                EditVariableValidator.ERROR_TOO_LONG,
                errors.getFieldError("helpText").getCode());
    }
}
