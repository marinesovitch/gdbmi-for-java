// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import com.sun.jdi.request.EventRequest;
import java.util.Iterator;
import java.util.List;

// ---------------------------------------------------------------------

abstract class LogicalBreakpoint extends Breakpoint
{

	protected LogicalBreakpoint (
		BreakpointHandle handle,
		boolean temporary )
	{
		super( handle, temporary );
		d_physicalHandleFactory = new BreakpointHandleFactory( handle );
	}

	// -----------------------------------------------------------------

	boolean isResolved()
	{
		boolean result = ( d_physicalBreakpoints != null );
		return result;
	}

	// -----------------------------------------------------------------

	public synchronized void resolve()
		throws Exception
	{
		try
		{
			if ( d_physicalBreakpoints == null )
			{
				d_physicalBreakpoints = resolvePhysicalBreakpoints();
				bind();
			}
		}
		catch ( XNotConnected e )
		{
		}
	}

	protected abstract List< PhysicalBreakpoint > resolvePhysicalBreakpoints()
		throws Exception;

	// -----------------------------------------------------------------

	public synchronized boolean resolveAgainstClass ( ReferenceType hType )
		throws Exception
	{
		boolean result = false;
		if ( d_physicalBreakpoints == null )
		{
			d_physicalBreakpoints = resolvePhysicalBreakpoints ( hType );
			result = bind();
		}
		return result;
	}

	protected abstract List< PhysicalBreakpoint > resolvePhysicalBreakpoints ( ReferenceType hType )
		throws Exception;

	// -----------------------------------------------------------------

	private boolean bind()
	{
		boolean result = false;
		if ( d_physicalBreakpoints != null )
		{
			for ( PhysicalBreakpoint breakpoint : d_physicalBreakpoints )
			{
				breakpoint.create();
				EventRequest hRequest = breakpoint.getRequest();

				hRequest.setSuspendPolicy ( d_suspendPolicy );

				internalSetPassCount();
				internalSetCondition();

				breakpoint.setEnabled( d_enabled );
			}
			result = true;
		}
		return result;
	}

	// -----------------------------------------------------------------

	synchronized void remove()
	{
		if ( d_physicalBreakpoints != null )
		{
			Iterator< PhysicalBreakpoint > iBreakpoint = d_physicalBreakpoints.iterator();

			while ( iBreakpoint.hasNext() )
			{
				PhysicalBreakpoint hBreakpoint = iBreakpoint.next();
				hBreakpoint.remove();
			}
		}
	}

	// -----------------------------------------------------------------

	public boolean isEnabled()
	{
		return d_enabled;
	}

	public void setEnabled( boolean enabled )
	{
		if ( enabled != d_enabled )
		{
			d_enabled = enabled;

			if ( d_physicalBreakpoints != null )
			{
				Iterator< PhysicalBreakpoint > iBreakpoint = d_physicalBreakpoints.iterator();

				while ( iBreakpoint.hasNext() )
				{
					PhysicalBreakpoint hBreakpoint = iBreakpoint.next();
					hBreakpoint.setEnabled( d_enabled );
				}
			}
		}
	}

	// -----------------------------------------------------------------

	public synchronized void setPassCount( int passCount )
	{
		d_passCount = passCount;
		internalSetPassCount();
	}

	private void internalSetPassCount()
	{
		if ( ( d_physicalBreakpoints != null ) && ( d_passCount != 0 ) )
		{
			Iterator< PhysicalBreakpoint > iBreakpoint = d_physicalBreakpoints.iterator();
			while ( iBreakpoint.hasNext() )
			{
				PhysicalBreakpoint hBreakpoint = iBreakpoint.next();
				hBreakpoint.setPassCount( d_passCount );
			}
		}
	}

	// -----------------------------------------------------------------

	public synchronized void setCondition( String condition )
	{
		d_condition = condition;
		internalSetCondition();
	}

	private void internalSetCondition()
	{
		if ( ( d_physicalBreakpoints != null ) && ( d_condition != null ) )
		{
			Iterator< PhysicalBreakpoint > iBreakpoint = d_physicalBreakpoints.iterator();
			while ( iBreakpoint.hasNext() )
			{
				PhysicalBreakpoint hBreakpoint = iBreakpoint.next();
				hBreakpoint.setCondition( d_condition );
			}
		}
	}

	// -----------------------------------------------------------------

	public String getCliDescription()
	{
		String result = getDescription( DescCli );
		return result;
	}

	public String getMiDescription()
	{
		String result = getDescription( DescMi );
		return result;
	}

	// -----------------------------------------------------------------

	private String getDescription( int descFlags )
	{
		StringBuffer sb = new StringBuffer();
		if ( ( d_physicalBreakpoints == null ) || d_physicalBreakpoints.isEmpty() )
			storePendingDescription( descFlags | DescSingle, sb );
		else
		{
			assert d_physicalBreakpoints != null;
			if ( 1 == d_physicalBreakpoints.size() )
				storeSingleDescription( descFlags | DescSingle, sb );
			else
				storeMultipleDescription( descFlags, sb );
		}
		String result = sb.toString();
		return result;
	}

	private void storePendingDescription( int descFlags, StringBuffer sb  )
	{
		BreakpointInfo bkInfo = getBreakpointInfo();
		bkInfo.d_pending = true;
		storeDescription( descFlags, bkInfo, sb );
	}

	private void storeSingleDescription( int descFlags, StringBuffer sb  )
	{
		PhysicalBreakpoint bkPhysical = d_physicalBreakpoints.get( 0 );
		BreakpointInfo bkPhysicalInfo = bkPhysical.getBreakpointInfo();
		storeDescription( descFlags, bkPhysicalInfo, sb );
	}

	private void storeMultipleDescription( int descFlags, StringBuffer sb  )
	{
		assert 1 < d_physicalBreakpoints.size();
		BreakpointInfo bkInfo = getBreakpointInfo();
		bkInfo.d_multiple = true;
		storeDescription( descFlags, bkInfo, sb );
		for ( PhysicalBreakpoint bkPhysical : d_physicalBreakpoints )
		{
			sb.append ( "\n" );
			BreakpointInfo bkPhysicalInfo = bkPhysical.getBreakpointInfo();
			storeDescription( descFlags, bkPhysicalInfo, sb );
		}
	}

	private void storeDescription( int descFlags, BreakpointInfo bkInfo, StringBuffer sb )
	{
		if ( Utils.checkFlag( descFlags, DescCli ) )
			storeCliDescription( descFlags, bkInfo, sb );
		else
			storeMiDescription( descFlags, bkInfo, sb );
	}

	// -----------------------------------------------------------------

	private void storeCliDescription( int descFlags, BreakpointInfo bkInfo, StringBuffer sb )
	{
		/*
			3       breakpoint     keep y   <PENDING>  unloadedLibrarySrc.cpp:10
			3       breakpoint     keep y   <PENDING>  f
			3       read watchpoint   keep y   <PENDING>  Test.d_x

			19      breakpoint     keep y   <MULTIPLE>

			2       breakpoint     keep y   0x08048542 in _Z8funkcja3R10Struktura1i at bioFile.cpp:30
			9       breakpoint     keep y   0x00b0fb63 <_dl_debug_state+3>
		*/

		boolean single = Utils.checkFlag( descFlags, DescSingle );
		appendCliDescription( bkInfo.getIndexString( single ), sb );
		appendCliDescription( getKindDescription(), sb );

		appendCliDescription( isTemporary() ? "del" : "keep", sb );
		appendCliDescription( isEnabled() ? "y" : "n", sb );

		appendCliDescription( bkInfo.getAddressString(), sb );
		if ( !bkInfo.d_multiple )
		{
			String locationStr = bkInfo.getCliLocationString();
			appendCliDescription( locationStr, sb );

			storeHitCountCliDescription( bkInfo, sb );
		}
	}

	private void storeHitCountCliDescription( BreakpointInfo bkInfo, StringBuffer sb )
	{
		Integer hitCount = bkInfo.d_hitCount;
		if ( ( hitCount != null ) && ( 0 < hitCount ) )
		{
			sb.append( "\n        breakpoint already hit " );
			sb.append( hitCount );
			String postifx = ( hitCount == 1 ) ? " time" : " times";
			sb.append( postifx );
		}
	}

	private <TValue> void appendCliDescription( TValue value, StringBuffer sb )
	{
		if ( value != null )
		{
			sb.append( value );
			sb.append( ' ' );
		}
	}

	// -----------------------------------------------------------------

	private void storeMiDescription( int descFlags, BreakpointInfo bkInfo, StringBuffer sb )
	{
		sb.append ( '{' );

		boolean single = Utils.checkFlag( descFlags, DescSingle );
		appendMiDescription( "number", bkInfo.getIndexString( single ), false, sb );
		appendMiDescription( "type", getKindDescription(), sb );

		appendMiDescription( "disp", isTemporary() ? "del" : "keep", sb );
		appendMiDescription( "enabled", isEnabled() ? "y" : "n", sb );

		appendMiDescription( "addr", bkInfo.getAddressString(), sb );
		appendMiDescription( "what", bkInfo.d_what, sb );
		appendMiDescription( "func", bkInfo.d_function, sb );

		appendMiDescription( "file", bkInfo.d_fileName, sb );
		appendMiDescription( "fullname", bkInfo.d_fullFilePath, sb );
		appendMiDescription( "line", bkInfo.d_lineNum, sb );

		appendMiDescription( "times", bkInfo.d_hitCount, sb );

		appendMiDescription( "original-location", bkInfo.d_originalLocation, sb );

		sb.append ( '}' );
	}

	private <TValue> void appendMiDescription( String label, TValue value, StringBuffer sb )
	{
		appendMiDescription( label, value, true, sb );
	}

	private <TValue> void appendMiDescription(
		String label, TValue value, boolean comma, StringBuffer sb )
	{
		if ( value != null )
		{
			if ( comma )
				sb.append( ',' );
			sb.append( label );
			sb.append( "=\"" );
			sb.append( value );
			sb.append ( "\"" );
		}
	}

	// ---------------------------------------------------------------------

	private String getKindDescription() //throws Exception
	{
		String result = null;
		Kind kind = getKind();
		switch ( kind )
		{
			case Breakpoint:
			case CatchpointCatch:
			case CatchpointUncaught:
				result = "breakpoint";
				break;

			case WatchpointRead:
				result = "read watchpoint";
				break;

			case WatchpointWrite:
				result = "watchpoint";
				break;

			case WatchpointAccess:
				result = "acc watchpoint";
				break;

			default:
				//throw new Exception( "Unknown breakpoint kind!" );
		}
		return result;
	}

	// ---------------------------------------------------------------------

	private final static int DescCli = 0x1;
	private final static int DescMi = 0x2;
	private final static int DescSingle = 0x4;

	// ---------------------------------------------------------------------

	void accept( LogicalBreakpointVisitor visitor )
	{
		visitor.visitBreakpoint( this );
	}

	// -----------------------------------------------------------------

	BreakpointHandle allocNextPhysicalHandle()
	{
		BreakpointHandle result = d_physicalHandleFactory.allocNextPhysicalHandle();
		return result;
	}

	// -----------------------------------------------------------------

	protected int d_suspendPolicy = EventRequest.SUSPEND_ALL;
	protected boolean d_enabled = true;
	protected String d_condition;
	protected Integer d_passCount = 0;
	protected List< PhysicalBreakpoint > d_physicalBreakpoints;

	private BreakpointHandleFactory d_physicalHandleFactory;

}
