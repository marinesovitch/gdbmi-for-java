package wingdbJavaDebugEngine;

import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.AbsentInformationException;

// ---------------------------------------------------------------------

abstract class PhysicalCommonBreakpoint extends PhysicalBreakpoint
{
    PhysicalCommonBreakpoint ( BreakpointHandle handle, boolean temporary )
    {
        super ( handle, temporary );
    }

    // -----------------------------------------------------------------

    Kind getKind()
    {
        return Breakpoint.Kind.Breakpoint;
    }

    // -----------------------------------------------------------------

    protected BreakpointInfo getBreakpointInfo()
    {
        /*
            samples:
            2 breakpoint     keep y   0x08048542 in _Z8funkcja3R10Struktura1i at bioFile.cpp:30
            3 breakpoint     keep y   0x00b0fb63 <_dl_debug_state+3>
        */
        BreakpointInfo bkInfo = new BreakpointInfo( getHandle() );

        MethodManager methodManager = Instance.s_methodManager;

        final Location location = getLocation();
        final Method method = location.method();
        final String methodId = methodManager.method2manglid ( method );

        final long offset = MethodManager.getOffset ( location );
        final long address = methodManager.methodoffset2address ( method, offset );

        bkInfo.d_address = address;
        bkInfo.d_function = methodId;
        bkInfo.d_offset = offset;

        try
        {
            SourceManager sourceManager = Instance.s_sourceManager;
            bkInfo.d_fileName = location.sourceName();
            bkInfo.d_fullFilePath = sourceManager.getAbsolutePath ( location );
            bkInfo.d_lineNum = location.lineNumber();
        }
        catch ( AbsentInformationException e )
        {
        }

        bkInfo.d_hitCount = getHitCount();
        return bkInfo;
    }

    // -----------------------------------------------------------------

    protected EventRequest createConcrete()
    {
        EventRequest result = null;

        final Location location = getLocation();
        if ( location != null )
        {
            EventRequestManager eventRequestManager =
                Instance.getVirtualMachine().eventRequestManager();

            result = eventRequestManager.createBreakpointRequest ( location );
        }

        return result;
    }

    // -----------------------------------------------------------------

    protected abstract Location getLocation();

}
