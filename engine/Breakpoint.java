package wingdbJavaDebugEngine;

abstract class Breakpoint
{
	protected Breakpoint( BreakpointHandle handle, boolean temporary )
	{
		d_handle = handle;
		d_temporary = temporary;
	}

	// -----------------------------------------------------------------

	enum Kind
	{
		Unknown,
		Breakpoint,
		CatchpointCatch,
		CatchpointUncaught,
		WatchpointRead,
		WatchpointWrite,
		WatchpointAccess
	};

	// -----------------------------------------------------------------

	BreakpointHandle getHandle()
	{
		return d_handle;
	}

	int getLogicalIndex()
	{
		return d_handle.d_logicalIndex;
	}

	int getPhysicalIndex()
	{
		return d_handle.d_physicalIndex;
	}

	abstract Kind getKind();

	boolean isTemporary()
	{
		return d_temporary;
	}

	// -----------------------------------------------------------------

	abstract boolean isEnabled();
	abstract void setEnabled( boolean enabled );

	// -----------------------------------------------------------------

	protected abstract BreakpointInfo getBreakpointInfo();

	// -----------------------------------------------------------------

	String getPredefinedReasonStr()
	{
		String result = null;
		Kind kind = getKind();
		switch ( kind )
		{
			case CatchpointCatch:
				result = "catch";
				break;

			case CatchpointUncaught:
				result = "uncaught";
				break;
		}
		return result;
	}

	// ---------------------------------------------------------------------

	String getPredefinedRoutineName()
	{
		String result = null;
		Kind kind = getKind();
		switch ( kind )
		{
			case CatchpointCatch:
				result = CatchRoutine;
				break;

			case CatchpointUncaught:
				result = UncaughtRoutine;
				break;

			default:
				//throw new Exception( "Unknown catchpoint kind!" );
		}
		return result;
	}

	// -----------------------------------------------------------------

	final protected BreakpointHandle d_handle;
	final protected boolean d_temporary;

	// -----------------------------------------------------------------

	private static final String CatchRoutine = "__cxa_begin_catch";
	private static final String UncaughtRoutine = "__cxa_uncaught";

}
