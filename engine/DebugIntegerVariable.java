package wingdbJavaDebugEngine;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class DebugIntegerVariable extends DebugFormattedScalarVariable
{
    DebugIntegerVariable(
        String parentId,
        String childId,
        String expression,
        StackFrame frame,
        Type type,
        IDebugVariableValue value,
        int sizeOfValue )
    {
        super( parentId, childId, expression, frame, type, value );
        d_sizeOfValue = sizeOfValue;
    }

    // -----------------------------------------------------------------

    protected String prepareValueString()
    {
        String result = null;
        String decimalValueStr = d_value.getRawString();
        if ( d_format == IDebugVariable.Format.Natural )
            result = decimalValueStr;
        else
        {
            assert d_format == IDebugVariable.Format.Hexadecimal;
            result = prepareHexValueString( decimalValueStr );
        }
        return result;
    }

    private String prepareHexValueString( String decimalValueStr )
    {
		Long intValue = Long.parseLong( decimalValueStr );
        String rawHexValueStr = Long.toHexString( intValue );
        String hexValueStr = enforceValueLength( rawHexValueStr );
        String result = "0x" + hexValueStr;
        return result;
    }

	private String enforceValueLength( String rawHexValueStr )
	{
		// for each byte there may be max 2 hex-digits
		int maxDigitCount = d_sizeOfValue * 2;
		int digitCount = rawHexValueStr.length();
		int firstIndex = 0;

		if ( maxDigitCount < digitCount )
			firstIndex = digitCount - maxDigitCount;

		String result = rawHexValueStr.substring( firstIndex );
		return result;
	}

    // -----------------------------------------------------------------

    final int d_sizeOfValue;

}
