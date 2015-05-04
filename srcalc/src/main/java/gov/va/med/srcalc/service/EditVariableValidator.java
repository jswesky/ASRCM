package gov.va.med.srcalc.service;

import gov.va.med.srcalc.domain.model.AbstractVariable;

import org.springframework.validation.*;

/**
 * This Validator validates {@link EditVariable} instances.
 */
public class EditVariableValidator implements Validator
{
    @Override
    public boolean supports(Class<?> clazz)
    {
        return EditVariable.class.equals(clazz);
    }

    @Override
    public void validate(Object obj, Errors e)
    {
        // Note that all of this validation could be accomplished through
        // JSR-303 Bean Validation. I may switch over at some point.

        final EditVariable editVariable = (EditVariable)obj;
        ValidationUtils.rejectIfEmpty(e, "displayName", "displayName.empty");
        if (editVariable.getDisplayName().length() > AbstractVariable.DISPLAY_NAME_MAX)
        {
            e.rejectValue(
                    "displayName",
                    "tooLong",
                    new Object[] {AbstractVariable.DISPLAY_NAME_MAX},
                    "The display name is too long.");
        }
    }
    
}
