package wingdbJavaDebugEngine;

import java.util.List;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class DebugArrayVariable extends DebugCompoundVariable
{
	DebugArrayVariable (
		String parentId,
		String childId,
		String expression,
		StackFrame frame,
		Type type,
		IDebugVariableValue value )
	{
		super( parentId, childId, expression, frame, type, value );

		assert type instanceof ArrayType;
		Value nativeValue = d_value.getNativeValue();
		d_refArray = ( ArrayReference ) nativeValue;
	}

	// -----------------------------------------------------------------

	protected Integer calcChildrenCount()
	{
		Integer result = d_refArray.length();
		return result;
	}

	// -----------------------------------------------------------------

	protected void generateChildren() throws ClassNotLoadedException
	{
		final List< Value > allValues = d_refArray.getValues();
		Integer index = 0;
		final ArrayType d_arrayType = ( ArrayType ) d_type;
		final Type arrayElementType = d_arrayType.componentType();
		for ( Value value : allValues )
		{
			String newChildId = index.toString();
			String newChildName = '[' + newChildId + ']';
			addChild( newChildId, newChildName, arrayElementType, value );
			++index;
		}
	}

	// -----------------------------------------------------------------

	private final ArrayReference d_refArray;

}
