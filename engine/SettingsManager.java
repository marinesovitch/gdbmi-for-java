// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;

// ---------------------------------------------------------------------

public class SettingsManager
{
    // -----------------------------------------------------------------

    public void setValue ( String name, String value )
    {
        d_variable2value.put ( name, value );
    }

    // -----------------------------------------------------------------

    public String getValue ( String name )
    {
        String result = d_variable2value.get ( name );
        if ( result == null )
            result = "";
        return result;
    }

    // -----------------------------------------------------------------

    public boolean isValue ( String key, String value )
    {
        boolean result = false;
        String actualValue = getValue( key );
        if ( value != null )
            result = actualValue.equals( value );
        return result;
    }

    // -----------------------------------------------------------------

    private Map< String, String > d_variable2value =
        new HashMap< String, String >();
}

// ---------------------------------------------------------------------
