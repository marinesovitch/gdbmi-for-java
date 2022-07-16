package wingdbJavaDebugEngine;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import com.sun.jdi.*;

// -----------------------------------------------------------------

class DebugNativeVariableValue implements IDebugVariableValue
{
    DebugNativeVariableValue( Value value )
    {
        d_value = value;
    }

    // -----------------------------------------------------------------

    public boolean isNull()
    {
        boolean result = ( d_value == null );
        return result;
    }

    public String getRawString()
    {
        assert !isNull();
        String result = d_value.toString();
        return result;
    }

    public Value getNativeValue()
    {
        return d_value;
    }

    // -----------------------------------------------------------------

    final Value d_value;

} // DebugNativeVariableValue

// -----------------------------------------------------------------
// -----------------------------------------------------------------

class DebugFakeVariableValue implements IDebugVariableValue
{
    DebugFakeVariableValue( Object value )
    {
        d_value = value;
    }

    // -----------------------------------------------------------------

    public boolean isNull()
    {
        boolean result = ( d_value == null );
        return result;
    }

    public String getRawString()
    {
        assert !isNull();
        String result = d_value.toString();
        return result;
    }

    public Value getNativeValue()
    {
        assert false : "don't call getNativeValue() for Fake non-JDI Value!";
        return null;
    }

    // -----------------------------------------------------------------

    final Object d_value;

} // DebugFakeVariableValue

// -----------------------------------------------------------------
// -----------------------------------------------------------------

class DebugVariableValueFactory
{
    IDebugVariableValue createValue( Type type, Value rawValue )
    {
		Value value = null;
		if ( isScalarObject( type ) )
			value = resolveScalarObjectValue( type, rawValue );
		else
			value = rawValue;

        IDebugVariableValue result = new DebugNativeVariableValue( value );
        return result;
    }

    IDebugVariableValue createValue( Object value )
    {
        IDebugVariableValue result = new DebugFakeVariableValue( value );
        return result;
    }

	// -----------------------------------------------------------------

	private boolean isScalarObject( Type type )
	{
		String typeName = type.name();
		boolean result = d_scalarObjectTypes.contains( typeName );
		return result;
	}

	private Value resolveScalarObjectValue( Type type, Value objectValue )
	{
		/*
			scalar values may be represented as objects too, it concerns all types like
			Byte, Integer, Boolean, ...
			user is not interested in viewing object reference handle, but the value it keeps
			to get that value we have to extract Value for Field of name "value"
		*/
		Value result = objectValue;

		if ( ( type instanceof ReferenceType ) && ( objectValue instanceof ObjectReference ) )
		{
			ReferenceType refType = ( ReferenceType ) type;
			Field field = refType.fieldByName( s_scalarObjectValueFieldName );
			if ( field != null )
			{
				ObjectReference scalarObject = ( ObjectReference ) objectValue;
				result = scalarObject.getValue( field );
 			}
		}

		return result;
	}

	// -----------------------------------------------------------------

    private Set< String > initScalarObjectTypes()
    {
		Set< String > scalarObjectTypes
			= new TreeSet< String >(
				Arrays.asList( new String[] {
					Consts.BooleanClassName,
					Consts.CharacterClassName,

					Consts.ByteClassName,
					Consts.ShortClassName,
					Consts.IntegerClassName,
					Consts.LongClassName,

					Consts.FloatClassName,
					Consts.DoubleClassName } ) );

		return scalarObjectTypes;
    }

	// -----------------------------------------------------------------

    private final Set< String > d_scalarObjectTypes = initScalarObjectTypes();

    private final static String s_scalarObjectValueFieldName = "value";

} // DebugVariableValueFactory
