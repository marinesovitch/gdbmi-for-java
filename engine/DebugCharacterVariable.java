package wingdbJavaDebugEngine;

import com.sun.jdi.*;

class DebugCharacterVariable extends DebugFormattedScalarVariable
{
	DebugCharacterVariable(
		String parentId,
		String childId,
		String expression,
		StackFrame frame,
		Type type,
		IDebugVariableValue value )
	{
		super( parentId, childId, expression, frame, type, value );
	}

	// -----------------------------------------------------------------

	protected String prepareValueString()
	{
		String valueStr = null;
		String rawValueStr = d_value.getRawString();
		if ( !rawValueStr.isEmpty() )
		{
			Character chr = rawValueStr.charAt( 0 );
			int radix = 0;
			if ( d_format == IDebugVariable.Format.Natural )
				radix = 10;
			else
				radix = 16;
			valueStr = prepareCharacterValue( chr, radix );
		}
		else
		{
			valueStr = rawValueStr;
		}

		String result = Utils.escapeValue( valueStr, false );
		return result;
	}

	private String prepareCharacterValue( Character chr, int radix )
	{
		String chrValueStr = prepareCharacterStrValue( chr );
		String intValueStr = prepareCharacterIntValue( chr, radix );
		String result = intValueStr + " '" + chrValueStr + "'";
		return result;
	}

	private String prepareCharacterStrValue( Character chr )
	{
		switch ( chr )
		{
			case 0: return "\\0";
			case 1: case 2: case 3: case 4: case 5: case 6:
				return "\\00" + Integer.toOctalString ( ( int ) chr.charValue() );
			case 7: return "\\a";
			case 8: return "\\b";
			case 9: return "\\t";
			case 10: return "\\n";
			case 11: return "\\v";
			case 12: return "\\f";
			case 13: return "\\r";
			case 14: case 15: case 16: case 17: case 18: case 19:
			case 20: case 21: case 22: case 23: case 24: case 25:
			case 26: case 27: case 28: case 29: case 30: case 31:
				return "\\0" + Integer.toOctalString ( ( int ) chr.charValue() );
			case 92: return "\\\\";
			default: return chr.toString();
		}
	}

	private String prepareCharacterIntValue( Character chr, int radix )
	{
		int intValue = chr.charValue();
		String result = Integer.toString( intValue, radix );
		return result;
	}

}
