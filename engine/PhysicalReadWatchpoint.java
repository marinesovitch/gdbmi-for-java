// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.Field;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

// ---------------------------------------------------------------------

public class PhysicalReadWatchpoint extends PhysicalWatchpoint
{
	PhysicalReadWatchpoint (
		BreakpointHandle handle,
		String className,
		String fieldName,
		ReferenceType classType,
		boolean temporary )
	{
		super ( handle, className, fieldName, classType, temporary );
	}

	// -----------------------------------------------------------------

	Kind getKind()
	{
		return Breakpoint.Kind.WatchpointRead;
	}

	// -----------------------------------------------------------------

	protected EventRequest createConcrete()
	{
		final Field field = d_classType.fieldByName ( d_fieldName );

		EventRequestManager eventRequestManager =
			Instance.getVirtualMachine().eventRequestManager();

		return eventRequestManager.createAccessWatchpointRequest ( field );
	}

}
