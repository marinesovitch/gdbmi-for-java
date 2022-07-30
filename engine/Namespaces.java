// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import com.sun.jdi.ClassType;
import com.sun.jdi.Location;
import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ReferenceType;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.TreeMap;
import java.io.*;

// ---------------------------------------------------------------------

class Namespaces
{
	Namespaces()
	{
	}

	// ---------------------------------------------------------------------

	boolean addSourceDirectory( String sourceDirectory )
	{
		boolean result = ( d_sourceDirectories.indexOf( sourceDirectory ) == -1 );
		if ( result )
			d_sourceDirectories.add( sourceDirectory );
		return result;
	}

	String getSourcePath()
	{
		StringBuffer sst = new StringBuffer();
		boolean first = true;
		for ( String srcDir : d_sourceDirectories )
		{
			if ( first )
				first = false;
			else
				sst.append ( File.pathSeparator );
			sst.append ( srcDir );
		}
		String result = sst.toString();
		return result;
	}

	void clearSourceDirectories()
	{
		d_sourceDirectories.clear();
	}

	// ---------------------------------------------------------------------

	int size()
	{
		int result = d_namespaces.size();
		return result;
	}

	Namespace get( int index )
	{
		Namespace result = d_namespaces.get( index );
		return result;
	}

	// ---------------------------------------------------------------------

	void addNamespace( Namespace namespace )
	{
		Integer namespaceIndex = d_namespaces.size();
		d_namespaces.add( namespace );

		String packageName = namespace.getPackageName();
		d_package2namespace.put( packageName, namespaceIndex );

		String dirPath = namespace.getDirPath();
		d_path2namespace.put( dirPath, namespaceIndex );
	}

	void addClass( ReferenceType refType )
	{
		Pair< String, String > packageAndMainClassName
			= Utils.extractClassPackageAndName( refType, true );
		String packageName = packageAndMainClassName.first;

		Namespace namespace = getNamespaceByPackageName( packageName );
		if ( namespace != null )
		{
			String mainClassName = packageAndMainClassName.second;
			namespace.addClass( mainClassName, refType );
		}
	}

	// ---------------------------------------------------------------------

	List< Location > getAllLocationsForFile( String srcAbsolutePath )
	{
		List< Location > result = null;

		Pair< Namespace, String > namespaceAndSrcFileName
			= resolveNamespaceAndSrcFileName( srcAbsolutePath );
		if ( namespaceAndSrcFileName != null )
		{
			Namespace namespace = namespaceAndSrcFileName.first;
			String srcFileName = namespaceAndSrcFileName.second;
			result = namespace.getAllLocationsForFile( srcFileName );
		}

		return result;
	}

	List< Location > getLocations(
		String srcAbsolutePath, SourceInfo.EFitLineMode fitLineMode, int line )
	{
		List< Location > result = null;

		Pair< Namespace, String > namespaceAndSrcFileName
			= resolveNamespaceAndSrcFileName( srcAbsolutePath );
		if ( namespaceAndSrcFileName != null )
		{
			Namespace namespace = namespaceAndSrcFileName.first;
			String srcFileName = namespaceAndSrcFileName.second;
			result = namespace.getLocations( srcFileName, fitLineMode, line );
		}

		return result;
	}

	List< Location > tryGetNearestLocations(
		String srcAbsolutePath, int line, ClassType refType )
	{
		List< Location > result = null;

		Pair< Namespace, String > namespaceAndSrcFileName
			= resolveNamespaceAndSrcFileName( srcAbsolutePath );
		if ( namespaceAndSrcFileName != null )
		{
			Namespace namespace = namespaceAndSrcFileName.first;
			String srcFileName = namespaceAndSrcFileName.second;
			result = namespace.tryGetNearestLocations( srcFileName, line, refType );
		}

		return result;
	}

	// ---------------------------------------------------------------------

	String resolveAbsolutePath( Location location )
	{
		String result = null;

		if ( d_location2fullpath.containsKey( location ) )
			result = d_location2fullpath.get( location );
		else
		{
			Pair< Namespace, String > namespaceAndSrcFileName
				= resolveNamespaceAndSrcFileName( location );
			if ( namespaceAndSrcFileName != null )
			{
				Namespace namespace = namespaceAndSrcFileName.first;
				String srcFileName = namespaceAndSrcFileName.second;
				SourceInfo srcInfo = namespace.getSourceInfo( srcFileName );
				if ( srcInfo != null )
				{
					result = srcInfo.getAbsolutePath();

					// we resolved path for location, so by the way we can associate
					// class with source file too
					ReferenceType refType = location.declaringType();
					namespace.ensureAssociateClass( refType, srcFileName );
				}
			}

			// cache result despite it is null or not
			d_location2fullpath.put( location, result );
		}

		return result;
	}

	private Pair< Namespace, String > resolveNamespaceAndSrcFileName( Location location )
	{
		Pair< Namespace, String > result = null;

		try
		{
			String rawRelativeSourcePath = location.sourcePath();
			String relativeSourcePath = Utils.adjustPath( rawRelativeSourcePath );

			Pair< String, String > namespaceDirAndSrcFileName
				= Utils.extractDirAndFileName( relativeSourcePath );

			String packageRelativeDir = namespaceDirAndSrcFileName.first;
			String packageName = packageRelativeDir.replace( '/', '.' );

			Namespace namespace = getNamespaceByPackageName( packageName );
			if ( namespace != null )
			{
				String srcFileName = namespaceDirAndSrcFileName.second;
				result = new Pair< Namespace, String >( namespace, srcFileName );
			}
		}
		catch ( AbsentInformationException e )
		{
		}

		return result;
	}

	private Pair< Namespace, String > resolveNamespaceAndSrcFileName( String srcAbsolutePath )
	{
		Pair< Namespace, String > result = null;

		Pair< String, String > namespaceDirAndSrcFileName
			= Utils.extractDirAndFileName( srcAbsolutePath );

		String dirPath = namespaceDirAndSrcFileName.first;
		Namespace namespace = getNamespaceByDirPath( dirPath );
		if ( namespace != null )
		{
			String srcFileName = namespaceDirAndSrcFileName.second;
			result = new Pair< Namespace, String >( namespace, srcFileName );
		}

		return result;
	}

	// ---------------------------------------------------------------------

	private Namespace getNamespaceByPackageName( String packageName )
	{
		Namespace result = getNamespace( d_package2namespace, packageName );
		return result;
	}

	private Namespace getNamespaceByDirPath( String dirPath )
	{
		Namespace result = getNamespace( d_path2namespace, dirPath );
		return result;
	}

	private Namespace getNamespace( Map< String, Integer > key2namespace, String key )
	{
		Namespace result = null;

		Integer namespaceIndex = key2namespace.get( key );
		if ( namespaceIndex != null )
			result = d_namespaces.get( namespaceIndex );

		return result;
	}

	// ---------------------------------------------------------------------

	private final List< String > d_sourceDirectories
		= new ArrayList< String >();

	private final List< Namespace > d_namespaces
		= new ArrayList< Namespace >();

	// maps package name to namespace index, e.g.
	// com.wingdb.winjdbengine -> 0
	// where d_namespaces[ 0 ] == Namespace( com.wingdb.winjdbengine )
	private Map< String, Integer > d_package2namespace
		= new TreeMap< String, Integer >( String.CASE_INSENSITIVE_ORDER );

	// maps full dir path to namespace index, e.g.
	// c:/sources/winjdb/com/wingdb/winjdbengine -> 0
	// where d_namespaces[ 0 ] == Namespace( com.wingdb.winjdbengine )
	private Map< String, Integer > d_path2namespace
		= new TreeMap< String, Integer >( String.CASE_INSENSITIVE_ORDER );

	// cache - Location to its absolute path
	private Map< Location, String > d_location2fullpath
		= new TreeMap< Location, String >();

} // Namespaces
