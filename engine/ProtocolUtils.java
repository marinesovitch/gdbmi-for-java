// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

// ---------------------------------------------------------------------

public class ProtocolUtils
{
	public static final int CMDS_VIRTUAL_MACHINE = 1;
	public static final int CMDS_REFERENCE_TYPE = 2;
	public static final int CMDS_CLASS_TYPE = 3;
	public static final int CMDS_ARRAY_TYPE = 4;
	public static final int CMDS_INTERFACE_TYPE = 5;
	public static final int CMDS_METHOD = 6;
	public static final int CMDS_FIELD = 8;
	public static final int CMDS_OBJECT_REFERENCE = 9;
	public static final int CMDS_STRING_REFERENCE = 10;
	public static final int CMDS_THREAD_REFERENCE = 11;
	public static final int CMDS_THREAD_GROUP_REFERENCE = 12;
	public static final int CMDS_ARRAY_REFERENCE = 13;
	public static final int CMDS_CLASS_LOADER_REFERENCE = 14;
	public static final int CMDS_EVENT_REQUEST = 15;
	public static final int CMDS_STACK_FRAME = 16;
	public static final int CMDS_CLASS_OBJECT_REFERENCE = 17;
	public static final int CMDS_EVENT = 64;

	public static final int CMD_VM_VERSION = 1;
	public static final int CMD_VM_CLASSESBYSIGNATURE = 2;
	public static final int CMD_VM_ALLCLASSES = 3;
	public static final int CMD_VM_ALLTHREADS = 4;
	public static final int CMD_VM_TOPLEVELTHREADGROUPS = 5;
	public static final int CMD_VM_DISPOSE = 6;
	public static final int CMD_VM_IDSIZES = 7;
	public static final int CMD_VM_SUSPEND = 8;
	public static final int CMD_VM_RESUME = 9;
	public static final int CMD_VM_EXIT = 10;
	public static final int CMD_VM_CREATESTRING = 11;
	public static final int CMD_VM_CAPABILITIES = 12;
	public static final int CMD_VM_CLASSPATHS = 13;
	public static final int CMD_VM_DISPOSEOBJECTS = 14;
	public static final int CMD_VM_HOLDEVENTS = 15;
	public static final int CMD_VM_RELEASEEVENTS = 16;
	public static final int CMD_VM_CAPABILITIESNEW = 17;
	public static final int CMD_VM_REDEFINECLASSES = 18;
	public static final int CMD_VM_SETDEFAULTSTRATUM = 19;

	public static final int CMD_RT_SIGNATURE = 1;
	public static final int CMD_RT_CLASSLOADER = 2;
	public static final int CMD_RT_MODIFIERS = 3;
	public static final int CMD_RT_FIELDS = 4;
	public static final int CMD_RT_METHODS = 5;
	public static final int CMD_RT_GETVALUES = 6;
	public static final int CMD_RT_SOURCEFILE = 7;
	public static final int CMD_RT_NESTEDTYPES = 8;
	public static final int CMD_RT_STATUS = 9;
	public static final int CMD_RT_INTERFACES = 10;
	public static final int CMD_RT_CLASSOBJECT = 11;
	public static final int CMD_RT_SOURCEDEBUGEXTENSION = 12;

	public static final int CMD_CT_SUPERCLASS = 1;
	public static final int CMD_CT_SETVALUES = 2;
	public static final int CMD_CT_INVOKEMETHOD = 3;
	public static final int CMD_CT_NEWINSTANCE = 4;

	public static final int CMD_AT_NEWINSTANCE = 1;

	public static final int CMD_M_LINETABLE = 1;
	public static final int CMD_M_VARIABLETABLE = 2;
	public static final int CMD_M_BYTECODES = 3;
	public static final int CMD_M_ISOBSOLETE = 4;

	public static final int CMD_OR_REFERENCETYPE = 1;
	public static final int CMD_OR_GETVALUES = 2;
	public static final int CMD_OR_SETVALUES = 3;
	public static final int CMD_OR_MONITORINFO = 5;
	public static final int CMD_OR_INVOKEMETHOD = 6;
	public static final int CMD_OR_DISABLECOLLECTION = 7;
	public static final int CMD_OR_ENABLECOLLECTION = 8;
	public static final int CMD_OR_ISCOLLECTED = 9;

	public static final int CMD_SR_VALUE = 1;

	public static final int CMD_TR_NAME = 1;
	public static final int CMD_TR_SUSPEND = 2;
	public static final int CMD_TR_RESUME = 3;
	public static final int CMD_TR_STATUS = 4;
	public static final int CMD_TR_THREADGROUP = 5;
	public static final int CMD_TR_FRAMES = 6;
	public static final int CMD_TR_FRAMECOUNT = 7;
	public static final int CMD_TR_OWNEDMONITORS = 8;
	public static final int CMD_TR_CURRENTCONTENDEDMONITOR = 9;
	public static final int CMD_TR_STOP = 10;
	public static final int CMD_TR_INTERRUPT = 11;
	public static final int CMD_TR_SUSPENDCOUNT = 12;

	public static final int CMD_TGR_NAME = 1;
	public static final int CMD_TGR_PARENT = 2;
	public static final int CMD_TGR_CHILDREN = 3;

	public static final int CMD_AR_LENGTH = 1;
	public static final int CMD_AR_GETVALUES = 2;
	public static final int CMD_AR_SETVALUES = 3;

	public static final int CMD_CLR_VISIBLECLASSES = 1;

	public static final int CMD_ER_SET = 1;
	public static final int CMD_ER_CLEAR = 2;
	public static final int CMD_ER_CLEARALLBREAKPOINTS = 3;

	public static final int CMD_SF_GETVALUES = 1;
	public static final int CMD_SF_SETVALUES = 2;
	public static final int CMD_SF_THISOBJECT = 3;
	public static final int CMD_SF_POPFRAMES = 4;

	public static final int CMD_COR_REFLECTEDTYPE = 1;

	public static final int CMD_E_COMPOSITE = 100;

	public static final int ERROR_VM_DEAD = 112;

	public interface IPacketDataSerializer
	{
		void serializePacketData ( DataOutputStream stream );
	}

	public static class PacketHeader
	{
		public PacketHeader ( byte[] data ) throws IOException
		{
			d_length = 0;
			d_id = 0;
			d_flags = 0;
			d_commandSet = 0;
			d_command = 0;
			d_errorCode = 0;

			final ByteArrayInputStream inputStream = new ByteArrayInputStream ( data );
			final DataInputStream dataStream = new DataInputStream ( inputStream );

			d_length = dataStream.readInt();
			d_id = dataStream.readInt();
			d_flags = ( dataStream.readByte() & 0xFF );

			final boolean bReply = ( ( d_flags & 0x80 ) != 0 );

			if ( ! bReply )
			{
				d_commandSet = ( dataStream.readByte() & 0xFF );
				d_command = ( dataStream.readByte() & 0xFF );
			}
			else
				d_errorCode = ( dataStream.readShort() & 0xFFFF );
		}

		public PacketHeader ( int dataLength, int id, int commandSet, int command )
		{
			makeCommand ( dataLength, id, commandSet, command );
		}

		public PacketHeader ( int dataLength, int id, int errorCode )
		{
			makeReply ( dataLength, id, errorCode );
		}

		public void makeCommand ( int dataLength, int id, int commandSet, int command )
		{
			d_length = dataLength + 11;
			d_id = id;
			d_flags = 0;
			d_commandSet = commandSet;
			d_command = command;
			d_errorCode = 0;
		}

		public void makeReply ( int dataLength, int id, int errorCode )
		{
			d_length = dataLength + 11;
			d_id = id;
			d_flags = 0x80;
			d_commandSet = 0;
			d_command = 0;
			d_errorCode = errorCode;
		}

		public byte[] serialize ( IPacketDataSerializer serializer ) throws IOException
		{
			final ByteArrayOutputStream arrayStream = new ByteArrayOutputStream ( d_length );
			final DataOutputStream dataStream = new DataOutputStream ( arrayStream );

			dataStream.writeInt ( d_length );
			dataStream.writeInt ( d_id );
			dataStream.writeByte ( ( byte ) d_flags );

			final boolean bReply = ( ( d_flags & 0x80 ) != 0 );

			if ( ! bReply )
			{
				dataStream.writeByte ( ( byte ) d_commandSet );
				dataStream.writeByte ( ( byte ) d_command );
			}
			else
				dataStream.writeShort ( ( short ) d_errorCode );

			if ( serializer != null )
				serializer.serializePacketData ( dataStream );

			return arrayStream.toByteArray();
		}

		public String getPacketName()
		{
			switch ( d_commandSet )
			{
				case CMDS_VIRTUAL_MACHINE:
					switch ( d_command )
					{
						case CMD_VM_VERSION: return "VirtualMachine.Version";
						case CMD_VM_CLASSESBYSIGNATURE: return "VirtualMachine.ClassesBySignature";
						case CMD_VM_ALLCLASSES: return "VirtualMachine.AllClasses";
						case CMD_VM_ALLTHREADS: return "VirtualMachine.AllThreads";
						case CMD_VM_TOPLEVELTHREADGROUPS: return "VirtualMachine.TopLevelThreadGroups";
						case CMD_VM_DISPOSE: return "VirtualMachine.Dispose";
						case CMD_VM_IDSIZES: return "VirtualMachine.IdSizes";
						case CMD_VM_SUSPEND: return "VirtualMachine.Suspend";
						case CMD_VM_RESUME: return "VirtualMachine.Resume";
						case CMD_VM_EXIT: return "VirtualMachine.Exit";
						case CMD_VM_CREATESTRING: return "VirtualMachine.CreateString";
						case CMD_VM_CAPABILITIES: return "VirtualMachine.Capabilities";
						case CMD_VM_CLASSPATHS: return "VirtualMachine.ClassPaths";
						case CMD_VM_DISPOSEOBJECTS: return "VirtualMachine.DisposeObjects";
						case CMD_VM_HOLDEVENTS: return "VirtualMachine.HoldEvents";
						case CMD_VM_RELEASEEVENTS: return "VirtualMachine.ReleaseEvents";
						case CMD_VM_CAPABILITIESNEW: return "VirtualMachine.CapabilitiesNew";
						case CMD_VM_REDEFINECLASSES: return "VirtualMachine.RedefineClasses";
						case CMD_VM_SETDEFAULTSTRATUM: return "VirtualMachine.SetDefaultStratum";
						default: return "VirtualMachine.Unknown";
					}

				case CMDS_REFERENCE_TYPE:
					switch ( d_command )
					{
						case CMD_RT_SIGNATURE: return "ReferenceType.Signature";
						case CMD_RT_CLASSLOADER: return "ReferenceType.ClassLoader";
						case CMD_RT_MODIFIERS: return "ReferenceType.Modifiers";
						case CMD_RT_FIELDS: return "ReferenceType.Fields";
						case CMD_RT_METHODS: return "ReferenceType.Methods";
						case CMD_RT_GETVALUES: return "ReferenceType.GetValues";
						case CMD_RT_SOURCEFILE: return "ReferenceType.SourceFile";
						case CMD_RT_NESTEDTYPES: return "ReferenceType.NestedTypes";
						case CMD_RT_STATUS: return "ReferenceType.Status";
						case CMD_RT_INTERFACES: return "ReferenceType.Interfaces";
						case CMD_RT_CLASSOBJECT: return "ReferenceType.ClassObject";
						case CMD_RT_SOURCEDEBUGEXTENSION: return "ReferenceType.SourceDebugExtension";
						default: return "ReferenceType.Unknown";
					}

				case CMDS_CLASS_TYPE:
					switch ( d_command )
					{
						case CMD_CT_SUPERCLASS: return "ClassType.SuperClass";
						case CMD_CT_SETVALUES: return "ClassType.SetValues";
						case CMD_CT_INVOKEMETHOD: return "ClassType.InvokeMethod";
						case CMD_CT_NEWINSTANCE: return "ClassType.NewInstance";
						default: return "ClassType.Unknown";
					}

				case CMDS_ARRAY_TYPE:
					switch ( d_command )
					{
						case CMD_AT_NEWINSTANCE: return "ArrayType.NewInstance";
						default: return "ArrayType.Unknown";
					}

				case CMDS_INTERFACE_TYPE:
					switch ( d_command )
					{
						default: return "InterfaceType.Unknown";
					}

				case CMDS_METHOD:
					switch ( d_command )
					{
						case CMD_M_LINETABLE: return "Method.LineTable";
						case CMD_M_VARIABLETABLE: return "Method.VariableTable";
						case CMD_M_BYTECODES: return "Method.ByteCodes";
						case CMD_M_ISOBSOLETE: return "Method.IsObsolete";
						default: return "Method.Unknown";
					}

				case CMDS_FIELD:
					switch ( d_command )
					{
						default: return "Field.Unknown";
					}

				case CMDS_OBJECT_REFERENCE:
					switch ( d_command )
					{
						case CMD_OR_REFERENCETYPE: return "ObjectReference.ReferenceType";
						case CMD_OR_GETVALUES: return "ObjectReference.GetValues";
						case CMD_OR_SETVALUES: return "ObjectReference.SetValues";
						case CMD_OR_MONITORINFO: return "ObjectReference.MonitorInfo";
						case CMD_OR_INVOKEMETHOD: return "ObjectReference.InvokeMethod";
						case CMD_OR_DISABLECOLLECTION: return "ObjectReference.DisableCollection";
						case CMD_OR_ENABLECOLLECTION: return "ObjectReference.EnableCollection";
						case CMD_OR_ISCOLLECTED: return "ObjectReference.IsCollected";
						default: return "ObjectReference.Unknown";
					}

				case CMDS_STRING_REFERENCE:
					switch ( d_command )
					{
						case CMD_SR_VALUE: return "StringReference.Value";
						default: return "StringReference.Unknown";
					}

				case CMDS_THREAD_REFERENCE:
					switch ( d_command )
					{
						case CMD_TR_NAME: return "ThreadReference.Name";
						case CMD_TR_SUSPEND: return "ThreadReference.Suspend";
						case CMD_TR_RESUME: return "ThreadReference.Resume";
						case CMD_TR_STATUS: return "ThreadReference.Status";
						case CMD_TR_THREADGROUP: return "ThreadReference.ThreadGroup";
						case CMD_TR_FRAMES: return "ThreadReference.Frames";
						case CMD_TR_FRAMECOUNT: return "ThreadReference.FrameCount";
						case CMD_TR_OWNEDMONITORS: return "ThreadReference.OwnedMonitors";
						case CMD_TR_CURRENTCONTENDEDMONITOR: return "ThreadReference.CurrentContendedMonitor";
						case CMD_TR_STOP: return "ThreadReference.Stop";
						case CMD_TR_INTERRUPT: return "ThreadReference.Interrupt";
						case CMD_TR_SUSPENDCOUNT: return "ThreadReference.SuspendCount";
						default: return "ThreadReference.Unknown";
					}

				case CMDS_THREAD_GROUP_REFERENCE:
					switch ( d_command )
					{
						case CMD_TGR_NAME: return "ThreadGroupReference.Name";
						case CMD_TGR_PARENT: return "ThreadGroupReference.Parent";
						case CMD_TGR_CHILDREN: return "ThreadGroupReference.Children";
						default: return "ThreadGroupReference.Unknown";
					}

				case CMDS_ARRAY_REFERENCE:
					switch ( d_command )
					{
						case CMD_AR_LENGTH: return "ArrayReference.Length";
						case CMD_AR_GETVALUES: return "ArrayReference.GetValues";
						case CMD_AR_SETVALUES: return "ArrayReference.SetValues";
						default: return "ArrayReference.Unknown";
					}

				case CMDS_CLASS_LOADER_REFERENCE:
					switch ( d_command )
					{
						case CMD_CLR_VISIBLECLASSES: return "ClassLoaderReference.VisibleClasses";
						default: return "ClassLoaderReference.Unknown";
					}

				case CMDS_EVENT_REQUEST:
					switch ( d_command )
					{
						case CMD_ER_SET: return "EventRequest.Set";
						case CMD_ER_CLEAR: return "EventRequest.Clear";
						case CMD_ER_CLEARALLBREAKPOINTS: return "EventRequest.ClearAllBreakpoints";
						default: return "EventRequest.Unknown";
					}

				case CMDS_STACK_FRAME:
					switch ( d_command )
					{
						case CMD_SF_GETVALUES: return "StackFrame.GetValues";
						case CMD_SF_SETVALUES: return "StackFrame.SetValues";
						case CMD_SF_THISOBJECT: return "StackFrame.ThisObject";
						case CMD_SF_POPFRAMES: return "StackFrame.PopFrames";
						default: return "StackFrame.Unknown";
					}

				case CMDS_CLASS_OBJECT_REFERENCE:
					switch ( d_command )
					{
						case CMD_COR_REFLECTEDTYPE: return "ClassObject.ReflectedType";
						default: return "ClassObject.Unknown";
					}

				case CMDS_EVENT:
					switch ( d_command )
					{
						case CMD_E_COMPOSITE: return "Event.Composite";
						default: return "Event.Unknown";
					}

				default:
					return "Unknown.Unknown";
			}
		}

		public String toString()
		{
			StringBuffer sst = new StringBuffer();

			if ( d_commandSet != 0 )
			{
				sst.append ( "CMD ( " );
				sst.append ( "Length=" );
				sst.append ( d_length );
				sst.append ( ", id=" );
				sst.append ( d_id );
				sst.append ( ", flags=" );
				sst.append ( d_flags );
				sst.append ( ", name=" );
				sst.append ( getPacketName() );
				sst.append ( " )" );
			}
			else
			{
				sst.append ( "RPL ( " );
				sst.append ( "Length=" );
				sst.append ( d_length );
				sst.append ( ", id=" );
				sst.append ( d_id );
				sst.append ( ", flags=" );
				sst.append ( d_flags );
				sst.append ( ", error=" );
				sst.append ( d_errorCode );
				sst.append ( " )" );
			}

			return sst.toString();
		}

		public int d_length;
		public int d_id;
		public int d_flags;

		public int d_commandSet;
		public int d_command;

		public int d_errorCode;
	}
}

// ---------------------------------------------------------------------
