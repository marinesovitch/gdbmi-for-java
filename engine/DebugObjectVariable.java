package wingdbJavaDebugEngine;

import java.util.List;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class DebugObjectVariable extends DebugCompoundVariable
{
	DebugObjectVariable (
		String parentId,
		String childId,
		String expression,
		StackFrame frame,
		Type type,
		IDebugVariableValue value )
	{
		super( parentId, childId, expression, frame, type, value );

		assert ( type instanceof ClassType ) || ( type instanceof InterfaceType );
		d_refType = ( ReferenceType ) type;
		d_refObject = ( ObjectReference ) value.getNativeValue();
	}

	// -----------------------------------------------------------------

	protected Integer calcChildrenCount()
	{
		Integer result = 0;

		if ( d_refType instanceof ClassType )
		{
			ClassType classType = ( ClassType ) d_refType;
			final ClassType superClass = classType.superclass();
			if ( isProperSuperClass( superClass ) )
				++result;
		}

		List< Field > classFields = d_refType.fields();
		result += classFields.size();

		return result;
	}

	// -----------------------------------------------------------------

	protected void generateChildren() throws ClassNotLoadedException
	{
		if ( d_refType instanceof ClassType )
		{
			final ClassType classType = ( ClassType ) d_type;
			generateSuperClassChild( classType );
			generateFieldChildren( classType );
		}
	}

	private void generateSuperClassChild( ClassType classType )
	{
		final ClassType superClass = classType.superclass();

		if ( isProperSuperClass( superClass ) )
		{
			String childId = "super";
			String superClassName = superClass.name();
			addChild( childId, superClassName, superClass, d_refObject );
		}
	}

	private void generateFieldChildren( ClassType classType ) throws ClassNotLoadedException
	{
		List< Field > classFields = classType.fields();
		for ( Field field : classFields )
		{
			String childId = field.name();
			String childName = field.name();
			Value childValue = d_refObject.getValue ( field );

			Type fieldType = null;

			try
			{
				fieldType = field.type();
			}
			catch ( Exception ex )
			{
				fieldType = new PseudoType ( field.virtualMachine(), field.typeName() );
			}

			addChild( childId, childName, fieldType, childValue );
		}
	}

	private boolean isProperSuperClass( ClassType superClass )
	{
		boolean result = true;
		if ( ( superClass == null ) || superClass.name().equals( JavaLangObjectClassName ) )
			result = false;
		return result;
	}

	// -----------------------------------------------------------------

	private final ReferenceType d_refType;
	private final ObjectReference d_refObject;

	// -----------------------------------------------------------------

	private static String JavaLangObjectClassName = "java.lang.Object";

}
