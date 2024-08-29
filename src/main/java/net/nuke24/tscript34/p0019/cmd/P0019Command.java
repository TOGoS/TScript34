package net.nuke24.tscript34.p0019.cmd;

import static net.nuke24.tscript34.p0019.util.DebugUtil.debug;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.tscript34.p0019.P0019;
import net.nuke24.tscript34.p0019.iface.SystemContext;
import net.nuke24.tscript34.p0019.util.HostSystemContext;
import net.nuke24.tscript34.p0019.util.JavaProjectBuilder;

public class P0019Command
{
	static final String indentSection(String indent, String text) {
		return text.replaceAll("\n", "\n"+indent);
	}
	
	static final String PRINT_HELP_CMD_NAME = "ts34p19:print-help";
	static final String PRINT_HELP_HELP_TEXT = PRINT_HELP_CMD_NAME+" ; print help\n";
	
	static final String PRINT_VERSION_CMD_NAME = "ts34p19:print-version";
	static final String PRINT_VERSION_HELP_TEXT = PRINT_VERSION_CMD_NAME+" ; print version\n";
	
	static final String PB_SELF_TEST_CMD_NAME = "ts34p19:project-builder-test";
	static final String PB_SELF_TEST_HELP_TEXT = PB_SELF_TEST_CMD_NAME+" ; run project builder self-test\n";
	
	static final String COMPILE_JAR_CMD_NAME = "ts34p19:compile-jar";
	static final String COMPILE_JAR_HELP_TEXT =
		COMPILE_JAR_CMD_NAME+" <options>\n"+
		"\n" +
		"Options:\n" +
		"  -o <path>            ; path to write JAR file\n" +
		"  --include-sources    ; include following source files in the JAR\n" +
		"  --java-sources=<dir> ; compile .java source files from source root <dir>\n" +
		"  --resources=<dir>    ; include resource files within <dir>\n" +
		"  --main-class=<classname> ; indicate the specified class as main\n" +
		"";
	
	static final String HELP_TEXT =
		"Usage: JavaProjectBuilder <options> <sub-command>\n"  +
		"\n" +
		"General options:\n" +
		"  --cd=<dir>           ; Use <dir> as 'current directory' for remaining operations\n" +
		"                       ; (doesn't necessarily apply to interpretation of --options)\n" +
		"\n" +
		"Subcommands:\n" +
		"\n" +
		indentSection("  ", COMPILE_JAR_HELP_TEXT)+
		"\n" +
		indentSection("  ", PRINT_HELP_HELP_TEXT)+
		"\n" +
		indentSection("  ", PRINT_VERSION_HELP_TEXT)+
		"\n" +
		indentSection("  ", PB_SELF_TEST_HELP_TEXT)+
		"";
	
	static final Pattern CD_PATTERN = Pattern.compile("--cd=(.*)");

	public static int main(
		final String[] args, int argi,
		final InputStream stdin,
		final PrintStream stdout,
		final PrintStream errout,
		SystemContext ctx
	) {
		try {
			Matcher m;
			
			while( argi < args.length ) {
				String arg = args[argi++];
				
				// Translate some option-looking arguments to commands
				if( "--help".equals(arg) || "-?".equals(arg) ) {
					arg = PRINT_HELP_CMD_NAME;
				} else if( "--version".equals(arg) ) {
					arg = PRINT_VERSION_CMD_NAME;
				}
				
				if( COMPILE_JAR_CMD_NAME.equals(arg) ) {
					return JavaProjectBuilder.compileJarMain(args, argi, stdin, stdout, errout, ctx);
				} else if( PRINT_HELP_CMD_NAME.equals(arg) ) {
					stdout.println(P0019.PROGRAM_NAME);
					stdout.println();
					stdout.print(HELP_TEXT);
					return 0;
				} else if( PRINT_VERSION_CMD_NAME.equals(arg) ) {
					stdout.println(P0019.PROGRAM_NAME);
					return 0;
				} else if( (m = CD_PATTERN.matcher(arg)).matches() ) {
					ctx = ctx.withPwd(HostSystemContext.resolveRelative(ctx.getPwd(), m.group(1)));
				} else {
					return P0019.scriptMain(args, --argi, stdin, stdout, errout);
				}
			}
			
			errout.println("Warning: No command given");
			
			return 0;
		} catch( Exception e ) {
			e.printStackTrace(errout);
			return 1;
		}
	}
	
	public static void main(String[] args) throws Exception {
		int exitCode = main(args, 0, System.in, System.out, System.err, HostSystemContext.fromEnv());
		debug("Exiting with code "+exitCode);
		System.exit(exitCode);
	}
}