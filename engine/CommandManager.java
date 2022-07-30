// ---------------------------------------------------------------------
package wingdbJavaDebugEngine;
// ---------------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

// ---------------------------------------------------------------------

public class CommandManager
{
	// -----------------------------------------------------------------

	public CommandManager()
	{
		registerCommands();
	}

	// -----------------------------------------------------------------

	public void registerCommands()
	{
		registerConfigCommands();
		registerInfoCommands();
		registerExecCommands();
		registerStackCommands();
		registerBreakpointCommands();
		registerVarCommands();
	}

	// -----------------------------------------------------------------

	private void registerConfigCommands()
	{
		registerCommand ( "-list-features", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandListFeatures ( args ); }
		} );

		registerCommand ( "-gdb-set", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandGdbSet ( args ); }
		} );

		registerCommand ( "-gdb-show", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandGdbShow ( args ); }
		} );

		registerCommand ( "-environment-directory", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandEnvironmentDirectory ( args ); }
		} );

		registerCommand ( "-internal-command", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandInternalCommand ( args ); }
		} );

		registerCommand ( "-enable-pretty-printing", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandEnablePrettyPrinting ( args ); }
		} );

		registerCommand ( "-add-visualizers-directory", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandAddVisualizersDirectory ( args ); }
		} );

		registerCommand ( "-load-visualizer", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdConfig.commandLoadVisualizer ( args ); }
		} );
	}

	// -----------------------------------------------------------------

	private void registerInfoCommands()
	{
		registerCommand ( "-info-target", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandInfoTarget ( args ); }
		} );

		registerCommand ( "-info-sharedlibrary", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandInfoSharedLibrary ( args ); }
		} );

		registerCommand ( "-info-threads", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandInfoThreads ( args ); }
		} );

		registerCommand ( "-info-symbol", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandInfoSymbol ( args ); }
		} );

		registerCommand ( "-info-frame", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandInfoFrame ( args ); }
		} );

		registerCommand ( "-info-line", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandInfoLine ( args ); }
		} );

		registerCommand ( "-maint-demangle", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandMaintDemangle ( args ); }
		} );

		registerCommand ( "whatis", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandWhatis ( args ); }
		} );

		registerCommand ( "-file-list-exec-source-files", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandFileListExecSourceFiles ( args ); }
		} );

		registerCommand ( "-symbol-list-lines", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandSymbolListLines ( args ); }
		} );

		registerCommand ( "-data-evaluate-expression", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdInfo.commandDataEvaluateExpression ( args ); }
		} );
	}

	// -----------------------------------------------------------------

	private void registerExecCommands()
	{
		registerCommand ( "-exec-arguments", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdExec.commandExecArguments ( args ); }
		} );

		registerCommand ( "-exec-run", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args ) throws Exception
			{ CmdExec.commandExecRun ( args ); }
		}, CommandInfo.Asynchronous );

		registerCommand ( "-exec-continue", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdExec.commandExecContinue ( args ); }
		}, CommandInfo.Asynchronous );

		registerCommand ( Consts.InterruptCmdName, new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdExec.commandExecInterrupt ( args ); }
		}, CommandInfo.DontPrintPrompt );

		registerCommand ( "-exec-finish", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args ) throws Exception
			{ CmdExec.commandExecFinish ( args ); }
		}, CommandInfo.Asynchronous );

		registerCommand ( "-exec-step", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args ) throws Exception
			{ CmdExec.commandExecStep ( args ); }
		}, CommandInfo.Asynchronous );

		registerCommand ( "-exec-step-instruction", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args ) throws Exception
			{ CmdExec.commandExecStepInstruction ( args ); }
		}, CommandInfo.Asynchronous );

		registerCommand ( "-exec-next", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args ) throws Exception
			{ CmdExec.commandExecNext ( args ); }
		}, CommandInfo.Asynchronous );

		registerCommand ( "-exec-next-instruction", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args ) throws Exception
			{ CmdExec.commandExecNextInstruction ( args ); }
		}, CommandInfo.Asynchronous );

		registerCommand ( "-thread-select", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdExec.commandThreadSelect ( args ); }
		} );

		registerCommand ( "target", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdExec.commandTarget ( args ); }
		} );

		registerCommand ( "-gdb-exit", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdExec.commandGdbExit ( args ); }
		}, CommandInfo.DontPrintPrompt );
	}

	// -----------------------------------------------------------------

	private void registerStackCommands()
	{
		registerCommand ( "-stack-list-frames", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdStack.commandStackListFrames ( args ); }
		} );

		registerCommand ( "-stack-list-locals", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdStack.commandStackListLocals ( args ); }
		} );

		registerCommand ( "-stack-list-arguments", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdStack.commandStackListArguments ( args ); }
		} );

		registerCommand ( "-stack-select-frame", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdStack.commandStackSelectFrame ( args ); }
		} );
	}

	// -----------------------------------------------------------------

	private void registerBreakpointCommands()
	{
		registerCommand ( "-break-insert", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandBreakInsert ( args ); }
		} );

		registerCommand ( "-break-watch", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandWatchInsert ( args ); }
		} );

		registerCommand ( "catch", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandCatch ( args ); }
		} );

		registerCommand ( "-break-delete", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandBreakDelete ( args ); }
		} );

		registerCommand ( "-break-enable", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandBreakEnable ( args ); }
		} );

		registerCommand ( "-break-disable", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandBreakDisable ( args ); }
		} );

		registerCommand ( "-break-condition", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandBreakSetCondition( args ); }
		} );

		registerCommand ( "-break-after", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandBreakSetPassCount( args ); }
		} );

		registerCommand ( "-info-break", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandInfoBreak ( args ); }
		} );

		registerCommand ( "-maint-info-break", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdBreakpoints.commandMaintInfoBreak ( args ); }
		} );
	}

	// -----------------------------------------------------------------

	private void registerVarCommands()
	{
		registerCommand ( "-var-create", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarCreate ( args ); }
		} );

		registerCommand ( "-var-delete", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarDelete ( args ); }
		} );

		registerCommand ( "-var-set-format", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarSetFormat ( args ); }
		} );

		registerCommand ( "-var-info-expression", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarInfoExpression ( args ); }
		} );

		registerCommand ( "-var-info-num-children", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarInfoNumChildren ( args ); }
		} );

		registerCommand ( "-var-list-children", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarListChildren ( args ); }
		} );

		registerCommand ( "-var-info-type", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarInfoType ( args ); }
		} );

		registerCommand ( "-var-evaluate-expression", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarEvaluateExpression ( args ); }
		} );

		registerCommand ( "-var-assign", new ICommandHandler() {
			public void executeCommand ( StringTokenizer args )
			{ CmdVariables.commandVarAssign ( args ); }
		} );
	}

	// -----------------------------------------------------------------

	private void registerCommand (
		String name,
		ICommandHandler handler )
	{
		d_name2command.put ( name, new CommandInfo ( handler ) );
	}

	// -----------------------------------------------------------------

	private void registerCommand (
		String name,
		ICommandHandler handler,
		int traits )
	{
		d_name2command.put ( name, new CommandInfo ( handler, traits ) );
	}

	// -----------------------------------------------------------------

	public boolean existCommand ( String name )
	{
		boolean result = d_name2command.containsKey ( name );
		return result;
	}

	// -----------------------------------------------------------------

	public CommandInfo getCommand ( String name )
	{
		CommandInfo result = d_name2command.get ( name );
		return result;
	}

	// -----------------------------------------------------------------

	public interface ICommandHandler
	{
		public abstract void executeCommand ( StringTokenizer t ) throws Exception;
	}

	// -----------------------------------------------------------------

	class CommandInfo
	{
		public static final int DontPrintPrompt = 0x1;
		// due to race-conditions asynchronous commands has to print prompt on their own!
		public static final int Asynchronous = DontPrintPrompt | 0x2;

		CommandInfo ( ICommandHandler handler )
		{
			d_handler = handler;
			d_traits = 0;
		}

		CommandInfo ( ICommandHandler handler, int traits )
		{
			d_handler = handler;
			d_traits = traits;
		}

		boolean hasTrait( int trait )
		{
			boolean result = ( d_traits & trait ) == trait;
			return result;
		}

		public ICommandHandler d_handler;
		public int d_traits;
	}

	// -----------------------------------------------------------------

	private Map< String, CommandInfo > d_name2command =
		new HashMap< String, CommandInfo >();

	// -----------------------------------------------------------------
}

// ---------------------------------------------------------------------
