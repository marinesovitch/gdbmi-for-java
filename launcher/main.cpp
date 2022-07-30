// -----------------------------------------------------------------------------

#include "ph.h"

#include "utils/cpp/cppFileSystemUtils.h"
#include "utils/cpp/cppSysUtils.h"
#include "utils/cpp/cppStringUtils.h"

#include "utils/sysutils/suDebugUtils.h"

// -----------------------------------------------------------------------------

const int DefaultConnectionTimeOut = 5000;

// -----------------------------------------------------------------------------

struct XArgumentError : public std::runtime_error
{
	XArgumentError ( const std::string& argName );
};

// -----------------------------------------------------------------------------

XArgumentError :: XArgumentError ( const std::string& argName ) :
	std::runtime_error ( "Bad argument: " + argName )
{
}

// -----------------------------------------------------------------------------
// -----------------------------------------------------------------------------

class KLauncher
{
	public:
		KLauncher();

	public:
		void run( int argc, char* argv[] );

	private:
		int getLaunchArgs( int argc, char** argv );

		std::string gatherGenericArgs( int argc, char** argv );

		std::string prepareExecutablePath() const;

		std::string prepareCommandLine ( const std::string& genericArgs ) const;

		std::string prepareDebuggerWorkingDirectory() const;

		void launchJavaDebugger(
			const std::string& executablePath,
			const std::string& commandLine,
			const std::string& workingDirectory );

	private:
		int d_startingGenericArg;
		std::string d_jdkPath;
		std::string d_mainClass;
		bool d_debugMode;
		int d_timeOut;

};

// -----------------------------------------------------------------------------

KLauncher::KLauncher()
	: d_startingGenericArg( -1 )
	, d_debugMode( false )
	, d_timeOut( DefaultConnectionTimeOut )
{
}

// -----------------------------------------------------------------------------

void KLauncher::run( int argc, char* argv[] )
{
	d_startingGenericArg = getLaunchArgs( argc, argv );

	const std::string& genericArgs = gatherGenericArgs( argc, argv );

	const std::string& executablePath = prepareExecutablePath();

	const std::string& commandLine = prepareCommandLine( genericArgs );

	const std::string& workingDirectory = prepareDebuggerWorkingDirectory();

	launchJavaDebugger( executablePath, commandLine, workingDirectory );
}

// -----------------------------------------------------------------------------

int KLauncher::getLaunchArgs ( int argc, char** argv )
{
	for ( int i = 1; i < argc; ++i )
	{
		const std::string strArg = argv [ i ];

		if ( strArg == "--jdk" )
		{
			if ( i + 1 == argc )
				throw XArgumentError ( strArg );

			d_jdkPath = argv [ i + 1 ];
			++i;
		}
		else if ( strArg == "--class" )
		{
			d_mainClass = argv [ i + 1 ];
			++i;
		}
		else if ( strArg == "--interpreter=mi" )
		{
			// ignore
		}
		else if ( strArg == "--debug" )
		{
			d_debugMode = true;
		}
		else if ( strArg == "--timeout" )
		{
			const std::string timeOutStr = argv [ i + 1 ];
			++i;
			d_timeOut = cpp::su::str2int( timeOutStr );
		}
		else if ( strArg == "--" )
		{
			return i + 1;
		}
		else
			throw XArgumentError ( strArg );
	}

	throw XArgumentError ( "No Java arguments on the command line." );
}

// -----------------------------------------------------------------------------

std::string KLauncher::gatherGenericArgs ( int argc, char** argv )
{
	std::stringstream sst;

	for ( int i = d_startingGenericArg; i < argc; ++i )
	{
		if ( i != d_startingGenericArg )
			sst << ' ';
		sst << argv [ i ];
	}

	return sst.str();
}

// -----------------------------------------------------------------------------

std::string KLauncher::prepareExecutablePath () const
{
	std::stringstream sst;
	sst << d_jdkPath << "\\jre\\bin\\java.exe";
	return sst.str();
}

// -----------------------------------------------------------------------------

std::string KLauncher::prepareCommandLine ( const std::string& genericArgs ) const
{
	std::stringstream sst;

	sst << "java.exe ";

	#ifdef PE_DEBUG
		if ( d_debugMode )
			sst << "-Xdebug -Xrunjdwp:transport=dt_socket,address=localhost:8000,server=y,suspend=n ";
	#endif

	sst << "-classpath .;";
	sst << "./wingdbJavaDebugger/wingdbJavaDebugEngine.jar;";
	sst << "./wingdbJavaDebugger/jython.jar;";
	sst << "\"" << d_jdkPath << "\\lib\\tools.jar\" ";

	sst << "wingdbJavaDebugger.KDebugger ";
	sst << " -mainclass " << d_mainClass;
	if ( d_timeOut != 0 )
		sst << " -timeout " << d_timeOut;
	sst << " " << genericArgs;

	return sst.str();
}

// -----------------------------------------------------------------------------

std::string KLauncher::prepareDebuggerWorkingDirectory() const
{
	using namespace boost::filesystem;
	using namespace cpp::fs;

	const path programPath = getCurrentProgramPath();
	const path binPath = programPath.branch_path();

	return binPath.native_directory_string();
}

// -----------------------------------------------------------------------------

void KLauncher::launchJavaDebugger (
	const std::string& executablePath,
	const std::string& commandLine,
	const std::string& workingDirectory )
{
	std::cout << executablePath << ' ' << commandLine << " (" << workingDirectory << ")" << std::endl;

	cpp::sys::child_process process ( executablePath );
	process.setWorkingDirectory ( workingDirectory );
	process.inheritStdIO();
	process.create ( commandLine );
	process.run();
	process.wait();
}

// -----------------------------------------------------------------------------

int main ( int argc, char* argv[] )
{
	try
	{
		KLauncher launcher;
		launcher.run( argc, argv );
	}
	catch ( const XArgumentError& error )
	{
		std::cout << error.what() << std::endl;
		return -1;
	}

	return 0;
}

// -----------------------------------------------------------------------------
