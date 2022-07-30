package wingdbJavaDebugEngine;

import java.util.Map;
import java.util.HashMap;

import com.sun.jdi.*;

// -----------------------------------------------------------------

class DebugVariableFactory
{
	DebugVariableFactory(
		DebugVariableTypeFactory debugVariableTypeFactory,
		DebugVariableValueFactory debugVariableValueFactory )
	{
		d_varTypeFactory = debugVariableTypeFactory;
		d_varValueFactory = debugVariableValueFactory;

		registerCreators();
	}

	// -----------------------------------------------------------------

	IDebugVariable createVariable(
		VirtualMachine vm,
		StackFrame frame,
		String id,
		String expression )
			throws
				XParseError,
				InvocationException,
				InvalidTypeException,
				ClassNotLoadedException,
				IncompatibleThreadStateException
	{
		Expression parsedExpr = ExpressionParser.parse ( expression, vm, frame );
		Type type = parsedExpr.getType();
		Value value = parsedExpr.getValue();

		IDebugVariable result = createVariable( null, id, expression, frame, type, value );
		return result;
	}

	IDebugVariable createVariable(
		String parentId,
		String childId,
		String expression,
		StackFrame frame,
		Type type,
		Value nativeValue )
	{
		IDebugVariableValue value = d_varValueFactory.createValue( type, nativeValue );

		IDebugVariable result
			= createVariable( parentId, childId, expression, frame, type, value );

		return result;
	}

	IDebugVariable createVariable(
		String parentId,
		String childId,
		String expression,
		StackFrame frame,
		DebugVariableTypeFactory.TypeKind typeKind,
		Object rawValue )
	{
		Type type = d_varTypeFactory.getType( typeKind );
		IDebugVariableValue value = d_varValueFactory.createValue( rawValue );

		IDebugVariable result
			= createVariable( parentId, childId, expression, frame, type, value );

		return result;
	}

	IDebugVariable createVariable(
		String expression,
		StackFrame frame,
		Type type,
		Value nativeValue )
	{
		IDebugVariableValue value = d_varValueFactory.createValue( type, nativeValue );

		IDebugVariable result
			= createVariable( null, expression, expression, frame, type, value );

		return result;
	}

	// -----------------------------------------------------------------

	private IDebugVariable createVariable(
		String parentId,
		String childId,
		String expression,
		StackFrame frame,
		Type type,
		IDebugVariableValue value )
	{
		String creatorName = resolveCreatorName( type, value );
		IVariableCreator creator = d_creators.get( creatorName );
		assert creator != null;
		IDebugVariable result
			= creator.createVariable( parentId, childId, expression, frame, type, value );
		return result;
	}

	private String resolveCreatorName( Type type, IDebugVariableValue value )
	{
		String creatorName = null;

		if ( value.isNull() )
			creatorName = NullCreator;
		else if ( hasCustomVisualizer( type ) )
			creatorName = CustomCreator;
		else if ( hasPredefinedCreator( type ) )
			creatorName = type.name();
		else if ( type instanceof PrimitiveType )
			creatorName = ScalarCreator;
		else if ( type instanceof ArrayType )
			creatorName = ArrayCreator;
		else
		{
			assert type instanceof ReferenceType;
			creatorName = ObjectCreator;
		}

		return creatorName;
	}

	private boolean hasCustomVisualizer( Type type )
	{
		boolean result = Instance.s_visualizerManager.isCustomType( type );
		return result;
	}

	private boolean hasPredefinedCreator( Type type )
	{
		String typeName = type.name();
		boolean result = d_creators.containsKey( typeName );
		return result;
	}

	// -----------------------------------------------------------------

	interface IVariableCreator
	{
		abstract IDebugVariable createVariable(
			String parentId,
			String childId,
			String expression,
			StackFrame frame,
			Type type,
			IDebugVariableValue value );
	}

	private void registerCreators()
	{
		registerNullCreator();
		registerScalarCreators();
		registerCompoundCreators();
	}

	private void registerNullCreator()
	{
		registerCreator( NullCreator, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugNullVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value ); }
		} );
	}

	private void registerScalarCreators()
	{
		registerCreator(
			new String[] { ScalarCreator, Consts.BooleanClassName, Consts.FloatClassName, Consts.DoubleClassName },
			new IVariableCreator()
			{
				public IDebugVariable createVariable(
					String parentId,
					String childId,
					String expression,
					StackFrame frame,
					Type type,
					IDebugVariableValue value )
				{ return new DebugScalarVariable(
					parentId,
					childId,
					expression,
					frame,
					type,
					value ); }
			}
		);

		registerCreator( new String[] { "char", Consts.CharacterClassName }, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugCharacterVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value ); }
		} );

		registerCreator( new String[] { "byte", Consts.ByteClassName }, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugIntegerVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value,
				1 ); }
		} );

		registerCreator( new String[] { "short", Consts.ShortClassName }, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugIntegerVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value,
				2 ); }
		} );

		registerCreator( new String[] { "int", Consts.IntegerClassName }, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugIntegerVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value,
				4 ); }
		} );

		registerCreator( new String[] { "long", Consts.LongClassName }, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugIntegerVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value,
				8 ); }
		} );
	}

	private void registerCompoundCreators()
	{
		registerCreator( Consts.StringClassName, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugStringVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value ); }
		} );

		registerCreator( ObjectCreator, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugObjectVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value ); }
		} );

		registerCreator( ArrayCreator, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{ return new DebugArrayVariable(
				parentId,
				childId,
				expression,
				frame,
				type,
				value ); }
		} );

		registerCreator( CustomCreator, new IVariableCreator() {
			public IDebugVariable createVariable(
				String parentId,
				String childId,
				String expression,
				StackFrame frame,
				Type type,
				IDebugVariableValue value )
			{
				IVisualizer visualizer = Instance.s_visualizerManager.getVisualizer( type );
				return new DebugCustomVariable(
					parentId,
					childId,
					expression,
					frame,
					type,
					value,
					visualizer ); }
		} );

	}

	private void registerCreator( String[] creatorNames, IVariableCreator creator )
	{
		for ( String creatorName : creatorNames )
			registerCreator( creatorName, creator );
	}

	private void registerCreator( String creatorName, IVariableCreator creator )
	{
		assert !d_creators.containsKey( creatorName );
		d_creators.put( creatorName, creator );
	}

	// -----------------------------------------------------------------

	final private DebugVariableTypeFactory d_varTypeFactory;
	final private DebugVariableValueFactory d_varValueFactory;

	// -----------------------------------------------------------------

	private Map< String, IVariableCreator > d_creators
		= new HashMap< String, IVariableCreator >();

	// -----------------------------------------------------------------

	private final static String NullCreator = "null";
	private final static String ScalarCreator = "scalar";
	private final static String ObjectCreator = "object";
	private final static String ArrayCreator = "array";
	private final static String CustomCreator = "custom";

}
