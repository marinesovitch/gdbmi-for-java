package wingdbJavaDebugEngine;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import com.sun.jdi.*;

// ---------------------------------------------------------------------

class DebugStringVariable extends DebugCompoundVariable
{
	DebugStringVariable(
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

	public String getValueString()
	{
		String valueString = super.getValueString();
		String result = Utils.escapeValue( valueString, true );
		return result;
	}

	// -----------------------------------------------------------------

	protected Integer calcChildrenCount()
	{
		Integer result = 1; // +1 for field 'length'

		String chars = ensureChars();
		Integer charsCounter = chars.length();
		result += charsCounter;

		return result;
	}

	protected void generateChildren()
	{
		generateChildLength();
		generateChildCharacters();
	}

	private void generateChildLength()
	{
		String chars = ensureChars();
		Integer length = chars.length();
		addChild(
			"len",
			"length",
			DebugVariableTypeFactory.TypeKind.Int,
			length );
	}

	private void generateChildCharacters()
	{
		String chars = ensureChars();
		Integer index = 0;
		CharacterIterator it = new StringCharacterIterator( chars );
		for( Character chr = it.first()
			; chr != CharacterIterator.DONE
			; chr = it.next()
			)
		{
			String newChildId = index.toString();
			String newChildName = '[' + newChildId + ']';
			addChild(
				newChildId,
				newChildName,
				DebugVariableTypeFactory.TypeKind.Char,
				chr );
			++index;
		}
	}

	// -----------------------------------------------------------------

	private String ensureChars()
	{
		if ( d_chars == null )
		{
			d_chars = d_value.getRawString();
			int length = d_chars.length();
			if ( 2 <= length )
			{
				// remove quotations
				d_chars = d_chars.substring( 1, length -1 );
			}
		}
		return d_chars;
	}

	// -----------------------------------------------------------------

	private String d_chars;

}
