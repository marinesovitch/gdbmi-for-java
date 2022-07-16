// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.lang.String;
import java.lang.StringBuffer;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import com.sun.jdi.*;

// ---------------------------------------------------------------------

public class MethodManager
{
    // ---------------------------------------------------------------------
    /*
        All information about methods that are retrieved from the Method class
        and dumped to output in whatever form, they all must pass through
        by this class (e.g. getting a name or anything else).

        Mangling rules:
        1. A name that is mapped out consists of a part N made up of the qualified part
           the name of the method and the T part created from the signature of the method (Method.signature()).
        2. The part N starts with the sequence of characters "!N" and the part T
           from "!T".
        3. Dots are replaced with the string "!O".
        4. Opening parentheses for "!C".
        5. Closing parentheses for "!J".
        6. Opening square brackets for "!E".
        7. Slashes for "!I".
        8. Semicolons for "!And".
        9. The whole name starts with the letter M.

        For example.
        A method: long com.pkg.MyClass.f ( int i, Class c );
        Part N: com.pkg.MyClass.f
        Part T: (ILjava/lang/Class;)J

        After mangling: M!Ncom!opkg!oMyClass!of!T!CILjava!Ilang!IClass!i!JJ.

        Terminology used in this class:
        method - an object of the Method class
        manglid - a mysterious name that uniquely identifies a method (mangled id)
        index - a numerical index of the method, also uniquely identifying it
        name - a method name (without signature)
        address - an address in the virtual address space, it can be set for a method
                  (from the beginning) or anywhere in the method code
        offset - a relative code address within some method
    */
    // ---------------------------------------------------------------------

    public final String method2manglid ( Method hMethod )
    {
        ReferenceType refType = hMethod.declaringType();
        ensureClassRegistered( refType );

        final String manglid = prepareManglid( hMethod );
        return manglid;
    }

    // ---------------------------------------------------------------------

    public final Method manglid2method ( String manglid )
    {
        final Integer index = d_manglid2index.get ( manglid );

        if ( index != null )
            return d_index2method.get ( index );
        else
            return null;
    }

    // ---------------------------------------------------------------------

    public final int method2index ( Method hMethod )
    {
        final String manglid = method2manglid ( hMethod );
        return d_manglid2index.get ( manglid );
    }

    // ---------------------------------------------------------------------

    public final Method index2method ( int index )
    {
        if ( ! d_index2method.containsKey ( index ) )
            return null;

        return d_index2method.get ( index );
    }

    // ---------------------------------------------------------------------

    public final int manglid2index ( String manglid )
    {
        return d_manglid2index.get ( manglid );
    }

    // ---------------------------------------------------------------------

    public final String index2manglid ( int index )
    {
        final Method hMethod = d_index2method.get ( index );

        if ( hMethod != null )
            return method2manglid ( hMethod );
        else
            return null;
    }

    // ---------------------------------------------------------------------

    /*
        methodSpec is method full name, e.g.
        wingdbJavaDebugEngine.MethodManager.indexoffset2address

        user may add arguments in parenthesis, but they will be ignored
    */
    public final List< Integer > getMethodsByFullName( String methodSpec )
    {
        List< Integer > result = null;

        String className = Utils.extractClassNameFromSpec( methodSpec );

        VirtualMachine vm = Instance.s_connection.getVirtualMachine();
        List< ReferenceType > classes = vm.classesByName( className );

        if ( classes != null )
        {
            for ( ReferenceType refType : classes )
                ensureClassRegistered( refType );

            String unmangledMethodName = Utils.extractMethodNameFromSpec( methodSpec );
            result = d_fullname2indexes.get( unmangledMethodName );
        }

        return result;
    }

    /*
        methodSpec is method name, but it doesn't have to contain the full-spec
        it may be e.g. :
        0) com.wingdb.javaDebugEngine.MethodManager.indexoffset2address
        1) wingdb.javaDebugEngine.MethodManager.indexoffset2address
        2) javaDebugEngine.MethodManager.indexoffset2address
        3) MethodManager.indexoffset2address

        and all they will be matched - it is comfortable for user, who doesn't
        have to introduce the full name

        user may add arguments in parenthesis, but they will be ignored
    */
    public final List< Integer > getMethodsByName( String methodSpec )
    {
        List< Integer > result = getMethodsByFullName( methodSpec );

        if ( result == null )
        {
            String shortMethodName = Utils.extractShortMethodNameFromSpec( methodSpec );
            List< Integer > matchedIndexes = d_shortname2indexes.get( shortMethodName );

            if ( matchedIndexes != null )
            {
                for ( Integer matchedIndex : matchedIndexes )
                {
                    if ( canMatchMethod( matchedIndex, methodSpec ) )
                    {
                        if ( result == null )
                            result = new ArrayList< Integer >();
                        result.add( matchedIndex );
                    }
                }
            }
        }

        return result;
    }

    private boolean canMatchMethod( Integer index, String methodSpec )
    {
        Method method = d_index2method.get( index );
        String fullMethodName = prepareUnmangledName( method );
        boolean result = fullMethodName.endsWith( methodSpec );
        return result;
    }

    // ---------------------------------------------------------------------

    public Method matchMethod(
        ReferenceType refType,
        String methodName,
        List< Value > args )
            throws ClassNotLoadedException
    {
        Method result = null;

        final List< Method > methods = refType.methodsByName ( methodName );
        if ( Container.safeIsNotEmpty( methods ) )
        {
            int methodsCount = methods.size();
            if ( 1 < methodsCount )
            {
                int argsCount = args != null ? args.size() : 0;
                for ( Method method : methods )
                {
                    int methodArgsCount = method.argumentTypes().size();
                    if ( methodArgsCount == argsCount )
                    {
                        result = method;
                        break;
                    }
                }
            }

            if ( result == null )
                result = methods.get( 0 );
        }

        return result;
    }

    // ---------------------------------------------------------------------

    public final long address2offset ( long address )
    {
        return address & 0xFFFFFFFFL;
    }

    // ---------------------------------------------------------------------

    public final int address2index ( long address )
    {
        return ( int )( address >> 32 );
    }

    // ---------------------------------------------------------------------

    public final long indexoffset2address ( int index, long offset )
    {
        return ( ( ( long ) index ) << 32 ) | offset;
    }

    // ---------------------------------------------------------------------

    public final String address2id ( long address )
    {
        final int index = address2index ( address );
        return index2manglid ( index );
    }

    // ---------------------------------------------------------------------

    public final long manglidoffset2address ( String manglid, long offset )
    {
        final int index = manglid2index ( manglid );
        return indexoffset2address ( index, offset );
    }

    // ---------------------------------------------------------------------

    public final Method address2method ( long address )
    {
        final int index = address2index ( address );
        return index2method ( index );
    }

    // ---------------------------------------------------------------------

    public final long methodoffset2address ( Method hMethod, long offset )
    {
        final int index = method2index ( hMethod );
        return indexoffset2address ( index, offset );
    }

    // ---------------------------------------------------------------------

    //CAUTION!
    //never use Location.codeIndex directly, instead use that method to
    //calculate offset
    static public long getOffset( Location location )
    {
        long result = 0;
        final long codeIndex = location.codeIndex();
        if ( 0 < codeIndex )
            result = codeIndex;
        // if codeIndex == -1 means that location is within a native method
        // then return offset == 0
        return result;
    }

    // ---------------------------------------------------------------------

    public final String demangle ( String manglid )
    {
        Method hMethod = manglid2method ( manglid );

        if ( hMethod != null )
        {
            StringBuffer sst = new StringBuffer();

            Location location = hMethod.location();
            ReferenceType refType = location.declaringType();
            String className = refType.name();
            sst.append ( className );
            sst.append ( '.' );
            sst.append ( hMethod.name() );
            sst.append ( " ( " );

            final List< String > argTypeNames = hMethod.argumentTypeNames();

            for ( Iterator< String > iArgTypeName = argTypeNames.iterator(); iArgTypeName.hasNext(); )
            {
                final String argTypeName = iArgTypeName.next();
                sst.append ( argTypeName );

                if ( iArgTypeName.hasNext() )
                    sst.append ( ", " );
            }

            sst.append ( " )" );

            return sst.toString();
        }
        else
            return null;
    }

    // ---------------------------------------------------------------------

    private final synchronized void ensureClassRegistered( ReferenceType classType )
    {
        if ( ! d_registeredClasses.contains( classType ) )
        {
            registerClassMethods( classType );
            d_registeredClasses.add( classType );
        }
    }

    private final void registerClassMethods ( ReferenceType hClass )
    {
        final List< Method > methods = hClass.allMethods();

        if ( methods != null )
        {
            for ( Iterator< Method > iMethod = methods.iterator(); iMethod.hasNext(); )
            {
                final Method method = iMethod.next();
                registerMethod( method );
            }
        }
    }

    private void registerMethod( Method method )
    {
        final String manglid = prepareManglid( method );
        final Integer index = d_manglid2index.get ( manglid );
        if ( index == null )
        {
            final int methodIndex  = d_manglid2index.size();
            d_manglid2index.put ( manglid, methodIndex  );
            d_index2method.put ( methodIndex , method );

            storeMethodFullName( methodIndex, method );
            storeMethodShortName( methodIndex, method );
        }
    }

    private void storeMethodFullName( int methodIndex, Method method )
    {
        // store full method name (namespace.ClassName.MethodName) index
        final String unmangledFullName = prepareUnmangledName( method );
        if ( !d_fullname2indexes.containsKey( unmangledFullName ) )
        {
        	List< Integer > indexes = new ArrayList< Integer >();
			d_fullname2indexes.put( unmangledFullName, indexes );
        }
        List< Integer > indexes = d_fullname2indexes.get( unmangledFullName );
        indexes.add( methodIndex );
    }

    private void storeMethodShortName( int methodIndex, Method method )
    {
        // store short method name (ClassName.MethodName) index
        final String unmangledFullName = prepareUnmangledName( method );
        final String unmangledShortName = Utils.extractShortMethodNameFromSpec( unmangledFullName );
        if ( !d_shortname2indexes.containsKey( unmangledShortName ) )
        {
        	List< Integer > indexes = new ArrayList< Integer >();
			d_shortname2indexes.put( unmangledShortName, indexes );
        }
        List< Integer > indexes = d_shortname2indexes.get( unmangledShortName );
        indexes.add( methodIndex );
    }

    // ---------------------------------------------------------------------

    private final String prepareManglid( Method hMethod )
    {
        final String name = hMethod.name();
        final ReferenceType hParent = hMethod.declaringType();
        final String typeSignature = hParent.signature();
        final String qualifiedName = typeSignature + "." + name;

        final String signature = hMethod.signature();
        final String combinedName = combine ( qualifiedName, signature );
        final String manglid = encode ( combinedName );

        return manglid;
    }

    private final String prepareUnmangledName( Method method )
    {
        final ReferenceType parentClass = method.declaringType();
        final String className = parentClass.name();
        final String methodName = method.name();
        final String result = className + "." + methodName;
        return result;
    }

    // ---------------------------------------------------------------------

    private String combine ( String methodName, String methodSignature )
    {
        return "M!N" + methodName + "!T" + methodSignature;
    }

    // ---------------------------------------------------------------------

    private String extractName ( String combinedName )
    {
        final int namePartStart = combinedName.indexOf ( "!N" );
        final int typePartStart = combinedName.indexOf ( "!T" );

        if ( namePartStart == -1 )
            return "";

        if ( typePartStart != -1 )
            return combinedName.substring ( namePartStart, typePartStart );
        else
            return combinedName.substring ( namePartStart );
    }

    // ---------------------------------------------------------------------

    private String extractSignature ( String combinedName )
    {
        final int typePartStart = combinedName.indexOf ( "!T" );

        if ( typePartStart != -1 )
            return combinedName.substring ( typePartStart );
        else
            return "";
    }

    // ---------------------------------------------------------------------

    private String encode ( String combinedName )
    {
        String result = combinedName;
        result = s_rxUnmangledDot.matcher ( result ).replaceAll ( "!o" );
        result = s_rxUnmangledOpeningParen.matcher ( result ).replaceAll ( "!C" );
        result = s_rxUnmangledClosingParen.matcher ( result ).replaceAll ( "!J" );
        result = s_rxUnmangledBracket.matcher ( result ).replaceAll ( "!E" );
        result = s_rxUnmangledSlash.matcher ( result ).replaceAll ( "!I" );
        result = s_rxUnmangledSemicolon.matcher ( result ).replaceAll ( "!i" );
        result = s_rxUnmangledOpeningAngle.matcher ( result ).replaceAll ( "!K" );
        result = s_rxUnmangledClosingAngle.matcher ( result ).replaceAll ( "!X" );

        return result;
    }

    // ---------------------------------------------------------------------

    private String decode ( String manglid )
    {
        String result = manglid;
        result = s_rxMangledDot.matcher ( result ).replaceAll ( "." );
        result = s_rxMangledOpeningParen.matcher ( result ).replaceAll ( "(" );
        result = s_rxMangledClosingParen.matcher ( result ).replaceAll ( ")" );
        result = s_rxMangledBracket.matcher ( result ).replaceAll ( "[" );
        result = s_rxMangledSlash.matcher ( result ).replaceAll ( "/" );
        result = s_rxMangledSemicolon.matcher ( result ).replaceAll ( ";" );
        result = s_rxMangledOpeningAngle.matcher ( result ).replaceAll ( "<" );
        result = s_rxMangledClosingAngle.matcher ( result ).replaceAll ( ">" );

        return result;
    }

    // ---------------------------------------------------------------------

    private String manglid2name ( String manglid )
    {
        final String combinedName = decode ( manglid );
        return extractName ( combinedName );
    }

    // ---------------------------------------------------------------------

    private String manglid2signature ( String manglid )
    {
        final String combinedName = decode ( manglid );
        return extractSignature ( combinedName );
    }

    // ---------------------------------------------------------------------

    private String getMainMethodSignature ( String qualifiedClassName )
    {
        final String classSignature = qualifiedClassName.replaceAll ( "[.]", "/" );
        return "M!NL" + classSignature + "!i!omain!T!C!ELjava!Ilang!IString!i!JV";
    }

    // ---------------------------------------------------------------------

    private Set< ReferenceType > d_registeredClasses = new TreeSet< ReferenceType >();
    private Hashtable< String, Integer > d_manglid2index = new Hashtable< String, Integer >();
    private Hashtable< Integer, Method > d_index2method = new Hashtable< Integer, Method >();

    //maps unmangled method full-name without arguments (namespace.ClassName.MethodName) to index(es)
    private Hashtable< String, List< Integer > > d_fullname2indexes = new Hashtable< String, List< Integer > >();

    //maps unmangled method short-name without arguments (ClassName.MethodName) to index(es)
    private Hashtable< String, List< Integer > > d_shortname2indexes = new Hashtable< String, List< Integer > >();

    private static Pattern s_rxMangledDot = Pattern.compile ( "!o" );
    private static Pattern s_rxMangledOpeningParen = Pattern.compile ( "!C" );
    private static Pattern s_rxMangledClosingParen = Pattern.compile ( "!J" );
    private static Pattern s_rxMangledBracket = Pattern.compile ( "!E" );
    private static Pattern s_rxMangledSlash = Pattern.compile ( "!I" );
    private static Pattern s_rxMangledSemicolon = Pattern.compile ( "!i" );
    private static Pattern s_rxMangledOpeningAngle = Pattern.compile ( "!K" );
    private static Pattern s_rxMangledClosingAngle = Pattern.compile ( "!X" );

    private static Pattern s_rxUnmangledDot = Pattern.compile ( "[.]" );
    private static Pattern s_rxUnmangledOpeningParen = Pattern.compile ( "[(]" );
    private static Pattern s_rxUnmangledClosingParen = Pattern.compile ( "[)]" );
    private static Pattern s_rxUnmangledBracket = Pattern.compile ( "\\[" );
    private static Pattern s_rxUnmangledSlash = Pattern.compile ( "[/]" );
    private static Pattern s_rxUnmangledSemicolon = Pattern.compile ( "[;]" );
    private static Pattern s_rxUnmangledOpeningAngle = Pattern.compile ( "[<]" );
    private static Pattern s_rxUnmangledClosingAngle = Pattern.compile ( "[>]" );

    // ---------------------------------------------------------------------
}
