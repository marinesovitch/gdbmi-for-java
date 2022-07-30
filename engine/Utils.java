// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import com.sun.jdi.ReferenceType;

// ---------------------------------------------------------------------

class Utils
{

	static void Sleep( long miliseconds )
	{
		try
		{
			Thread.sleep( miliseconds );
		}
		catch ( InterruptedException e )
		{
		}
	}

	// ---------------------------------------------------------------------

	static boolean checkFlag( long lhs, long rhs )
	{
		boolean result = ( lhs & rhs ) == rhs;
		return result;
	}

	// ---------------------------------------------------------------------

	static boolean isNotEmpty( String str )
	{
		boolean result = ( str != null ) && !str.isEmpty();
		return result;
	}

	static boolean hasWhitespace ( String string )
	{
		int length = string.length();
		for ( int i = 0; i < length; i++ )
		{
			Character chr = string.charAt( i );
			if ( Character.isWhitespace( chr ) )
			{
				return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------

	static String toAddressString( long address )
	{
		String result = "0x" + Long.toHexString( address );
		return result;
	}

	static long parseAddress ( String strAddress )
	{
		int base = 10;

		String effectiveAddress = strAddress;

		if ( strAddress.startsWith ( "0x" ) || strAddress.startsWith ( "0X" ) )
		{
			base = 16;
			effectiveAddress = strAddress.substring ( 2 );
		}

		final BigInteger biAddress = new BigInteger ( effectiveAddress, base );
		return biAddress.longValue();
	}

	// ---------------------------------------------------------------------

	static String unquote ( String src )
	{
		String result = src;

		if ( src.length() > 1
			 && src.startsWith ( "\"" )
			 && src.endsWith ( "\"" ) )
		{
			result = src.substring ( 1, src.length() - 1 );
		}

		return result;
	}

	static List< String > getTokens ( StringTokenizer t )
	{
		ArrayList< String > result = new ArrayList< String >();

		while ( t.hasMoreTokens() )
		{
			final String token = t.nextToken();
			result.add ( token );
		}

		return result;
	}

	static String getRemainingText( StringTokenizer st )
	{
		String result = getRemainingText( st, "\0" );
		return result;
	}

	static String getRemainingText( StringTokenizer st, String terminator )
	{
		String result = st.nextToken( terminator ).trim();
		return result;
	}

	static boolean containWhiteSpace( String token )
	{
		for ( char c : token.toCharArray() )
		{
			if ( Character.isWhitespace( c ) )
			{
			   return true;
			}
		}
		return false;
	}

	static boolean isQuoted( String value )
	{
		final String Quotation = "\"";
		boolean result = ( value != null ) && ( 2 <= value.length() )
			&& value.startsWith( Quotation ) && value.endsWith( Quotation );
		return result;
	}

	static String escapeValue( String value, boolean onlyIfQuotation )
	{
		String result = null;

		if ( ( value != null ) && ( !onlyIfQuotation || isQuoted( value ) ) )
		{
			StringBuffer sb = new StringBuffer();
			CharacterIterator it = new StringCharacterIterator( value );
			for( char chr = it.first()
				; chr != CharacterIterator.DONE
				; chr = it.next()
				)
			{
				String escapedStr = d_escape2str.get( chr );
				if ( escapedStr == null )
					sb.append( chr );
				else
					sb.append( escapedStr );
			}
			result = sb.toString();
		}
		else
		{
			result = value;
		}
		return result;
	}

	private static Hashtable< Character, String > initEscapeMappings()
	{
		Hashtable< Character, String > mappings = new Hashtable< Character, String >();
		mappings.put( '\\', "\\\\" );
		mappings.put( '\"', "\\\"" );
//	    mappings.put( '\a', "\\a" );
		mappings.put( '\b', "\\b" );
		mappings.put( '\f', "\\f" );
		mappings.put( '\n', "\\n" );
		mappings.put( '\r', "\\r" );
		mappings.put( '\t', "\\t" );
//	    mappings.put( '\v', "\\v" );

		return mappings;
	}

	static String quoteToken( String rawToken )
	{
		String result = rawToken;
		if ( containWhiteSpace( rawToken ) )
		{
			if ( rawToken.charAt( 0 ) != '"' )
				result = "\"" + rawToken + "\"";
		}
		return result;
	}

	static String quoteTokens( String rawTokens, String separator )
	{
		StringBuffer sb = new StringBuffer();
		StringTokenizer st = new StringTokenizer( rawTokens, separator );
		boolean firstToken = true;
		while ( st.hasMoreTokens() )
		{
			if ( firstToken )
				firstToken = false;
			else
				sb.append( separator );

			String rawToken = st.nextToken();
			String token = quoteToken( rawToken );
			sb.append( token );
		}
		String result = sb.toString();
		return result;
	}

	// ---------------------------------------------------------------------

	static boolean isUNCPath( String path )
	{
		final String UNCPrefix = "\\\\";
		boolean result = path.startsWith( UNCPrefix );
		return result;
	}

	static String adjustPath( String rawPath )
	{
		/*
			!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			CAUTION!!!
			clone of C++ routine PlatformWindows::adjustPath
			source: .\code\Tellurium\controllers\controller\detail\conPlatform.cpp
			!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

			on Windows we have to convert backslashes to slashes, because backslashes
			need escaping while passing arguments to commands, but some GDB commands
			doesn't accept such escaped paths

			thus far according to our experience GDB on windows always accepts paths with slashes

			possible paths
			C:\\Windows\\system32\\ntdll.dll
			C:\\test\\gdb_test\\gdb_test/./Debug/test.elf
			c:\\program files\\codesourcery\\sourcery g++ lite\\bin\\../arm-none-linux-gnueabi/libc/usr/include/stdio.h
			c:\\program files\\nativesdk\\include\\SDL/SDL_keysym.h
			C:\\Program Files\\NativeSDK\\lib/libGLES_CM.so
			c:\\test\\gdb_test\\gdb_test/entry.s
			C:\Windows\system32\msvcrt.dll
			\\\\builder\\sources\\bio\\main.cpp
			\\builder\sources\bio2\asdbTester.exe
		*/
		String result = null;

		final boolean isUNC = isUNCPath( rawPath );
		if ( isUNC )
		{
			result = rawPath.substring( 2 );
			if ( isUNCPath( result ) )
			{
				/*
					for such case:
						\\\\builder\\sources\\bio\\main.cpp
					we have to cut prefix twice to get:
						builder\\sources\\bio\\main.cpp
				*/
				result = result.substring( 2 );
			}
		}
		else
		{
			result = rawPath;
		}

		result = result.replace( "\\\\", "\\" );
		result = result.replace( "\\", "/" );

		if ( isUNC )
		{
			//CAUTION! this is NOT BUG! prefix should be "//" not "\\\\"
			final String UNCPrefix = "//";
			result = UNCPrefix + result;
		}

		return result;
	}

	// ---------------------------------------------------------------------

	static Pair< String, String > extractBranchAndName(
		String rawFullSpec,
		Character separator,
		boolean removeInternalName )
	{
		Pair< String, String > result = new Pair< String, String >();

		String fullSpec = rawFullSpec;

		if ( removeInternalName )
		{
			int endOfMainClassNameIndex = rawFullSpec.indexOf( '$' );
			if ( endOfMainClassNameIndex != -1 )
				fullSpec = rawFullSpec.substring( 0, endOfMainClassNameIndex );
		}

		int endOfPackageIndex = fullSpec.lastIndexOf( separator );
		if ( endOfPackageIndex != -1 )
		{
			result.first = fullSpec.substring( 0, endOfPackageIndex );
			result.second = fullSpec.substring( endOfPackageIndex + 1 );
		}
		else
		{
			result.first = "";
			result.second = fullSpec;
		}

		return result;
	}

	static Pair< String, String > extractClassPackageAndName(
		ReferenceType refType,
		boolean removeInternalClassName )
	{
		final String classFullName = refType.name();
		Pair< String, String > result
			= extractBranchAndName( classFullName, '.', removeInternalClassName );
		return result;
	}

	static Pair< String, String > extractDirAndFileName( String rawPath )
	{
		Pair< String, String > result
			= extractBranchAndName( rawPath, '/', false );
		return result;
	}

	// ---------------------------------------------------------------------

	static String extractMethodNameFromSpec( String rawMethodSpec )
	{
		String methodSpec = rawMethodSpec.replace( " ", "" );

		int openParenthesisIndex = methodSpec.lastIndexOf( '(' );
		if ( openParenthesisIndex == -1 )
			openParenthesisIndex = methodSpec.length();

		String result = methodSpec.substring( 0, openParenthesisIndex );
		return result;
	}

	/*
		extracts ClassName and MethodName, removes package
		samples:
		0) com.wingdb.javaDebugEngine.MethodManager.indexoffset2address
		1) wingdb.javaDebugEngine.MethodManager.indexoffset2address
		2) javaDebugEngine.MethodManager.indexoffset2address
		3) MethodManager.indexoffset2address

		result:
		MethodManager.indexoffset2address
	*/
	static String extractShortMethodNameFromSpec( String rawMethodSpec )
	{
		String result = null;

		String methodFullSpec = extractMethodNameFromSpec( rawMethodSpec );
		boolean extracted = false;
		int beginMethodNameIndex = methodFullSpec.lastIndexOf( '.' );
		if ( ( beginMethodNameIndex != -1 ) && ( 0 < beginMethodNameIndex ) )
		{
			int beginShortClassNameIndex = methodFullSpec.lastIndexOf( '.', beginMethodNameIndex - 1 );
			if ( beginShortClassNameIndex != -1 )
			{
				result = methodFullSpec.substring( beginShortClassNameIndex + 1 );
				extracted = true;
			}
		}

		if ( !extracted )
			result = methodFullSpec;

		return result;
	}

	// ---------------------------------------------------------------------

	static String extractClassNameFromSpec( String methodSpec )
	{
		String result = null;

		String methodName = extractMethodNameFromSpec( methodSpec );

		int beginMethodNameIndex = methodName.lastIndexOf( '.' );
		if ( beginMethodNameIndex != -1 )
			result = methodName.substring( 0, beginMethodNameIndex );
		else
			result = methodName;

		return result;
	}

	static String extractShortClassNameFromSpec( String methodSpec )
	{
		String result = null;

		String methodName = extractMethodNameFromSpec( methodSpec );

		int beginMethodNameIndex = methodName.lastIndexOf( '.' );
		if ( beginMethodNameIndex != -1 )
			result = methodName.substring( 0, beginMethodNameIndex );
		else
			result = methodName;

		return result;
	}

	// spec may be full name of class e.g "com.wingdb.winjdbengine.Engine"
	static String extractClassNameFromFullName( String fullClassName )
	{
		String result = extractLastTokenFromSpec( fullClassName );
		return result;
	}

	// ---------------------------------------------------------------------

	// spec may be full name of class or method
	// "com.wingdb.winjdbengine.Engine" or "com.wingdb.winjdbengine.SourceManager.get"
	static String extractLastTokenFromSpec( String spec )
	{
		String result = extractLastTokenFromString( spec, '.' );
		return result;
	}

	// spec may be absolute or relative path
	// "/com/wingdb/winjdbengine/Engine" or "com/wingdb/winjdbengine/SourceManager/get"
	static String extractLastTokenFromPath( String path )
	{
		String result = extractLastTokenFromString( path, '/' );
		return result;
	}

	static String extractLastTokenFromString( String str, Character separator )
	{
		String result = null;

		int endClassPackageName = str.lastIndexOf( separator );
		if ( endClassPackageName != -1 )
		{
			int beginShortClassName = endClassPackageName + 1;
			result = str.substring( beginShortClassName );
		}
		else
		{
			result = str;
		}

		return result;
	}

	// ---------------------------------------------------------------------

	static boolean equalTypes( String className, ReferenceType refType )
	{
		String refTypeName = refType.name();
		boolean result = refTypeName.equals( className );
		return result;
	}

	static boolean canMatchRefType( String className, ReferenceType refType )
	{
		String refTypeName = refType.name();
		boolean result = refTypeName.endsWith( className );
		return result;
	}

	// ---------------------------------------------------------------------

	static boolean isIntNumber( String rawValue )
	{
		boolean result = true;
		for ( int i = 0
			; ( i < rawValue.length() ) && result
			; ++i
			)
		{
			char chr = rawValue.charAt( i );
			if ( ! Character.isDigit( chr ) )
				result = false;
		}
		return result;
	}

	static boolean isJavaIdentifier ( String id )
	{
		boolean result = false;
		if ( !id.isEmpty() )
		{
			int codePoint = id.codePointAt( 0 );
			if ( Character.isJavaIdentifierStart( codePoint ) )
			{
				result = true;
				for ( int i = Character.charCount( codePoint )
					; ( i < id.length() ) && result
					; i += Character.charCount( codePoint )
					)
				{
					codePoint = id.codePointAt( i );
					if ( !Character.isJavaIdentifierPart( codePoint ) )
						result = false;
				}
			}
		}
		return result;
	}

	private static boolean isValidMethodName ( String methodName )
	{
		if ( Utils.isJavaIdentifier( methodName )
			|| methodName.equals( "<init>" )
			|| methodName.equals( "<clinit>" ) )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	// ---------------------------------------------------------------------

	private static Hashtable< Character, String > d_escape2str = initEscapeMappings();

}
