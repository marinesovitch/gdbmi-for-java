// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.Location;
import com.sun.jdi.Method;

// ---------------------------------------------------------------------

public class PhysicalCodeBreakpoint extends PhysicalCommonBreakpoint
{

	PhysicalCodeBreakpoint (
		BreakpointHandle handle,
		long address,
		boolean temporary )
	{
		super ( handle, temporary );
		d_address = address;
	}

	// -----------------------------------------------------------------

	protected Location getLocation()
	{
		Location location = null;
		final MethodManager methodManager = Instance.s_methodManager;
		final Method method = methodManager.address2method ( d_address );
		if ( method != null )
		{
			final long offset = methodManager.address2offset ( d_address );
			location = method.locationOfCodeIndex ( offset );
		}
		return location;
	}

	// -----------------------------------------------------------------

	private long d_address;

}
