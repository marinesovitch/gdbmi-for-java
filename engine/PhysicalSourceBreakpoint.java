// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.Location;

// ---------------------------------------------------------------------

public class PhysicalSourceBreakpoint extends PhysicalCommonBreakpoint
{
	PhysicalSourceBreakpoint (
		BreakpointHandle handle,
		Location location,
		boolean temporary )
	{
		super ( handle, temporary );
		d_location = location;
	}

	// -----------------------------------------------------------------

	protected Location getLocation()
	{
		return d_location;
	}

	// -----------------------------------------------------------------

	private Location d_location;

}
