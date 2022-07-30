// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;

import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------

class LogicalCodeBreakpoint extends LogicalCommonBreakpoint
{

	LogicalCodeBreakpoint ( BreakpointHandle handle, long address, boolean temporary )
	{
		super ( handle, temporary );
		d_address = address;
	}

	// ---------------------------------------------------------------------

	protected BreakpointInfo getBreakpointInfo()
	{
		/*
			samples:
			3       breakpoint     keep y   <PENDING>  0x0123456789ABCDEF
		*/
		BreakpointInfo bkInfo = new BreakpointInfo( getHandle() );
		bkInfo.d_what = Utils.toAddressString( d_address );

		return bkInfo;
	}

	// ---------------------------------------------------------------------

	protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints()
		throws Exception
	{
		List< PhysicalBreakpoint > breakpoints = null;

		final Method hMethod =
			Instance.s_methodManager.address2method ( d_address );

		if ( hMethod != null )
		{
			final ReferenceType hParent = hMethod.declaringType();

			breakpoints = new ArrayList< PhysicalBreakpoint >();
			final PhysicalBreakpoint hBreakpoint = new PhysicalCodeBreakpoint (
				allocNextPhysicalHandle(), d_address, isTemporary() );
			breakpoints.add ( hBreakpoint );
		}

		return breakpoints;
	}

	protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints ( ReferenceType hType )
		throws Exception
	{
		return resolvePhysicalBreakpoints();
	}

	// ---------------------------------------------------------------------

	void accept( LogicalBreakpointVisitor visitor )
	{
		visitor.visitCodeBreakpoint( this );
	}

	// ---------------------------------------------------------------------

	private long d_address = 0;

}
