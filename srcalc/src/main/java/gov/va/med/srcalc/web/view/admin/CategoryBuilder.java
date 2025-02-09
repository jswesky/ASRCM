package gov.va.med.srcalc.web.view.admin;

import gov.va.med.srcalc.domain.model.DiscreteNumericalVariable.Category;
import gov.va.med.srcalc.domain.model.MultiSelectOption;

/**
 * <p>Builds a {@link Category} from incrementally- specified component parts. This class
 * is useful because Category is immutable: you must specify all properties in its
 * constructor and you can't use it as a Java bean.</p>
 * 
 * <p>Per Effective Java Item 17, this class is marked final because it was not
 * designed for inheritance.</p>
 */
public final class CategoryBuilder
{
    private float fUpperBound;
    private boolean fUpperInclusive;
    private String fValue;
    
    /**
     * Constructs an instance with default values. The default upper bound is 0
     * inclusive and the default value is an empty string.
     */
    public CategoryBuilder()
    {
        fUpperInclusive = true;
        fValue = "";
    }
    
    /**
     * Factory method to create an instance based on a prototype Category.
     * @return a new builder that will build categories equivalent to the given
     * prototype
     */
    public static CategoryBuilder fromPrototype(
            final Category prototype)
    {
        return new CategoryBuilder()
            .setValue(prototype.getOption().getValue())
            .setUpperBound(prototype.getUpperBound())
            .setUpperInclusive(prototype.isUpperInclusive());
    }

    /**
     * Returns the value of this Category.
     * @see Category#getOption()
     */
    public String getValue()
    {
        return fValue;
    }

    /**
     * Sets the value of the Category.
     * @see Category#getOption()
     * @return this builder for convenience
     */
    public CategoryBuilder setValue(String value)
    {
        fValue = value;
        return this;
    }
    
    /**
     * Returns the upper bound for the Category.
     * @see Category#getUpperBound()
     */
    public float getUpperBound()
    {
        return fUpperBound;
    }

    /**
     * Sets the upper bound for the Category.
     * @see Category#getUpperBound()
     * @return this builder for convenience
     */
    public CategoryBuilder setUpperBound(float upperBound)
    {
        fUpperBound = upperBound;
        return this;
    }

    /**
     * Returns the upper bound inclusive flag for the Category.
     * @see Category#isUpperInclusive()
     */
    public boolean getUpperInclusive()
    {
        return fUpperInclusive;
    }

    /**
     * Sets the upper bound inclusive flag for the Category.
     * @see Category#isUpperInclusive()
     * @return this builder for convenience
     */
    public CategoryBuilder setUpperInclusive(boolean upperInclusive)
    {
        fUpperInclusive = upperInclusive;
        return this;
    }

    /**
     * Builds the Category with the configured range and value.
     */
    public Category build()
    {
        return new Category(
                new MultiSelectOption(fValue), fUpperBound, fUpperInclusive);
        
    }
}
