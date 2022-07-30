package wingdbJavaDebugEngine;

import java.util.List;
import java.util.Arrays;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class DebugCustomVariable extends DebugCompoundVariable implements IVariableCallback
{
	DebugCustomVariable(
		String parentId,
		String childId,
		String expression,
		StackFrame frame,
		Type type,
		IDebugVariableValue value,
		IVisualizer visualizer )
	{
		super( parentId, childId, expression, frame, type, value );
		d_visualizer = visualizer;
		d_nativeValue = value.getNativeValue();
	}

	// -----------------------------------------------------------------

	public boolean isScalar()
	{
		boolean result = true;
		try
		{
			result = d_visualizer.isScalar();
		}
		catch ( Exception e )
		{
			Messenger.printException( e );
		}
		return result;
	}

	public String getValueString()
	{
		String result;
		try
		{
			result = d_visualizer.getValueString( d_type, d_nativeValue, this );
		}
		catch ( Exception e )
		{
			Messenger.printException( e );
			result = "";
		}
		return result;
	}

	// -----------------------------------------------------------------

	protected Integer calcChildrenCount()
	{
		Integer result;
		try
		{
			result = d_visualizer.getChildrenCount( d_type, d_nativeValue, this );
		}
		catch ( Exception e )
		{
			Messenger.printException( e );
			result = 0;
		}
		return result;
	}

	// -----------------------------------------------------------------

	protected void generateChildren() throws Exception
	{
		try
		{
			int childrenCount = getChildrenCount();
			d_visualizer.generateChildren( d_type, d_nativeValue, this, childrenCount );
		}
		catch ( Exception e )
		{
			Messenger.printException( e );
		}
	}

	// -----------------------------------------------------------------

	public Value invokeValue( String methodName, Value[] args )
	{
		Value result = invokeByValue( d_nativeValue, methodName, args );
		return result;
	}

	public Value invokeByValue( Value value, String methodName, Value[] args )
	{
		List< Value > argsList = Arrays.asList( args );
		//ThreadReference thread = d_frame.thread();
		ThreadReference thread = null;
		Value result = Instance.invokeMethodInValue( value, methodName, argsList, thread );
		return result;
	}

	public void addField( String id, String label, Type type, Value value ) throws Exception
	{
		addChild( id, label, type, value );
	}

	public void setElementType( Type type ) throws Exception
	{
		d_elementType = type;
	}

	public void addElement( String label, Value value ) throws Exception
	{
		assert d_elementType != null;
		String elementId = d_elementIndex.toString();
		addChild( elementId, label, d_elementType, value );
		++d_elementIndex;
	}

	// -----------------------------------------------------------------

	private final IVisualizer d_visualizer;
	private final Value d_nativeValue;

	private Type d_elementType;
	private Integer d_elementIndex = 0;

}
