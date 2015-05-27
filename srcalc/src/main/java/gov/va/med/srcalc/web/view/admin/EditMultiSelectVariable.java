package gov.va.med.srcalc.web.view.admin;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.collect.*;

import gov.va.med.srcalc.domain.model.*;
import gov.va.med.srcalc.service.ModelInspectionService;
import gov.va.med.srcalc.util.RetrievalEnum;
import gov.va.med.srcalc.web.view.Views;

/**
 * <p>A form backing object for creating a new or editing an existing
 * MultiSelectVariable.</p>
 * 
 * <p>This code is tightly coupled with newMultiSelectVariable.jsp.</p>
 */
public class EditMultiSelectVariable extends EditVariable
{
    private MultiSelectVariable.DisplayType fDisplayType;
    
    private final ArrayList<String> fOptions;
    
    /**
     * Constructs an instance with default values for all properties. The default
     * list of options is 3 blanks (to offer the user 3 text boxes to fill out).
     * @param modelService to provide reference data (e.g., available
     * VariableGroups) to the user
     */
    public EditMultiSelectVariable(final ModelInspectionService modelService)
    {
        super(modelService);
        fDisplayType = MultiSelectVariable.DisplayType.Radio;
        fOptions = Lists.newArrayList("", "", "");
    }
    
    @Override
    public String getTypeName()
    {
        return "Multi-Select";
    }
    
    /**
     * Returns {@link Views#NEW_MULTI_SELECT_VARIABLE}.
     */
    @Override
    public String getNewViewName()
    {
        return Views.NEW_MULTI_SELECT_VARIABLE;
    }
    
    @Override
    public ImmutableSortedSet<RetrievalEnum> getAllRetrievers()
    {
        return ImmutableSortedSet.of(RetrievalEnum.GENDER);
    }
    
    /**
     * Returns all possible display types for user selection.
     * @return an immutable collection
     */
    public final ImmutableCollection<MultiSelectVariable.DisplayType> getAllDisplayTypes()
    {
        return ImmutableList.copyOf(MultiSelectVariable.DisplayType.values());
    }
    
    /**
     * Returns the DisplayType to set on the variable.
     * @see MultiSelectVariable#getDisplayType()
     */
    public final MultiSelectVariable.DisplayType getDisplayType()
    {
        return fDisplayType;
    }
    
    /**
     * Sets the DisplayType to set on the variable.
     * @see MultiSelectVariable#getDisplayType()
     */
    public final void setDisplayType(final MultiSelectVariable.DisplayType displayType)
    {
        fDisplayType = displayType;
    }
    
    /**
     * Returns the options to set on the variable. Note that {@link
     * MultiSelectVariable}s actually have collections of {@link MultiSelectOption},
     * but since those objects effectively only contain Strings, we represent
     * them as Strings here for easy use in views.
     * @see MultiSelectVariable#getOptions()
     * @return a mutable List
     */
    public final List<String> getOptions()
    {
        return fOptions;
    }
    
    /**
     * Like {@link #getOptions()} but with trailing blanks trimmed off. (The
     * HTTP form submission may create spurious trailing blank options.) The
     * returned list may still contain empty strings in the middle.
     * @return an ImmutableList
     */
    public final ImmutableList<String> getTrimmedOptions()
    {
        // Start with a copy of all the options.
        final ArrayList<String> trimmed = new ArrayList<>(fOptions);
        
        // Iterate from the end, removing as we go.
        for (int i = fOptions.size() - 1; i >= 0; --i)
        {
            if (Strings.isNullOrEmpty(trimmed.get(i)))
            {
                trimmed.remove(i);
            }
            // Break at the first non-empty String.
            else
            {
                break;
            }
        }
        
        return ImmutableList.copyOf(trimmed);
    }
    
    /**
     * Returns the options as a List of {@link MultiSelectOption}, the type
     * needed to actually create the variable. Internally uses {@link
     * #getTrimmedOptions()} to omit any trailing blanks.
     * @return an ImmutableList
     */
    protected ImmutableList<MultiSelectOption> getMultiSelectOptions()
    {
        final ImmutableList<String> trimmed = getTrimmedOptions();
        final ArrayList<MultiSelectOption> options = new ArrayList<>(trimmed.size());
        for (final String option : trimmed)
        {
            options.add(new MultiSelectOption(option));
        }
        return ImmutableList.copyOf(options);
    }

    @Override
    public MultiSelectVariable buildNew()
    {
        final MultiSelectVariable var = new MultiSelectVariable(
                getDisplayName(),
                getGroup().get(),
                fDisplayType,
                getMultiSelectOptions(),
                getKey());
        applyBaseProperties(var);
        return var;
    }
    
    /**
     * Returns the maximum number of valid options. Note that MultiSelecVariable
     * itself does not enforce this limit, but enforce it here just because we
     * need to limit the user input somewhere.
     * @return 20
     */
    public int getMaxOptions()
    {
        return 20;
    }
    
}
