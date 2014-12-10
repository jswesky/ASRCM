package gov.va.med.srcalc.web.view;

/**
 * <p>Defines Tile name strings. See /WEB-INF/tiles-defs.xml.</p>
 * 
 * <p>All references in code to Tile names should be through this class to
 * enhance code clarity and facilitiate refactoring.</p>
 */
public class Tile
{
    /**
     * No construction.
     */
    private Tile()
    {
    }
    
    public static final String SELECT_SPECIALTY = "srcalc.selectSpecialty";
    public static final String ENTER_VARIABLES = "srcalc.enterVariables";
    public static final String DISPLAY_RESULTS = "srcalc.displayResults";
    public static final String MODEL_ADMIN_HOME = "srcalc.modelAdminHome";
    public static final String EDIT_VARIABLE = "srcalc.editVariable";
}
