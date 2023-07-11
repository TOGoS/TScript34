package net.nuke24.jcr36;

import java.util.HashMap;

import net.nuke24.jcr36.action.JCRAction;
import net.nuke24.jcr36.action.LetEnv;
import net.nuke24.jcr36.action.Print;
import net.nuke24.jcr36.action.RunExternalProgram;

/**
 * Parses commands of the form
 * 
 *   [option|bindings]* [--] command ...args
 * 
 * option = one of
 *   --help                 ; Show this help text and exit
 * binding =  
 *   <varname> 
 * 
 * Similar to sh/bash/etc, but with some extensions.
 * 
 * Does not support variables or any escape mechanisms.
 */
public class SimpleCommandParser {
	public static final String HELP_TEXT =
		"Simple command syntax: [--version|--help] [<var>=<value> ...] [--] <command> [<arg> ...]\n";
	
	/**
	 * Parse command, assuming args[offset] is the command name;
	 * i.e. <Var>=<value> and options have been parsed.
	 */
	public static JCRAction parse2(String[] args, int offset, Function<Integer,JCRAction> onExit) {
		assert(args.length > offset);
		String arg0 = args[offset];
		String aliased;
		while( (aliased = CommandNames.DEFAULT_ALIASES.get(arg0)) != null ) {
			arg0 = aliased;
		}
		if( CommandNames.CMD_DOCMD.equals(arg0) ) {
			return parseDoCmd(args, offset+1, onExit);
		} else if( CommandNames.CMD_PRINT.equals(arg0) ) {
			return parsePrint(args, offset+1);
		} else {
			return new RunExternalProgram(ArrayUtils.slice(args, offset, String.class), onExit);
		}
	}
	
	public static JCRAction parsePrint(String[] args, int offset) {
		String suffix = "\n";
		for( ; offset < args.length ; ++offset ) {
			if( "-n".equals(args[offset]) ) {
				suffix = "";
			} else if( "--".equals(args[offset]) ) {
				++offset;
				break;
			} else if( args[offset].startsWith("-") ) {
				throw new IllegalArgumentException("Unrecognized option to jcr:print: "+StringUtils.quote(args[offset]));
			} else {
				break;
			}
		}
		StringBuilder toPrint = new StringBuilder();
		String sep = "";
		for( ; offset < args.length ; ++offset ) {
			toPrint.append(sep).append(args[offset]);
			sep = " ";
		}
		toPrint.append(suffix);
		return new Print(toPrint.toString(), Streams.STDOUT_FD);
	}
	
	public static JCRAction parseDoCmd(String[] args, int offset, Function<Integer,JCRAction> onExit) {
		HashMap<String,String> envVars = new HashMap<String,String>();
		for( ; offset < args.length ; ++offset ) {
			String arg = args[offset];
			
			if( "--".equals(arg) ) {
				++offset;
				break;
			}
			
			if( arg.startsWith("-") ) {
				if( "--help".equals(arg) || "-?".equals(arg) || "-h".equals(arg) ) {
					return new Print(HELP_TEXT, Streams.STDOUT_FD);
				} else {
					throw new IllegalArgumentException("Unrecognized option: "+arg);
				}
			}
			
			int equalIndex = arg.indexOf('=');
			if( equalIndex == -1 ) break;
			
			envVars.put(arg.substring(0, equalIndex), arg.substring(equalIndex+1));
		}
		
		JCRAction rest = parse2(args, offset, onExit);
		
		return LetEnv.of(envVars, rest);
	}
	
	public static JCRAction parseDoCmd(String[] args, Function<Integer,JCRAction> onExit) {
		return parseDoCmd(args, 0, onExit);
	}
	
	public static JCRAction parseDoCmd(String[] args) {
		return parseDoCmd(args, RunExternalProgram.DEFAULT_ON_EXIT);
	}
}
