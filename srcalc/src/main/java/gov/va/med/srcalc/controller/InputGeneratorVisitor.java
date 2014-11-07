package gov.va.med.srcalc.controller;

import gov.va.med.srcalc.domain.variable.MultiSelectOption;
import gov.va.med.srcalc.domain.variable.MultiSelectVariable;
import gov.va.med.srcalc.domain.variable.NumericalVariable;
import gov.va.med.srcalc.domain.variable.VariableVisitor;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes an HTML input control for visited {@link Variable}s.
 */
class InputGeneratorVisitor implements VariableVisitor
{
    private final Writer fWriter;
    
    public InputGeneratorVisitor(Writer writer)
    {
        fWriter = writer;
    }
    
    protected void writeRadio(MultiSelectVariable variable) throws IOException
    {
        for (final MultiSelectOption option : variable.getOptions())
        {
            fWriter.write(String.format(
                "<label class=\"radioLabel\">" +
                    "<input type=\"radio\" name=\"%s\" value=\"%s\"> " +
                    "%s</label>",
                variable.getDisplayName(),
                option.getDisplayName(),
                option.getDisplayName()));
        }
    }
    
    protected void writeDropdown(MultiSelectVariable variable)
    {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void visitMultiSelect(MultiSelectVariable variable) throws IOException
    {
        // Display differently based on configured displayType.
        switch (variable.getDisplayType())
        {
            case Radio:
                writeRadio(variable);
                break;
            case Dropdown:
                writeDropdown(variable);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported displayType.");
        }
    }
    
    @Override
    public void visitNumerical(NumericalVariable variable) throws Exception
    {
        fWriter.write(String.format(
                "<input type=\"text\" name=\"%s\">",
                variable.getDisplayName()));
    }
}