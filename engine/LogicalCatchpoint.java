// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.ReferenceType;

import java.util.ArrayList;
import java.util.List;

// ---------------------------------------------------------------------

class LogicalCatchpoint extends LogicalBreakpoint
{

	LogicalCatchpoint( BreakpointHandle handle, Kind kind, boolean temporary )
	{
		super ( handle, temporary );
		d_kind = kind;
	}

	// -----------------------------------------------------------------

	Kind getKind()
	{
		return d_kind;
	}

	// -----------------------------------------------------------------

	protected BreakpointInfo getBreakpointInfo()
	{
		/*
			samples:
			2       breakpoint     keep y   0x00403847 exception catch
			2       breakpoint     keep y   0x00007ffff7b91cd0 exception catch
			3       breakpoint     keep y   0x00007ffff7b92de0 exception throw
			3       breakpoint     keep y   0x0 exception uncaught

			bkpt={number="1",type="breakpoint",disp="keep",enabled="y",addr="0x00401514",
			what="exception catch",times="2",original-location="__cxa_begin_catch"},

			bkpt={number="2",type="breakpoint",disp="keep",enabled="y",addr="0x00401514",what=
				"exception throw",times="2",original-location="__cxa_throw"},
		*/
		BreakpointInfo bkInfo = new BreakpointInfo( getHandle() );
		bkInfo.d_address = PhysicalCatchpoint.PredefinedAddress;
		String reason = getPredefinedReasonStr();
		bkInfo.d_what = "exception " + reason;
		bkInfo.d_originalLocation = getPredefinedRoutineName();
		return bkInfo;
	}

	// -----------------------------------------------------------------

	protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints()
		throws Exception
	{
		List< PhysicalBreakpoint > breakpoints = null;

		List< ReferenceType > catchRefTypes = Instance.s_sourceManager.getClassByName( CatchpointRefType );
		if ( ( catchRefTypes != null ) && !catchRefTypes.isEmpty() )
		{
			ReferenceType catchRefType = catchRefTypes.get( 0 );
			breakpoints = allocPhysicalBreakpoints( catchRefType );
		}

		return breakpoints;
	}

	protected List< PhysicalBreakpoint > resolvePhysicalBreakpoints ( ReferenceType refType )
		throws Exception
	{
		List< PhysicalBreakpoint > breakpoints = null;

		if ( Utils.equalTypes( CatchpointRefType, refType ) )
			breakpoints = allocPhysicalBreakpoints( refType );

		return breakpoints;
	}

	private List< PhysicalBreakpoint > allocPhysicalBreakpoints( ReferenceType catchRefType )
	{
		List< PhysicalBreakpoint > breakpoints = new ArrayList< PhysicalBreakpoint >();

		final PhysicalBreakpoint hBreakpoint = new PhysicalCatchpoint (
			allocNextPhysicalHandle(), d_kind, catchRefType );
		breakpoints.add ( hBreakpoint );

		return breakpoints;
	}

	// ---------------------------------------------------------------------

	void accept( LogicalBreakpointVisitor visitor )
	{
		visitor.visitCatchpoint( this );
	}

	// ---------------------------------------------------------------------

	private final Kind d_kind;
	private static final String CatchpointRefType = "java.lang.Throwable";

}
