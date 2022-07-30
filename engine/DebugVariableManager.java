// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

public class DebugVariableManager
{
	public DebugVariableManager( DebugVariableFactory debugVariableFactory )
	{
		d_debugVariableFactory = debugVariableFactory;
	}

	// -----------------------------------------------------------------

	public IDebugVariable createVariable (
		String expression, StackFrame contextFrame, VirtualMachine vm )
			throws
				XParseError,
				InvocationException,
				InvalidTypeException,
				ClassNotLoadedException,
				IncompatibleThreadStateException
	{
		final Integer currentIndex = ++s_index;
		final String id = "var" + currentIndex.toString();

		final IDebugVariable hVariable
			= d_debugVariableFactory.createVariable ( vm, contextFrame, id, expression );

		d_id2variable.put ( id, hVariable );

		return hVariable;
	}

	// -----------------------------------------------------------------

	public IDebugVariable findVariable ( String id )
		throws XVariableNotFound, Exception
	{
		final int firstDotPos = id.indexOf ( '.' );
		String rootId = null;
		String childPath = null;

		if ( firstDotPos != -1 )
		{
			rootId = id.substring ( 0, firstDotPos );
			childPath = id.substring ( firstDotPos + 1 );
		}
		else
			rootId = id;

		if ( ! d_id2variable.containsKey ( rootId ) )
			throw new XVariableNotFound ( id );

		final IDebugVariable hRootVariable = d_id2variable.get ( rootId );

		if ( childPath != null )
		{
			final IDebugVariable hChildVariable =
				hRootVariable.getChildByPath ( childPath );

			if ( hChildVariable == null )
				throw new XVariableNotFound ( id );

			return hChildVariable;
		}
		else
			return hRootVariable;
	}

	// -----------------------------------------------------------------

	public void deleteVariable ( String id ) throws XVariableNotFound
	{
		if ( d_id2variable.containsKey ( id ) )
			d_id2variable.remove ( id );
		else
			throw new XVariableNotFound ( id );
	}

	// -----------------------------------------------------------------

	final private DebugVariableFactory d_debugVariableFactory;

	private Map< String, IDebugVariable > d_id2variable =
		new HashMap< String, IDebugVariable >();

	private static int s_index = 0;

	// -----------------------------------------------------------------
}

// ---------------------------------------------------------------------
