package gov.va.med.srcalc.vista;

import java.util.List;

import com.google.common.collect.ImmutableList;

enum LabRetrievalEnum
{
    ALBUMIN
    {
        @Override
        List<String> getPossibleLabNames()
        {
            return ImmutableList.of("ALBUMIN");
        }
    };
    
    abstract List<String> getPossibleLabNames();
      
}
