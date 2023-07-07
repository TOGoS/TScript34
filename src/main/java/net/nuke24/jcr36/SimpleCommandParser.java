package net.nuke24.jcr36;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Parses commands of the form
 * 
 *   [option|bindings]* [--] command ...args
 * 
 * option = one of
 *   --help
 * binding =  
 *   <varname> 
 * 
 * Similar to sh/bash/etc, but with some extensions.
 * 
 * Does not support variables or any escape mechanisms.
 */
public class SimpleCommandParser {
	public static final String HELP_TEXT =
		"Simple command syntx: [--help] [<var>=<value> ...] [--] <command> [<arg> ...]\n";
	
	public static JCRAction parse(String[] args, Function<Integer,JCRAction> onExit) {
		HashMap<String,String> envVars = new HashMap<String,String>();
		int commandIndex = 0;
		for( ; commandIndex < args.length ; ++commandIndex ) {
			String arg = args[commandIndex];
			
			if( "--".equals(arg) ) {
				++commandIndex;
				break;
			}
			
			if( arg.startsWith("-") ) {
				if( "--help".equals(arg) || "-?".equals(arg) || "-h".equals(arg) ) {
					return new PrintAction(HELP_TEXT);
				} else {
					throw new IllegalArgumentException("Unrecognized option: "+arg);
				}
			}
			
			int equalIndex = arg.indexOf('=');
			if( equalIndex == -1 ) break;
			
			envVars.put(arg.substring(0, equalIndex), arg.substring(equalIndex+1));
		}
		
		String[] command = new String[args.length - commandIndex];
		for( int i=0; i<command.length; ++i ) {
			command[i] = args[commandIndex++];
		}
		return LetEnv.of(envVars, new ShellCommand(command, onExit));
	}
	
	public static JCRAction parse(String[] args) {
		return parse(args, code -> code == 0 ? NullAction.INSTANCE : new QuitAction(code));
	}
}
