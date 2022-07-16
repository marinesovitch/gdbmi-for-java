// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;

// ---------------------------------------------------------------------

public class PhysicalCatchpoint extends PhysicalBreakpoint
{
    // -----------------------------------------------------------------

    PhysicalCatchpoint (
        BreakpointHandle handle,
        Breakpoint.Kind kind,
        ReferenceType exceptionRefType )
    {
        super( handle, false );
        d_kind = kind;
        d_exceptionRefType = exceptionRefType;
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
        bkInfo.d_address = PredefinedAddress;
        String reason = getPredefinedReasonStr();
        bkInfo.d_what = "exception " + reason;
        bkInfo.d_originalLocation = getPredefinedRoutineName();
        bkInfo.d_hitCount = getHitCount();
        return bkInfo;
    }

    // -----------------------------------------------------------------

    long getAddress()
    {
        return PredefinedAddress;
    }

    // -----------------------------------------------------------------

    protected EventRequest createConcrete()
    {
        EventRequestManager eventRequestManager =
            Instance.getVirtualMachine().eventRequestManager();

        boolean notifyCatch = false;
        boolean notifyUncaught = false;

        switch ( d_kind )
        {
            case CatchpointCatch:
                notifyCatch = true;
                break;

            case CatchpointUncaught:
                notifyUncaught = true;
                break;
        }

        return eventRequestManager.createExceptionRequest (
            d_exceptionRefType, notifyCatch, notifyUncaught );
    }

    // ---------------------------------------------------------------------

    private final Breakpoint.Kind d_kind;
    private ReferenceType d_exceptionRefType;

    // -----------------------------------------------------------------

    static final long PredefinedAddress = 0x0;

}
