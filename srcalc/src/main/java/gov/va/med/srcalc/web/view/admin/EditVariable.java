package gov.va.med.srcalc.web.view.admin;

import java.util.*;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSortedSet;

import gov.va.med.srcalc.domain.model.*;
import gov.va.med.srcalc.service.ModelInspectionService;

/**
 * Stores basic {@link AbstractVariable} properties for creating a new or
 * editing an existing variable.
 */
public abstract class EditVariable
{
    private final ImmutableSortedSet<VariableGroup> fAllGroups;
    
    private String fKey;

    private String fDisplayName;
    
    private Optional<String> fHelpText;
    
    private int fGroupId;
    
    private final SortedSet<RiskModel> fDependentModels;
    
    /**
     * Constructs an instance with default values for all properties.
     * @param modelService to provide reference data (e.g., available
     * VariableGroups) to the user
     */
    public EditVariable(
            final ModelInspectionService modelService) 
    {
        // Sort the groups per the getAllGroups() contract.
        fAllGroups = ImmutableSortedSet.copyOf(modelService.getAllVariableGroups());
        fKey = "";
        fDisplayName = "";
        fHelpText = Optional.absent();
        fGroupId = 0;
        fDependentModels = new TreeSet<>();
    }
    
    /**
     * Constructs an instance with the properties initialized based on the given
     * existing variable.
     * @param variable an existing variable containing the initial properties.
     * Not stored.
     * @param modelService to provide reference data (e.g., available
     * VariableGroups) to the user
     */
    public EditVariable(
            final AbstractVariable variable,
            final ModelInspectionService modelService)
    {
        this(modelService);
        fKey = variable.getKey();
        fDisplayName = variable.getDisplayName();
        fHelpText = variable.getHelpText();
        fGroupId = variable.getGroup().getId();

        // Calculate the dependent models.
        for (final RiskModel model : modelService.getAllRiskModels())
        {
            if (model.getRequiredVariables().contains(variable))
            {
                fDependentModels.add(model);
            }
        }
    }
    
    /**
     * Returns a user-friendly name of the variable type.
     */
    public abstract String getTypeName();
    
    /**
     * <p>If {@link #calculateDependentModels(Collection)} has been called,
     * returns the set of RiskModels that depend on this variable for the
     * user's reference. Otherwise, returns an empty set.</p>
     * @return a set sorted by the RiskModels' natural order
     */
    public final SortedSet<RiskModel> getDependentModels()
    {
        return fDependentModels;
    }

    /**
     * Returns the VariableGroups provided during construction.
     * @return a SortedSet in natural order
     */
    public final ImmutableSortedSet<VariableGroup> getAllGroups()
    {
        return fAllGroups;
    }

    /**
     * Returns the key to set on the variable.
     * @see AbstractVariable#getKey()
     */
    public final String getKey()
    {
        return fKey;
    }
    
    /**
     * Sets the key to set on the variable.
     * @see AbstractVariable#getKey()
     */
    public final void setKey(final String key)
    {
        fKey = key;
    }
    
    /**
     * <p>Returns the maximum valid length of the key to set, {@link
     * Variable#KEY_MAX}, for easy access from views.</p>
     */
    public final int getKeyMax()
    {
        return Variable.KEY_MAX;
    }

    /**
     * Returns the display name which {@link #applyToVariable()} will set.
     */
    public final String getDisplayName()
    {
        return fDisplayName;
    }

    /**
     * Sets the display name which {@link #applyToVariable()} will set.
     */
    public final void setDisplayName(String displayName)
    {
        fDisplayName = displayName;
    }
    
    /**
     * <p>Returns the maximum valid length of the display name to set,
     * {@link Variable#DISPLAY_NAME_MAX}, for easy access from views.</p>
     */
    public final int getDisplayNameMax()
    {
        return Variable.DISPLAY_NAME_MAX;
    }
    
    /**
     * Returns the optional help text as a String (for use in JSPs). An absent
     * value is represented by an empty string.
     */
    public final String getHelpText()
    {
        return fHelpText.or("");
    }

    /**
     * Sets the help text which {@link #applyToVariable()} will set.
     * @param helpText may be null or empty, which will be translated to an
     * absent value
     */
    public final void setHelpText(String helpText)
    {
        fHelpText = Optional.fromNullable(Strings.emptyToNull(helpText));
    }
    
    /**
     * Returns the database ID of the variable's group.
     */
    public final int getGroupId()
    {
        return fGroupId;
    }
    
    /**
     * Sets the database ID of the variable's group. Accepts an invalid group,
     * though {@link #applyToVariable()} will throw an exception if an invalid
     * group is set.
     */
    public final void setGroupId(final int groupId)
    {
        fGroupId = groupId;
    }
    
    /**
     * Returns the actual VariableGroup object corresponding to the set group
     * ID.
     * @return an {@link Optional} containing the group if it exists
     */
    public final Optional<VariableGroup> getGroup()
    {
        Optional<VariableGroup> foundGroup = Optional.absent();
        // There should not be more than 10 groups, so just iterate.
        for (final VariableGroup g : fAllGroups)
        {
            if (g.getId() == fGroupId)
            {
                foundGroup = Optional.of(g);
            }
        }
        
        return foundGroup;
    }
    
    /**
     * Returns the appropriate view name for creating a new variable of the
     * supported type.
     */
    public abstract String getNewViewName();
    
    /**
     * <p>Applies the base properties that we store here to an existing
     * variable.</p>
     * 
     * <ul>
     * <li>Display Name</li>
     * <li>Help Text</li>
     * <li>Variable Group</li>
     * </ul>
     * 
     * @param var the existing variable to modify
     * @throws IllegalStateException if the key to set doesn't already match
     * the variable's key
     */
    protected void applyBaseProperties(final AbstractVariable var)
    {
        if (!Objects.equals(fKey, var.getKey()))
        {
            throw new IllegalStateException(String.format(
                    "The given variable's key does not already match the key to set (%s).",
                    fKey));
        }
        var.setDisplayName(fDisplayName);
        var.setHelpText(fHelpText);
        var.setGroup(getGroup().get());
        // TODO: retrieval key, when we support more than BooleanVariables
    }
    
    /**
     * Builds a new instance based on the stored properties.
     */
    public abstract AbstractVariable buildNew();
}
