package gov.va.med.srcalc.vista;

import java.util.List;

public interface VistaProcedureCaller
{
    /**
     * Performs a Remote Procedure Call.
     * @param duz the calling user's DUZ
     * @param procedure the remote procedure to call
     * @param args the remote procedure arguments, if any
     * 		The only valid types for arguments are String and Map<?,?>
     * @return an unmodifiable list of String lines from the response
     * @throws IllegalArgumentException if the provided DUZ is invalid
     * @throws DataAccessException if some error occurred communicating with
     * VistA
     * @throws ClassCastException if an object is not of type String or Map<?,?>
     */
    public List<String> doRpc(
            final String duz, final RemoteProcedure procedure, final Object... args);
    
    /**
     * Returns the division identifier (including any suffix) for the target
     * VistA.
     */
    public String getDivision();
}
