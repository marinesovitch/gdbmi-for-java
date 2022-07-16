// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.*;
import com.sun.jdi.*;

// ---------------------------------------------------------------------

/*
    All operations regarding threads and stack-frames have to go
    through this class

    ThreadManager has to be notified about start/end of each thread

    Indexes of threads are unique, if thread is closed/completed, then
    if it was used/published outside it will be not reused anymore

    But to avoid premature exhaustion of thread-indexes, the thread
    index which wasn't "published" goes back to the set of free indexes.
    Therefore the longer debugging session of application which does
    create and destroy a lot of threads will not exhaust all free
    thread-indexes very early, and free thread-index increases too fast.

    Stack-frame address is generated similar to address of code

    Address of code is equal to
        ( ( index_metody << 32 ) | offset ).

    Address of stack-frame is equal to
        ( ( 0xFFFFFFFF - thread_id ) << 32 ) | stack_frame_id.

    Stack frame id uniquely identifies stack frame in its thread
    this is index used in methods of ThreadReference class:
    frame, frameCount, frames.
*/

// ---------------------------------------------------------------------

public class ThreadManager
{
    // -----------------------------------------------------------------

    public synchronized void onThreadStart ( ThreadReference hThread )
    {
        addThread( hThread );
    }

    // -----------------------------------------------------------------

    public synchronized void onThreadEnd ( ThreadReference hThread )
    {
        if ( d_thread2id.containsKey ( hThread ) )
        {
            final int threadId = d_thread2id.get ( hThread );
            final ThreadInfo threadInfo = d_id2thread.get ( threadId );

            if ( ! threadInfo.d_bPublicId )
                d_freeThreadIdPool.add ( threadId );

            d_thread2id.remove ( hThread );
            d_id2thread.remove ( threadId );
        }
    }

    // -----------------------------------------------------------------

    public synchronized void onResumeExecution()
    {
        /*
            CAUTION!
            Always call this method after vmachine resumes, else in
            d_frame2address may stay incorrect values regarding running or
            non-existing thread, and at next insertion there will be
            the exception generated
                throw new InvalidStackFrameException("Thread has been resumed");
            see method
            StackFrameImpl::validateStackFrame
            jdi.src\jdimodelsrc\org\eclipse\jdi\internal\StackFrameImpl.java
        */
        d_frame2address.clear();
    }

    // -----------------------------------------------------------------

    public static class ThreadInfo
    {
        public int d_id;
        public ThreadReference d_thread;
        public boolean d_bPublicId;
        public int d_currentFrameIndex;
    }

    // -----------------------------------------------------------------

    public void loadThreads()
    {
        VirtualMachine vm = Instance.s_connection.getVirtualMachine();
        final List< ThreadReference > threads = vm.allThreads();
        for ( ThreadReference thread : threads )
        {
        	int threadId = addThread( thread );
            updateCurrentThread( threadId, thread );
        }
    }

    // -----------------------------------------------------------------

    private void updateCurrentThread( int threadId, ThreadReference thread )
    {
        if ( d_currentThreadId == InvalidThread )
        {
            // marines: maybe we should check thread name too
            // research showed that name of the first thread is "<1> main"
            int threadStatus = thread.status();
            if ( threadStatus == ThreadReference.THREAD_STATUS_RUNNING )
                d_currentThreadId = threadId;
        }
    }

    private int addThread( ThreadReference thread )
    {
        final int threadId = getFreeThreadId();

        ThreadInfo threadInfo = new ThreadInfo();
        threadInfo.d_bPublicId = false;
        threadInfo.d_id = threadId;
        threadInfo.d_thread = thread;
        threadInfo.d_currentFrameIndex = 0;

        d_thread2id.put ( thread, threadId );
        d_id2thread.put ( threadId, threadInfo );

        return threadId;
    }

    // -----------------------------------------------------------------

    public synchronized List< ThreadInfo > getThreads()
    {
        List< ThreadInfo > result = new ArrayList< ThreadInfo >();
        Iterator< ThreadInfo > iThreadInfo = d_id2thread.values().iterator();

        while ( iThreadInfo.hasNext() )
        {
            ThreadInfo threadInfo = iThreadInfo.next();
            threadInfo.d_bPublicId = true;
            result.add ( threadInfo );
        }

        return result;
    }

    // -----------------------------------------------------------------

    public synchronized ThreadReference getThreadById ( int threadId )
    {
        if ( ! d_id2thread.containsKey ( threadId ) )
            return null;

        return d_id2thread.get ( threadId ).d_thread;
    }

    // -----------------------------------------------------------------

    public synchronized int getIdByThread ( ThreadReference hThread )
    {
        if ( ! d_thread2id.containsKey ( hThread ) )
            return InvalidThread;

        return d_thread2id.get ( hThread );
    }

    // -----------------------------------------------------------------

    public synchronized int getCurrentThreadId()
    {
        return d_currentThreadId;
    }

    // -----------------------------------------------------------------

    public synchronized int getCurrentFrameIndex ( int threadId )
    {
        if ( ! d_id2thread.containsKey ( threadId ) )
            return InvalidThread;

        ThreadInfo threadInfo = d_id2thread.get ( threadId );
        return threadInfo.d_currentFrameIndex;
    }

    // -----------------------------------------------------------------

    public synchronized void setCurrentFrameIndex ( int threadId, int frameIndex )
        throws
            IncompatibleThreadStateException,
            ArrayIndexOutOfBoundsException
    {
        if ( ! d_id2thread.containsKey ( threadId ) )
            return;

        final ThreadInfo threadInfo = d_id2thread.get ( threadId );
        final List< StackFrame > frames = threadInfo.d_thread.frames();

        if ( frameIndex >= frames.size() )
            throw new ArrayIndexOutOfBoundsException();

        threadInfo.d_currentFrameIndex = frameIndex;
    }

    // -----------------------------------------------------------------

    public synchronized void setCurrentThreadId ( int threadId )
    {
        d_currentThreadId = threadId;
    }

    // -----------------------------------------------------------------

    public synchronized StackFrame getCurrentFrame()
    {
        StackFrame result = null;
        try
        {
            final int threadId = getCurrentThreadId();
            if ( threadId != InvalidThread )
            {
                final int currentFrameId
                    = Instance.s_threadManager.getCurrentFrameIndex ( threadId );
                if ( currentFrameId != InvalidThread )
                {
                    result = Instance.s_threadManager.getFrameById ( threadId, currentFrameId );
                }
            }
        }
        catch ( IncompatibleThreadStateException e )
        {
        }
        return result;
    }

    // -----------------------------------------------------------------

    public synchronized List< StackFrame > getFrames ( int threadId )
        throws IncompatibleThreadStateException
    {
        if ( d_id2thread.containsKey ( threadId ) )
        {
            final ThreadInfo threadInfo = d_id2thread.get ( threadId );
            final ThreadReference hThread = threadInfo.d_thread;

            final List< StackFrame > frames = hThread.frames();
            Iterator< StackFrame > iFrame = frames.iterator();

            int nFrame = 0;

            while ( iFrame.hasNext() )
            {
                final StackFrame hFrame = iFrame.next();
                final long address = frameIndex2address ( threadId, nFrame );
                d_frame2address.put ( hFrame, address );
                ++nFrame;
            }

            return frames;
        }
        else
            return null;
    }

    // -----------------------------------------------------------------

    public synchronized StackFrame getFrameById ( int threadId, int frameId )
        throws IncompatibleThreadStateException
    {
        StackFrame result = null;

        final List< StackFrame > frames = getFrames ( threadId );
        if ( Container.safeIsNotEmpty( frames ) )
        {
            if ( frameId >= frames.size() )
                throw new ArrayIndexOutOfBoundsException();

            result = frames.get ( frameId );
        }

        return result;
    }

    // -----------------------------------------------------------------

    public synchronized StackFrame getFrameByAddress ( long address )
    {
        final int threadId = address2threadIndex ( address );
        final int frameIndex = address2frameIndex ( address );
        final ThreadReference hThread = getThreadById ( threadId );

        if ( hThread == null )
            return null;

        try
        {
            final StackFrame hFrame = hThread.frame ( frameIndex );
            d_frame2address.put ( hFrame, address );
            return hFrame;
        }
        catch ( IncompatibleThreadStateException e )
        {
            return null;
        }
    }

    // -----------------------------------------------------------------

    public synchronized long getAddressByFrame ( StackFrame hFrame )
        throws IncompatibleThreadStateException
    {
        if ( ! d_frame2address.containsKey ( hFrame ) )
        {
            final ThreadReference hThread = hFrame.thread();

            if ( ! d_thread2id.containsKey ( hThread ) )
                return 0;

            final int threadId = d_thread2id.get ( hThread );
            getFrames ( threadId );

            if ( ! d_frame2address.containsKey ( hFrame ) )
                return 0;
        }

        return d_frame2address.get ( hFrame );
    }

    // -----------------------------------------------------------------

    private synchronized int getFreeThreadId()
    {
        if ( d_freeThreadIdPool.isEmpty() )
        {
            final int threadId = ++s_nextThreadId;
            d_freeThreadIdPool.add ( threadId );
        }

        final Integer firstFree = d_freeThreadIdPool.first();
        d_freeThreadIdPool.remove ( firstFree );

        return firstFree;
    }

    // -----------------------------------------------------------------

    private static long frameIndex2address ( int threadId, int frameIndex )
    {
        return ( ( ( long ) ~threadId ) << 32 ) | frameIndex;
    }

    // -----------------------------------------------------------------

    private static int address2frameIndex ( long address )
    {
        return ( int )( address & 0xFFFFFFFF );
    }

    // -----------------------------------------------------------------

    private static int address2threadIndex ( long address )
    {
        return ~( int )( address >> 32 );
    }

    // -----------------------------------------------------------------

    public static final int InvalidThread = -1;

    // -----------------------------------------------------------------

    private SortedSet< Integer > d_freeThreadIdPool = new TreeSet< Integer >();
    private static int s_nextThreadId = 0;

    private Map< ThreadReference, Integer > d_thread2id =
        new HashMap< ThreadReference, Integer >();

    private Map< Integer, ThreadInfo > d_id2thread =
        new TreeMap< Integer, ThreadInfo >();

    private Map< StackFrame, Long > d_frame2address =
        new HashMap< StackFrame, Long >();

    private int d_currentThreadId = InvalidThread;
}

// ---------------------------------------------------------------------
