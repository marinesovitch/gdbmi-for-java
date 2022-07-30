// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

// ---------------------------------------------------------------------

/*
	input: key0=value0, key1=value1, ...
	although value0, value1, ... may be quoted
*/
class ArgumentsParser
{
	ArgumentsParser( Map< String, Connector.Argument > defaultArguments )
	{
		d_arguments = defaultArguments;
	}

	Map< String, Connector.Argument > run( String rawArguments )
	{
		if ( init( rawArguments ) )
			parseArguments();
		return d_arguments;
	}

	private boolean init( String rawArguments )
	{
		d_it = new StringCharacterIterator( rawArguments );
		d_it.first();
		boolean result = !isEot();
		return result;
	}

	private void parseArguments()
	{
		while ( ! isEot() )
		{
			String key = parseKey();
			if ( key != null )
			{
				String value = parseValue();
				if ( value != null )
				{
					Connector.Argument argument = d_arguments.get( key );
					if ( argument != null )
						argument.setValue( value );
					else
						throw new IllegalArgumentException( "Unexpected argument: " + key );
				}
				else
					throw new IllegalArgumentException( "Unexpected argument: " + key );
			}
		}
	}

	private void incChar()
	{
		d_it.next();
	}

	private char getChar()
	{
		char result = d_it.current();
		return result;
	}

	private boolean isEot()
	{
		char chr = d_it.current();
		boolean result = ( chr == CharacterIterator.DONE );
		return result;
	}

	private String parseKey()
	{
		StringBuffer sb = new StringBuffer();
		boolean stop = false;
		do
		{
			char chr = getChar();
			if ( chr != '=' )
				sb.append( chr );
			else
				stop = true;
			incChar();
		}
		while ( !isEot() && !stop );
		String result = null;
		if ( 0 < sb.length() )
			result = sb.toString();
		return result;
	}

	private enum EState
	{
		Normal,
		IsInsideQuotation
	}

	private String parseValue()
	{
		StringBuffer sb = new StringBuffer();
		boolean stop = false;
		while ( !isEot() && !stop )
		{
			stop = procesChar( sb );
			incChar();
		}
		String result = null;
		if ( 0 < sb.length() )
			result = sb.toString();
		return result;
	}

	private boolean procesChar( StringBuffer sb )
	{
		boolean stop = false;
		char chr = getChar();
		if ( chr == ',' )
			stop = processComma();
		else if ( chr == '"' )
			processQuotation( sb );
		else
			sb.append( chr );
		return stop;
	}

	private boolean processComma()
	{
		// stop parsing value after meet comma which is not included in quotation
		boolean stopParsing = ( d_state != EState.IsInsideQuotation );
		return stopParsing;
	}

	private void processQuotation( StringBuffer sb )
	{
		if ( d_state == EState.IsInsideQuotation )
			d_state = EState.Normal;
		else
			d_state = EState.IsInsideQuotation;
		sb.append( '"' );
	}

	private Map< String, Connector.Argument > d_arguments;

	private CharacterIterator d_it;
	private EState d_state = EState.Normal;
}

// ---------------------------------------------------------------------

class VirtualMachineConnector
{
	// ---------------------------------------------------------------------

	enum EKind
	{
		Launch,
		AttachSharedMemory,
		AttachSocket,
		AttachProcess,
		Listen,
		Unknown
	};

	// ---------------------------------------------------------------------

	VirtualMachineConnector( int timeOut )
	{
		d_kind = EKind.Unknown;
		d_timeOut = timeOut;
	}

	// ---------------------------------------------------------------------

	public String getJdiVersionString()
	{
		VirtualMachineManager vmm = Bootstrap.virtualMachineManager();
		StringBuffer sb = new StringBuffer();
		sb.append( vmm.majorInterfaceVersion() );
		sb.append( '.' );
		sb.append( vmm.minorInterfaceVersion() );
		String result = sb.toString();
		return result;
	}

	// ---------------------------------------------------------------------

	void setTargetDescription ( EKind connectorKind, String rawArguments )
		throws Exception
	{
		d_kind = connectorKind;

		final String connectorName = prepareConnectorName();

		if ( connectorName != null )
		{
			d_connector = get( connectorName );
			if ( d_connector != null )
			{
				final Map< String, Connector.Argument > defaultArguments =
					d_connector.defaultArguments();
				ArgumentsParser argumentsParser
					= new ArgumentsParser( defaultArguments );
				d_args = argumentsParser.run( rawArguments );
			}
			else
			{
				throw new Exception ( "Unknown connector name " + connectorName );
			}
		}
	}

	// ---------------------------------------------------------------------

	private String prepareConnectorName()
	{
		String result = null;

		switch ( d_kind )
		{
			case Launch:
				result = "com.sun.jdi.CommandLineLaunch";
				break;

			case AttachSharedMemory:
				result = "com.sun.jdi.SharedMemoryAttach";
				break;

			case AttachSocket:
				result = "com.sun.jdi.SocketAttach";
				break;

			case AttachProcess:
				result = "com.sun.jdi.ProcessAttach";
				break;

			case Listen:
				result = "FIXME";
				break;
		}

		return result;
	}

	// ---------------------------------------------------------------------

	private Connector get ( String name )
	{
		Connector result = null;

		List< Connector > connectors = Bootstrap.virtualMachineManager().allConnectors();
		for ( Connector connector : connectors )
		{
			String connectorName = connector.name();
			if ( connectorName.equals( name ) )
			{
				result = connector;
				break;
			}
		}
		return result;
	}

	// ---------------------------------------------------------------------

	boolean setArgument ( String name, String value )
	{
		boolean result = false;
		if ( d_virtualMachine == null )
		{
			Connector.Argument argument = d_args.get( name );
			if ( argument != null )
			{
				argument.setValue ( value );
				result = true;
			}
		}
		return result;
	}

	// ---------------------------------------------------------------------

	public boolean create()
		throws IOException, IllegalConnectorArgumentsException,
			VMStartException, InternalError
	{
		if ( d_connector instanceof LaunchingConnector )
			d_virtualMachine = launch();
		else if ( d_connector instanceof AttachingConnector )
			d_virtualMachine = attach();
		else if ( d_connector instanceof ListeningConnector )
			d_virtualMachine = listen();
		else
			throw new InternalError ( "unknown connector type" );

		final boolean result = isConnected();
		return result;
	}

	// ---------------------------------------------------------------------

	private VirtualMachine launch()
		throws IOException, IllegalConnectorArgumentsException, VMStartException
	{
		LaunchingConnector connector = ( LaunchingConnector ) d_connector;
		VirtualMachine vm = connector.launch ( d_args );
		d_process = vm.process();
		return vm;
	}

	// ---------------------------------------------------------------------

	private VirtualMachine attach()
		throws IOException, IllegalConnectorArgumentsException, InternalError
	{
		if ( d_kind != EKind.AttachSocket )
		{
			final AttachingConnector connector = ( AttachingConnector ) d_connector;
			final VirtualMachine result = connector.attach( d_args );
			return result;
		}
		else
		{
			final Connector.Argument argHost = d_args.get ( "hostname" );
			final Connector.Argument argPort = d_args.get ( "port" );

			if ( argHost != null && argPort != null )
			{
				final String strHost = argHost.value();
				final int port = Integer.parseInt ( argPort.value() );

				final Connection connection = new Connection ( strHost, port, d_timeOut );
				final VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

				return vmm.createVirtualMachine ( connection );
			}
			else
				throw new InternalError ( "missing hostname or port argument" );
		}
	}

	// ---------------------------------------------------------------------

	private VirtualMachine listen()
		throws IOException, IllegalConnectorArgumentsException
	{
		ListeningConnector connector = ( ListeningConnector ) d_connector;
		String address = connector.startListening( d_args );
		Messenger.println( "listening at " + address + "..." );
		VirtualMachine result = connector.accept( d_args );
		connector.stopListening( d_args );
		Messenger.println( "connection accepted" );
		return result;
	}

	// ---------------------------------------------------------------------

	synchronized void init()
	{
		d_virtualMachine.setDebugTraceMode ( VirtualMachine.TRACE_NONE );

		if ( d_virtualMachine.canBeModified() )
		{
			configureTargetMachine ( d_virtualMachine );
		}

		prepareSourceDirectories();
	}

	// ---------------------------------------------------------------------

	public synchronized VirtualMachine getVirtualMachine()
	{
		if ( isConnected() )
			return d_virtualMachine;
		else
			throw new XNotConnected();
	}

	// ---------------------------------------------------------------------

	public synchronized boolean isConnected()
	{
		final boolean result = d_virtualMachine != null;
		return result;
	}

	// ---------------------------------------------------------------------

	public void close()
	{
		try
		{
			closeVirtualMachine();
		}
		finally
		{
			closeProcess();
		}
	}

	// ---------------------------------------------------------------------

	private void closeVirtualMachine()
	{
		if ( isConnected() )
		{
			d_virtualMachine.dispose();
			d_virtualMachine = null;
		}
	}

	// ---------------------------------------------------------------------

	private void closeProcess()
	{
		if ( d_process != null )
		{
			d_process.destroy();
			d_process = null;
		}
	}

	// ---------------------------------------------------------------------

	private void configureTargetMachine ( VirtualMachine vm )
	{
		EventRequestManager eventRequestManager = vm.eventRequestManager();

		ThreadStartRequest threadStartRequest = eventRequestManager.createThreadStartRequest();
		threadStartRequest .enable();

		ThreadDeathRequest threadDeathRequest = eventRequestManager.createThreadDeathRequest();
		threadDeathRequest.enable();

		ClassPrepareRequest classPrepareRequest = eventRequestManager.createClassPrepareRequest();
		classPrepareRequest.enable();

		ClassUnloadRequest classUnloadRequest = eventRequestManager.createClassUnloadRequest();
		classUnloadRequest.enable();
	}

	// ---------------------------------------------------------------------

	private void prepareSourceDirectories()
	{
		if ( d_virtualMachine instanceof PathSearchingVirtualMachine )
		{
			PathSearchingVirtualMachine psvm = ( PathSearchingVirtualMachine ) d_virtualMachine;
			Instance.s_sourceManager.addSourceDirectories ( psvm.classPath() );
		}
		else
			Instance.s_sourceManager.addSourceDirectories ( "." );
	}

	// -----------------------------------------------------------------

	private EKind d_kind;
	private int d_timeOut;

	private Connector d_connector;
	private Map< String, Connector.Argument > d_args;

	private VirtualMachine d_virtualMachine;
	private Process d_process;

	// ---------------------------------------------------------------------
}
