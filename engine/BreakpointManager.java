// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import com.sun.jdi.event.ClassPrepareEvent;

import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

// ---------------------------------------------------------------------

class BreakpointManager
{
    public BreakpointManager()
    {
    }

    // ---------------------------------------------------------------------

    public synchronized boolean resolve ( ClassPrepareEvent event )
    {
        boolean result = false;

        final ReferenceType hType = event.referenceType();

        if ( hType instanceof ClassType )
        {
            final ClassType hClass = ( ClassType ) hType;

            Iterator< LogicalBreakpoint > it = d_allBreakpoints.values().iterator();

            while ( it.hasNext() )
            {
                LogicalBreakpoint breakpoint = it.next();

                if ( ! breakpoint.isResolved() )
                {
                    try
                    {
                        result |= breakpoint.resolveAgainstClass ( hClass );
                    }
                    catch ( Exception e )
                    {
                    }
                }
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------

    public synchronized void resolveAll()
    {
        for ( LogicalBreakpoint breakpoint : d_allBreakpoints.values() )
        {
            try
            {
                breakpoint.resolve();
            }
            catch ( Exception e )
            {
            }
        }
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalBreakpoint createSourceBreakpoint (
        String sourcePath,
        int lineNum,
        boolean temporary )
        throws Exception
    {
        LogicalBreakpoint breakpoint = new LogicalSourceBreakpoint (
            allocNextHandle(), sourcePath, lineNum, temporary );

        d_allBreakpoints.put ( breakpoint.getLogicalIndex(), breakpoint );
        breakpoint.resolve();
        return breakpoint;
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalBreakpoint createFunctionBreakpoint (
        String rawMethodName,
        boolean temporary )
        throws Exception
    {
        String methodSpec = rawMethodName;
        if ( rawMethodName.equals ( "main" ) )
            methodSpec = Instance.s_mainClassName + '.' + rawMethodName;

        LogicalBreakpoint breakpoint = new LogicalFunctionBreakpoint (
            allocNextHandle(), methodSpec, temporary );

        d_allBreakpoints.put ( breakpoint.getLogicalIndex(), breakpoint );
        breakpoint.resolve();
        return breakpoint;
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalBreakpoint createCodeBreakpoint (
        long address,
        boolean temporary )
        throws Exception
    {
        LogicalBreakpoint breakpoint = new LogicalCodeBreakpoint (
            allocNextHandle(), address, temporary );

        d_allBreakpoints.put ( breakpoint.getLogicalIndex(), breakpoint );
        breakpoint.resolve();
        return breakpoint;
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalBreakpoint createCatchpoint( Breakpoint.Kind kind )
            throws Exception
    {
        LogicalBreakpoint breakpoint = new LogicalCatchpoint (
        	allocNextHandle(), kind, false );

        d_allBreakpoints.put ( breakpoint.getLogicalIndex(), breakpoint );
        breakpoint.resolve();
        return breakpoint;
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalWatchpoint createReadWatchpoint (
        String className,
        String fieldName,
        boolean temporary )
        throws Exception
    {
        LogicalWatchpoint breakpoint = new LogicalReadWatchpoint (
            allocNextHandle(), className, fieldName, temporary );

        d_allBreakpoints.put ( breakpoint.getLogicalIndex(), breakpoint );
        breakpoint.resolve();
        return breakpoint;
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalWatchpoint createWriteWatchpoint (
        String className,
        String fieldName,
        boolean temporary )
        throws Exception
    {
        LogicalWatchpoint breakpoint = new LogicalWriteWatchpoint (
            allocNextHandle(), className, fieldName, temporary );

        d_allBreakpoints.put ( breakpoint.getLogicalIndex(), breakpoint );
        breakpoint.resolve();
        return breakpoint;
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalWatchpoint createAccessWatchpoint (
        String className,
        String fieldName,
        boolean temporary )
        throws Exception
    {
        LogicalWatchpoint breakpoint = new LogicalAccessWatchpoint (
            allocNextHandle(), className, fieldName, temporary );

        int bkLogicalIndex = breakpoint.getLogicalIndex();
        d_allBreakpoints.put ( bkLogicalIndex, breakpoint );
        breakpoint.resolve();
        return breakpoint;
    }

    // ---------------------------------------------------------------------

    public synchronized boolean remove( BreakpointHandle bkHandle )
    {
        LogicalBreakpoint breakpoint = getBreakpoint( bkHandle );

        if ( breakpoint != null )
        {
            breakpoint.remove();
            int bkLogicalIndex = breakpoint.getLogicalIndex();
            d_allBreakpoints.remove ( bkLogicalIndex );
            return true;
        }
        else
            return false;
    }

    // ---------------------------------------------------------------------

    public synchronized boolean enable ( BreakpointHandle bkHandle, boolean bEnable )
    {
        LogicalBreakpoint hBreakpointInfo = getBreakpoint( bkHandle );

        if ( hBreakpointInfo != null )
        {
            hBreakpointInfo.setEnabled ( bEnable );
            return true;
        }
        else
            return false;
    }

    // ---------------------------------------------------------------------

    public synchronized boolean setCondition( BreakpointHandle bkHandle, String condition )
    {
        LogicalBreakpoint hBreakpointInfo = getBreakpoint( bkHandle );

        if ( hBreakpointInfo != null )
        {
            hBreakpointInfo.setCondition( condition );
            return true;
        }
        else
            return false;
    }

    // ---------------------------------------------------------------------

    public synchronized boolean setPassCount( BreakpointHandle bkHandle, int passCount )
    {
        LogicalBreakpoint hBreakpointInfo = getBreakpoint( bkHandle );

        if ( hBreakpointInfo != null )
        {
            hBreakpointInfo.setPassCount ( passCount );
            return true;
        }
        else
            return false;
    }

    // ---------------------------------------------------------------------

    public synchronized LogicalBreakpoint getBreakpoint( BreakpointHandle bkHandle )
    {
        int bkLogicalIndex = bkHandle.d_logicalIndex;
        LogicalBreakpoint result = d_allBreakpoints.get ( bkLogicalIndex );
        return result;
    }

    // ---------------------------------------------------------------------

    public synchronized List< LogicalBreakpoint > getBreakpoints()
    {
        return new ArrayList< LogicalBreakpoint > ( d_allBreakpoints.values() );
    }

    // -----------------------------------------------------------------

    BreakpointHandle allocNextHandle()
    {
        BreakpointHandle result = d_bkHandleFactory.allocNextLogicalHandle();
        return result;
    }

    // ---------------------------------------------------------------------

    private Map< Integer, LogicalBreakpoint > d_allBreakpoints =
        new TreeMap< Integer, LogicalBreakpoint >();

    private BreakpointHandleFactory d_bkHandleFactory = new BreakpointHandleFactory();

}
