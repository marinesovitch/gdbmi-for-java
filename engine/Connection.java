// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.io.*;
import java.net.*;

// ---------------------------------------------------------------------

public class Connection
	extends com.sun.jdi.connect.spi.Connection
	implements Watchdog.IClient
{
	public Connection ( String host, int port, int timeOut ) throws
		UnknownHostException,
		SocketException,
		IOException
	{
		d_bIsOpen = false;
		d_currentErrorReplyPacket = null;

		d_suspendErrorReplyPacket =
			new ProtocolUtils.PacketHeader ( 0, 0, ProtocolUtils.ERROR_VM_DEAD );

		initInputBuffer();

		d_socket = new Socket ( host, port );
		d_inputStream = d_socket.getInputStream();
		d_outputStream = d_socket.getOutputStream();

		try
		{
			d_socket.setTcpNoDelay ( true );
		}
		catch ( Exception error )
		{
		}

		d_watchdog = new Watchdog();
		d_watchdog.set ( this, timeOut );

		if ( ! initProtocol() )
		{
			d_watchdog.finish();
			if ( !d_socket.isClosed() )
				d_socket.close();
			d_socket = null;
			d_inputStream = null;
			d_outputStream = null;

			throw new IOException ( "Failed JDWP handshake." );
		}
		else
		{
			d_watchdog.reset();
		}

		d_bIsOpen = true;
	}

	// ---------------------------------------------------------------------
	// Connection

	public byte[] readPacket() throws IOException
	{
		byte[] newPacket = null;

		try
		{
			readToInputBuffer ( 4 );

			final int packetSize = getBigEndianInt ( 0 );

			readToInputBuffer ( packetSize );

			newPacket = new byte [ packetSize ];
			System.arraycopy ( d_inputBuffer, 0, newPacket, 0, packetSize );

			clearInputBuffer();

			ProtocolUtils.PacketHeader header = new ProtocolUtils.PacketHeader ( newPacket );
			// Logger.begin().log ( "Reading packet: " ).log ( header.toString() ).end();
		}
		catch ( IOException error )
		{
			if ( d_currentErrorReplyPacket != null )
				newPacket = d_currentErrorReplyPacket.serialize ( null );
			else
			{
				d_watchdog.reset();
				throw error;
			}
		}

		d_watchdog.reset();
		d_currentErrorReplyPacket = null;

		return newPacket;
	}

	public void writePacket ( byte pkt[] ) throws IOException
	{
		d_outputStream.write ( pkt );
		d_outputStream.flush();

		ProtocolUtils.PacketHeader header = new ProtocolUtils.PacketHeader ( pkt );
		// Logger.begin().log ( "Writing packet: " ).log ( header.toString() ).end();

		if ( header.d_commandSet == ProtocolUtils.CMDS_VIRTUAL_MACHINE
			 && header.d_command == ProtocolUtils.CMD_VM_SUSPEND )
		{
			d_currentErrorReplyPacket = d_suspendErrorReplyPacket;
			d_currentErrorReplyPacket.d_id = header.d_id;

			d_watchdog.set ( this, 8000 );
		}
	}

	public void close() throws IOException
	{
		if ( d_bIsOpen )
		{
			d_watchdog.finish();
			d_socket.close();
			d_bIsOpen = false;
		}
	}

	public boolean isOpen()
	{
		return d_bIsOpen;
	}

	// ---------------------------------------------------------------------
	// Watchdog.IClient

	public void onWatchdogTimeout()
	{
		try
		{
			d_socket.close();
		}
		catch ( Exception ex )
		{
		}
	}

	// ---------------------------------------------------------------------
	// internal operations

	private void createInputBuffer()
	{
		d_inputBuffer = new byte [ d_inputBufferCapacity ];
	}

	private void growInputBuffer ( int minCapacity )
	{
		while ( d_inputBufferCapacity < minCapacity )
			d_inputBufferCapacity = 2 * d_inputBufferCapacity;

		byte[] oldBuffer = d_inputBuffer;

		createInputBuffer();
		System.arraycopy ( oldBuffer, 0, d_inputBuffer, 0, d_inputBufferDataSize );
	}

	private void initInputBuffer()
	{
		final int initialInputBufferCapacity = 4096;
		d_inputBufferDataSize = 0;
		d_inputBufferCapacity = initialInputBufferCapacity;
		createInputBuffer();
	}

	private void readToInputBuffer ( int destDataSize ) throws
		IOException
	{
		int appendedDataSize = destDataSize - d_inputBufferDataSize;

		if ( destDataSize > d_inputBufferCapacity )
			growInputBuffer ( destDataSize );

		while ( appendedDataSize > 0 )
		{
			final int bytesRead = d_inputStream.read (
				d_inputBuffer, d_inputBufferDataSize, appendedDataSize );

			d_inputBufferDataSize += bytesRead;
			appendedDataSize -= bytesRead;
		}
	}

	private void clearInputBuffer()
	{
		d_inputBufferDataSize = 0;
	}

	private boolean initProtocol()
	{
		final byte[] handshake =
		{
			'J', 'D', 'W', 'P', '-', 'H', 'a', 'n', 'd', 's', 'h', 'a', 'k', 'e'
		};

		try
		{
			d_outputStream.write ( handshake );
			d_outputStream.flush();

			readToInputBuffer ( 14 );
		}
		catch ( Exception error )
		{
			clearInputBuffer();
			return false;
		}

		final boolean bResult = isFragmentEqual ( 0, handshake );

		clearInputBuffer();

		return bResult;
	}

	private int getBigEndianInt ( int offset )
	{
		int result = 0;
		int currentByte = 0;

		currentByte = d_inputBuffer [ offset ];
		result |= ( ( currentByte & 0xFF ) << 24 );

		currentByte = d_inputBuffer [ offset + 1 ];
		result |= ( ( currentByte & 0xFF ) << 16 );

		currentByte = d_inputBuffer [ offset + 2 ];
		result |= ( ( currentByte & 0xFF ) << 8 );

		currentByte = d_inputBuffer [ offset + 3 ];
		result |= ( currentByte & 0xFF );

		return result;
	}

	private boolean isFragmentEqual ( int offset, byte[] pattern )
	{
		final int patternSize = pattern.length;

		if ( offset + patternSize > d_inputBufferDataSize )
			return false;

		for ( int i = 0; i != patternSize; ++i )
			if ( pattern [ i ] != d_inputBuffer [ offset + i ] )
				return false;

		return true;
	}

	// ---------------------------------------------------------------------

	private Socket d_socket;

	private InputStream d_inputStream;
	private OutputStream d_outputStream;

	private int d_inputBufferCapacity;
	private int d_inputBufferDataSize;
	private byte[] d_inputBuffer;

	private boolean d_bIsOpen;

	private Watchdog d_watchdog;
	private volatile ProtocolUtils.PacketHeader d_currentErrorReplyPacket;
	private ProtocolUtils.PacketHeader d_suspendErrorReplyPacket;
}

// ---------------------------------------------------------------------
