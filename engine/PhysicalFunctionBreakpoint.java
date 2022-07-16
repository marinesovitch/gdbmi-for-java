// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.Location;
import com.sun.jdi.Method;

// ---------------------------------------------------------------------

public class PhysicalFunctionBreakpoint extends PhysicalCommonBreakpoint
{
    // -----------------------------------------------------------------

    PhysicalFunctionBreakpoint (
        BreakpointHandle handle,
        Integer methodIndex,
        boolean temporary )
    {
        super ( handle, temporary );
        d_methodIndex = methodIndex;
    }

    // -----------------------------------------------------------------

    protected Location getLocation()
    {
        final MethodManager methodManager = Instance.s_methodManager;
        final Method method = methodManager.index2method( d_methodIndex );
        final Location location = method.location();
        return location;
    }

    // -----------------------------------------------------------------

    private Integer d_methodIndex;

}
