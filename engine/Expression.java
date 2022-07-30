// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.*;
import java.util.*;

// ---------------------------------------------------------------------

abstract class Expression
{
	public Value getValue() throws XParseError
	{
		return null;
	}

	public Type getType() throws XParseError
	{
		return null;
	}
}
