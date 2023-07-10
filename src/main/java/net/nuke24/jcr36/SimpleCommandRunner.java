package net.nuke24.jcr36;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.jcr36.ActionRunner.QuitException;

public class SimpleCommandRunner {
	protected static final Pattern RESOLVE_PROGRAM_PATHS_OPTION_PATTERN = Pattern.compile("^--resolve-program-paths=(.*)$");
	
	// Separate from SimpleCommandParser.parse since some options are global to the action runner,
	// not specific to the action.  Though maybe this is a mistake.  Could use environment variables instead,
	// and then there would be no need for this special case.
	static class SCROptions {
		public enum Mode {
			PRINT_HELP,
			PRINT_VERSION,
			PRINT_USAGE_ERRORS,
			RUN
		};
		
		public final Mode mode;
		public final boolean shouldResolvePaths;
		public final String[] argv;
		public final List<String> usageErrors;
		
		SCROptions(Mode mode, boolean shouldResolvePaths, String[] argv, List<String> usageErrors ) {
			this.mode = mode;
			this.shouldResolvePaths = shouldResolvePaths;
			this.argv = argv;
			this.usageErrors = usageErrors;
		}
		
		protected static final SCROptions blank(Mode mode) {
			return new SCROptions(mode, false, new String[0], Collections.<String>emptyList());
		}
		
		public static final SCROptions PRINT_HELP = blank(Mode.PRINT_HELP);
		public static final SCROptions PRINT_VERSION = blank(Mode.PRINT_VERSION);
		
		public static SCROptions parse(String[] args ) {
			boolean shouldResolvePaths = System.getenv("PATHEXT") != null;
			int argEnd = 0;
			Matcher m;
			for( argEnd=0; argEnd<args.length; ++argEnd ) {
				if( "--help".equals(args[argEnd]) ) {
					return SCROptions.PRINT_HELP;
				} else if( "--version".equals(args[argEnd]) ) {
						return SCROptions.PRINT_VERSION;
				} else if( "--resolve-program-paths".equals(args[argEnd]) ) {
					shouldResolvePaths = true;
				} else if( (m = RESOLVE_PROGRAM_PATHS_OPTION_PATTERN.matcher(args[argEnd])).matches() ) {
					shouldResolvePaths = StringUtils.parseBool(m.group(1), args[argEnd]);
				} else {
					break;
				}
			}
			return new SCROptions(Mode.RUN, shouldResolvePaths, ArrayUtils.slice(args, argEnd, String.class), Collections.<String>emptyList());
		}
	}
	
	protected static JCRAction optsToAction(SCROptions opts) {
		switch( opts.mode ) {
		case RUN:
			return SimpleCommandParser.parse(opts.argv, new Function<Integer,JCRAction>() {
				@Override public JCRAction apply(Integer code) {
					if( code.intValue() == 0 ) return NullAction.INSTANCE;
					
					return new SerialAction(
						// TODO: To stdout, and include command, env, etc 
						new PrintAction("Command failed with code "+code, Streams.STDERR_FD),
						new QuitAction(code.intValue())
					);
				}
			});
		case PRINT_HELP:
			return new PrintAction(
				// TODO: Our own help text, unless I unify this stuff.
				SimpleCommandParser.HELP_TEXT,
				Streams.STDOUT_FD
			);			
		case PRINT_VERSION:
			return new PrintAction(
				Versions.JCR_NAME_AND_VERSION,
				Streams.STDOUT_FD
			);
		default:
			return new SerialAction(
				// TODO: To stderr 
				new PrintAction("Oops, mode="+opts.mode+" not implemented", Streams.STDERR_FD),
				new QuitAction(1)
			);
		}
	}
	
	public static void main(String[] args) {
		SCROptions opts = SCROptions.parse(args);
		
		JCRAction action = optsToAction(opts);
		try {
			ActionRunner actionRunner = new ActionRunner();
			actionRunner.programPathResolutionEnabled = opts.shouldResolvePaths;
			actionRunner.run(action, actionRunner.createContextFromEnv());
		} catch( QuitException e ) {
			System.exit(e.exitCode);
		}
	}
}
