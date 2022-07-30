// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

public class Watchdog extends Thread
{
	public Watchdog()
	{
		super ( "watchdog" );

		d_state = STATE_IDLE;
		d_waitTargetTime = 0;
		d_client = null;

		start();
	}

	public interface IClient
	{
		public void onWatchdogTimeout();
	}

	public void set ( IClient client, long timeout )
	{
		/*
			thread-race: if Watchdog.set is called just after Watchdog object has
			been created it may be executed before Watchdog.run even starts and
			we may get into troubles, so wait until loop in Watchdog.run does
			really start

			therefore this routine _cannot_ has attribute 'synchronized', because
			it may get into endless while waiting for start of loop inside ensureLoopStarted

			internalSet is still 'synchronized'
		*/
		ensureLoopStarted();
		internalSet( client, timeout );
	}

	public synchronized void reset()
	{
		if ( d_state == STATE_WAITING )
		{
			d_state = STATE_IDLE;
			d_client = null;
			notifyAll();
		}
	}

	public synchronized void finish()
	{
		d_state = STATE_FINISHING;
		notifyAll();

		while ( d_state != STATE_FINISHED )
			tryWait ( 0 );
	}

	// ---------------------------------------------------------------------
	// Thread

	public synchronized void run()
	{
		d_started = true;
		for ( ; ; )
		{
			tryWait ( 0 );

			if ( d_state == STATE_FINISHING )
			{
				d_state = STATE_FINISHED;
				notifyAll();
				return;
			}

			if ( d_state == STATE_WAITING && d_client != null )
			{
				while ( d_state == STATE_WAITING )
				{
					final long currentTime = System.currentTimeMillis();

					if ( currentTime < d_waitTargetTime )
					{
						final long period = d_waitTargetTime - currentTime;
						tryWait ( period );
					}
					else
						break;
				}

				if ( d_state == STATE_WAITING )
				{
					d_client.onWatchdogTimeout();
					d_client = null;
					d_state = STATE_IDLE;
				}
				else if ( d_state == STATE_FINISHING )
				{
					d_state = STATE_FINISHED;
					notifyAll();
					return;
				}
			}
		}
	}

	// ---------------------------------------------------------------------
	// internal operations

	private synchronized void internalSet ( IClient client, long timeout )
	{
		if ( d_state == STATE_IDLE )
		{
			final long currentTime = System.currentTimeMillis();
			d_state = STATE_WAITING;
			d_waitTargetTime = currentTime + timeout;
			d_client = client;
			notifyAll();
		}
	}

	private void tryWait ( long timeout )
	{
		try
		{
			wait ( timeout );
		}
		catch ( Exception ex )
		{
		}
	}

	private void ensureLoopStarted()
	{
		while ( ! d_started )
			Thread.yield();
	}

	// ---------------------------------------------------------------------

	public static final int STATE_IDLE = 0;
	public static final int STATE_WAITING = 1;
	public static final int STATE_FINISHING = 2;
	public static final int STATE_FINISHED = 3;

	private volatile boolean d_started = false;
	private volatile int d_state;
	private volatile long d_waitTargetTime;
	private volatile IClient d_client;
}

// ---------------------------------------------------------------------
