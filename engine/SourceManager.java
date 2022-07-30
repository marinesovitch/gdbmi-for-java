// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.ClassType;
import com.sun.jdi.Location;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ReferenceType;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.io.*;

// ---------------------------------------------------------------------

class NamespaceLoader
{
	NamespaceLoader( Namespaces namespaces )
	{
		d_namespaces = namespaces;
	}

	public synchronized void run( List< String > sourceDirs )
	{
		addSourceDirectories ( sourceDirs );
	}

	// ---------------------------------------------------------------------

	private void addSourceDirectories ( List< String > sourceDirs )
	{
		for ( String srcDir : sourceDirs )
		{
			if ( ! ( srcDir.endsWith ( ".jar" ) || srcDir.endsWith ( ".zip" ) ) )
				addSourceDirectory( srcDir );
		}
	}

	private void addSourceDirectory( String dirPath )
	{
		File dir = new File ( dirPath );
		if ( dir.isDirectory() )
		{
			String rawDirPath = dir.getAbsolutePath();
			String srcDirPath = Utils.adjustPath( rawDirPath );
			if ( d_namespaces.addSourceDirectory( srcDirPath ) )
			{
				String emptyPackageName = "";
				addSourceDirectory( dir, emptyPackageName );
			}
		}
	}

	private void addSourceDirectory( File dir, String packageName )
	{
		File[] files = dir.listFiles();
		if ( files != null )
		{
			String rawDirPath = dir.getAbsolutePath();
			String srcDirPath = Utils.adjustPath( rawDirPath );
			Namespace namespace = new Namespace( packageName, srcDirPath );
			if ( addNamespaceItems( files, namespace ) )
				d_namespaces.addNamespace( namespace );
		}
	}

	private boolean addNamespaceItems( File[] files, Namespace namespace )
	{
		boolean sourceAdded = false;

		for ( File file : files )
		{
			if ( file.isFile() )
			{
				if ( addSourceFile( file, namespace ) )
					sourceAdded = true;
			}
			else if ( file.isDirectory() )
			{
				String childPackageName = prepareChildPackageName( namespace, file );
				addSourceDirectory( file, childPackageName );
			}
		}

		return sourceAdded;
	}

	private boolean addSourceFile( File file, Namespace namespace )
	{
		boolean result = false;
		String fileName = file.getName();
		if ( ( fileName != null ) && fileName.endsWith( SourceInfo.JavaSrcExtension ) )
		{
			namespace.addSourceFile( fileName );
			result = true;
		}
		return result;
	}

	private String prepareChildPackageName( Namespace namespace, File dir )
	{
		String parentPackageName = namespace.getPackageName();
		String result = parentPackageName;
		if ( !parentPackageName.isEmpty() )
			result += '.';
		String dirName = dir.getName();
		result += dirName;
		return result;
	}

	// ---------------------------------------------------------------------

	final private Namespaces d_namespaces;

} // NamespaceLoader

// ---------------------------------------------------------------------
// ---------------------------------------------------------------------

class SourceManager
{

	public SourceManager()
	{
	}

	// ---------------------------------------------------------------------

	public synchronized void addSourceDirectories ( String rawSourceDirs )
	{
		StringTokenizer tokenizer = new StringTokenizer ( rawSourceDirs, File.pathSeparator );
		List< String > sourceDirs = new ArrayList< String >();
		while ( tokenizer.hasMoreTokens() )
		{
			final String srcDir = tokenizer.nextToken();
			sourceDirs.add( srcDir );
		}
		addSourceDirectories ( sourceDirs );
	}

	public synchronized void addSourceDirectories ( List< String > sourceDirs )
	{
		NamespaceLoader namespaceLoader = new NamespaceLoader( d_namespaces );
		namespaceLoader.run( sourceDirs );
	}

	public synchronized void clearSourceDirectories()
	{
		d_namespaces.clearSourceDirectories();
	}

	synchronized String getSourcePath()
	{
		String result = d_namespaces.getSourcePath();
		return result;
	}

	// ---------------------------------------------------------------------

	public synchronized SourceTraverser getAllSources()
	{
		SourceTraverser sourceTraverser = new SourceTraverser( d_namespaces );
		return sourceTraverser;
	}

	// ---------------------------------------------------------------------

	public synchronized void addClass ( ReferenceType hClass )
	{
		try
		{
			d_namespaces.addClass( hClass );
			storeClassName( hClass );
		}
		catch ( Exception e )
		{
		}
	}

	private void storeClassName( ReferenceType refType )
	{
		String className = refType.name();
		d_name2class.put( className, refType );

		String shortClassName = Utils.extractClassNameFromFullName( className );
		if ( !shortClassName.equals( className ) )
		{
			if ( !d_shortName2classes.containsKey( shortClassName ) )
				d_shortName2classes.put( shortClassName, new ArrayList< ReferenceType >() );
			List< ReferenceType > classes = d_shortName2classes.get( shortClassName );
			classes.add( refType );
		}
	}

	// ---------------------------------------------------------------------

	List< ReferenceType > getClassByName( String className )
	{
		List< ReferenceType > result = null;
		if ( d_name2class.containsKey( className ) )
		{
			result = new ArrayList< ReferenceType >();
			ReferenceType refType = d_name2class.get( className );
			result.add( refType );
		}
		else
		{
			result = getClassByShortName( className );
		}
		return result;
	}

	private List< ReferenceType > getClassByShortName( String className )
	{
		List< ReferenceType > result = null;
		String shortClassName = Utils.extractClassNameFromFullName( className );
		if ( d_shortName2classes.containsKey( shortClassName ) )
		{
			String suffixClassName = '.' + className;
			List< ReferenceType > refTypes = d_shortName2classes.get( shortClassName );
			for ( ReferenceType refType : refTypes )
			{
				if ( Utils.canMatchRefType( suffixClassName, refType ) )
				{
					if ( result == null )
						result = new ArrayList< ReferenceType >();
					result.add( refType );
				}
			}
		}
		return result;
	}

	// ---------------------------------------------------------------------

	public synchronized List< Location > getAllLocationsForFile (
		String rawAbsolutePath )
	{
		String srcAbsolutePath = Utils.adjustPath( rawAbsolutePath );
		List< Location > result = d_namespaces.getAllLocationsForFile( srcAbsolutePath );
		return result;
	}

	public synchronized List< Location > getExactLocations(
		String rawAbsolutePath, int line )
	{
		String srcAbsolutePath = Utils.adjustPath( rawAbsolutePath );
		List< Location > result
			= d_namespaces.getLocations( srcAbsolutePath, SourceInfo.EFitLineMode.Exactly, line );
		return result;
	}

	public synchronized List< Location > getNearestLocations(
		String rawAbsolutePath, int line )
	{
		String srcAbsolutePath = Utils.adjustPath( rawAbsolutePath );
		List< Location > result
			= d_namespaces.getLocations( srcAbsolutePath, SourceInfo.EFitLineMode.Nearest, line );
		return result;
	}

	public synchronized List< Location > tryGetNearestLocations(
		String rawAbsolutePath, int line, ClassType refType )
	{
		String srcAbsolutePath = Utils.adjustPath( rawAbsolutePath );
		List< Location > result
			= d_namespaces.tryGetNearestLocations( srcAbsolutePath, line, refType );
		return result;
	}

	// ---------------------------------------------------------------------

	public String getAbsolutePath ( Location hLocation )
	{
		final String srcAbsolutePath = d_namespaces.resolveAbsolutePath( hLocation );
		return srcAbsolutePath;
	}

	// ---------------------------------------------------------------------

	private Namespaces d_namespaces = new Namespaces();

	private Map< String, ReferenceType > d_name2class =
		new TreeMap< String, ReferenceType >();

	private Map< String, List< ReferenceType > > d_shortName2classes =
		new TreeMap< String, List< ReferenceType > >();

} // SourceManager
