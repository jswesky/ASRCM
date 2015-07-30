package gov.va.med.srcalc.web.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import gov.va.med.srcalc.domain.calculation.*;
import gov.va.med.srcalc.domain.model.*;
import gov.va.med.srcalc.service.CalculationService;
import gov.va.med.srcalc.util.MissingValuesException;
import gov.va.med.srcalc.web.DynamicValueVisitor;
import gov.va.med.srcalc.web.view.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

/**
 * A "branch" of {@link CalculationController} containing request handlers
 * specifically for variable entry.
 */
@Controller
public class EnterVariablesController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EnterVariablesController.class);

    /**
     * Attribute name for the VariableEntry object.
     */
    public static final String ATTR_VARIABLE_ENTRY = "variableEntry";
    
    /**
     * Error code used when a required value is not provided.
     */
    public static final String ERROR_NO_VALUE = "noInput";
    
    private final CalculationService fCalculationService;
    
    @Inject
    public EnterVariablesController(final CalculationService calculationService)
    {
        fCalculationService = calculationService;
    }
    
    @ModelAttribute
    public VariableEntry constructVariableEntry(
            final HttpSession session)
    {
        // Get the Calculation from the session.
        final CalculationSession cs = SrcalcSession.getCalculationSession(session);
        final Calculation calculation = cs.getCalculation();
        final VariableEntry initialValues = VariableEntry.withRetrievedValues(
                calculation.getVariables(), calculation.getPatient());
        // In the case of using the "Return to Input Form" button, add the values already
        // in the calculation to the variable entry object to maintain the previously calculated
        // values on the entry page.
        if (cs.getOptionalLastResult().isPresent())
        {
            final CalculationResult lastResult = cs.getRequiredLastResult();
            final DynamicValueVisitor visitor = new DynamicValueVisitor(initialValues);
            LOGGER.debug("Pre-existing input values: {}", lastResult.getValues());
            for(final Value value: lastResult.getValues())
            {
                visitor.visit(value);
            }
        }
        return initialValues;
    }
    
    /**
     * Presents that variable entry form.
     * @param session the current HTTP session (required)
     * @param response needed to alter the response header to expire the page
     *         after a completed calculation
     * @param initialValues the initial values to set
     * @return a ModelAndView for view rendering
     */
    @RequestMapping(value = "/enterVars", method = RequestMethod.GET)
    public ModelAndView presentVariableEntry(
            final HttpSession session, final HttpServletResponse response,
            @ModelAttribute(ATTR_VARIABLE_ENTRY) final VariableEntry initialValues)
    {
        // Expire the page to warn the user that a back button is not viable
        // after completing a calculation. The page will require a reload if the back button
        // is used to go back to the enter variables page.
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        // Get the Calculation from the session.
        final Calculation calculation =
                SrcalcSession.getCalculationSession(session).getCalculation();

        // Present the view.
        final ModelAndView mav = new ModelAndView(Views.ENTER_VARIABLES);
        mav.addObject("calculation", calculation);
        mav.addObject("displayGroups", getDisplayGroups(calculation));
        // Note: "variableEntry" object is automatically added through annotated
        // method parameter.
        return mav;
    }
    
    @RequestMapping(value = "/enterVars", method = RequestMethod.POST)
    public ModelAndView enterVariables(
            final HttpSession session, final HttpServletResponse response,
            @ModelAttribute(ATTR_VARIABLE_ENTRY) final VariableEntry values,
            final BindingResult valuesBindingResult)
    {
        // Get the CalculationSession.
        final CalculationSession cs = SrcalcSession.getCalculationSession(session);
        final Calculation calculation = cs.getCalculation();
        
        // Extract the values from the HTTP POST.
        final InputParserVisitor parserVisitor = new InputParserVisitor(values, valuesBindingResult);
        for (final Variable variable : calculation.getVariables())
        {
            parserVisitor.visit(variable);
        }
        
        try
        {
            final CalculationResult result = fCalculationService.runCalculation(
                    calculation, parserVisitor.getValues());
            
            cs.setLastResult(result);
        }
        catch(final MissingValuesException e)
        {
            // Add all of the missing values to the binding errors
            for(final MissingValueException missingValue : e.getMissingValues())
            {
                // If the variable had invalid input, rather than missing input,
                // it would never be included in the values.
                final String dynamicKey = VariableEntry.makeDynamicValuePath(missingValue.getVariable().getKey());
                LOGGER.debug("Field Error: {}", dynamicKey);
                // Account for field errors on special fields like discrete numerical
                // variables.
                if(bindingResultAlreadyContainsError(dynamicKey, valuesBindingResult, missingValue))
                {
                            valuesBindingResult.rejectValue(
                                    dynamicKey, ERROR_NO_VALUE, missingValue.getMessage());
                }
            }
        }
        
        // If we have any binding errors, return to the Enter Variables screen.
        if (valuesBindingResult.hasErrors())
        {
            LOGGER.debug(
                    "Invalid variables entered by user. Reshowing variable entry screen. Errors: {}",
                    valuesBindingResult.getAllErrors());
            return presentVariableEntry(session, response, values);
        }

        // Using the POST-redirect-GET pattern.
        return new ModelAndView("redirect:/displayResults");
    }
    
    private boolean bindingResultAlreadyContainsError(final String dynamicKey,
            final BindingResult valuesBindingResult, final MissingValueException missingValue)
    {
        final String numericalKey = VariableEntry.makeDynamicValuePath(
                VariableEntry.makeNumericalInputName(missingValue.getVariable().getKey()));
        return !valuesBindingResult.hasFieldErrors(dynamicKey) && !valuesBindingResult.hasFieldErrors(numericalKey);
    }
    
    private List<PopulatedDisplayGroup> getDisplayGroups(final Calculation calculation)
    {
        // Ensure we are in the proper state.
        if (calculation.getSpecialty() == null)
        {
            throw new IllegalStateException(
                    "Cannot return list of variables because no specialty has been set.");
        }
        
        return Collections.unmodifiableList(
                buildDisplayGroupList(calculation.getVariables(), calculation));
    }
    
    /**
     * Builds a brand-new, sorted list of {@link PopulatedDisplayGroup}s from
     * the given variables. The groups are sorted by their natural ordering and
     * each variable list is sorted by display name.
     */
    private List<PopulatedDisplayGroup> buildDisplayGroupList(
            Collection<? extends Variable> variables, final Calculation calculation)
    {
        // Bucket the Variables according to VariableGroup.
        final HashMap<VariableGroup, List<Variable>> map = new HashMap<>();
        for (final Variable var : variables)
        {
            final VariableGroup group = var.getGroup();
            if (!map.containsKey(group))
            {
                final ArrayList<Variable> varList = new ArrayList<>();
                map.put(group, varList);
            }
            map.get(group).add(var);
        }
        
        // Transform the map into PopulatedVariableGroups.
        final ArrayList<PopulatedDisplayGroup> groupList =
                new ArrayList<>(map.values().size());
        final DisplayNameComparator comparator = new DisplayNameComparator();
        for (final List<Variable> varList : map.values())
        {
            Collections.sort(varList, comparator);
            groupList.add(new PopulatedDisplayGroup(varList, calculation.getPatient()));
        }
        
        // Finally, sort the List.
        Collections.sort(groupList);
        
        return groupList;
    }
}
