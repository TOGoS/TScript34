package net.nuke24.jcr36;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Pattern;

import net.nuke24.jcr36.action.JCRAction;
import net.nuke24.jcr36.action.Null;
import net.nuke24.jcr36.action.Print;
import net.nuke24.jcr36.action.RunExternalProgram;
import net.nuke24.jcr36.action.SerialAction;

public class ActionRunner {
	class Context {
		public final Map<String,String> env;
		// Note: Might want to just use Input/OutputStreams instead of Redirects,
		// since we use these for more than just passing to ProcessBuilders.
		public final Redirect stdinRedirect;
		public final Redirect stdoutRedirect;
		public final Redirect stderrRedirect;
		
		Context(
			Map<String,String> env,
			Redirect stdinRedirect,
			Redirect stdoutRedirect,
			Redirect stderrRedirect
		) {
			this.env            =            env;
			this.stdinRedirect  =  stdinRedirect;
			this.stdoutRedirect = stdoutRedirect;
			this.stderrRedirect = stderrRedirect;
		}
		
		public boolean isProgramPathResolutionEnabled() {
			String pprStr = env.get("JCR_PROGRAM_PATH_RESOLUTION");
			if( pprStr == null ) return true;
			return StringUtils.parseBool(pprStr, "JCR_PROGRAM_PATH_RESOLUTION");
		}
	}
	
	class ActionAndContext {
		public final JCRAction action;
		public final Context context;
		public ActionAndContext(JCRAction act, Context ctx) {
			this.action = act;
			this.context = ctx;
		}
	}
	
	class QuitException extends Throwable {
		public final int exitCode;
		public QuitException(int exitCode) {
			this.exitCode = exitCode;
		}
	}
	
	Queue<ActionAndContext> queue = new LinkedList<ActionAndContext>();
	
	public Context createContextFromEnv() {
		return new Context(
			System.getenv(),
			Redirect.INHERIT,
			Redirect.INHERIT,
			Redirect.INHERIT
		);
	}
	
	Function<String,String> programPathResolver;
	
	protected String resolveProgram(String name, Context ctx) {
		if( !ctx.isProgramPathResolutionEnabled() ) return name;
		
		String pathSepRegex = Pattern.quote(File.pathSeparator);
		
		String pathsStr = System.getenv("PATH"); // TODO: Shouldn't this be ctx.env.get
		if( pathsStr == null ) pathsStr = "";
		String[] pathParts = pathsStr.length() == 0 ? new String[0] : pathsStr.split(pathSepRegex);
		String pathExtStr = System.getenv("PATHEXT"); // TODO: Shouldn't this be ctx.env.get
		String[] pathExts = pathExtStr == null || pathExtStr.length() == 0 ? new String[] {""} : pathExtStr.split(pathSepRegex);
		
		for( String path : pathParts ) {
			for( String pathExt : pathExts ) {
				File candidate = new File(path + File.separator + name + pathExt);
				// System.err.println("Checking for "+candidate.getPath()+"...");
				if( candidate.exists() ) return candidate.getPath();
			}
		}
		return name;
	}
	
	protected PrintStream getPrintStream(int fd, Context ctx) {
		// TODO: Mind redirects
		switch( fd ) {
		case 1: return System.out;
		case 2: return System.err;
		default:
			throw new RuntimeException("Don't know how to print to stream #"+fd);
		}
	}
	
	// Note: This currently assumes each action runs in series.
	// Will need to redesign for parallel running, including to support pipelines!
	protected void step(ActionAndContext aac) throws QuitException {
		JCRAction action = aac.action;
		Context ctx = aac.context;
		if( action instanceof QuitException ) {
			throw new QuitException( ((QuitException)action).exitCode );
		} else if( action instanceof RunExternalProgram ) {
			RunExternalProgram sc = (RunExternalProgram)action;
			
			if( sc.argv.length == 0 ) throw new RuntimeException("Don't know how to run shell command with zero arguments");
			
			String[] resolvedArgv = new String[sc.argv.length];
			resolvedArgv[0] = resolveProgram(sc.argv[0], ctx);
			for( int i=1; i<sc.argv.length; ++i ) resolvedArgv[i] = sc.argv[i];
			
			ProcessBuilder pb = new ProcessBuilder( resolvedArgv );
			pb.environment().clear();
			pb.environment().putAll(ctx.env);
			pb.redirectInput(ctx.stdinRedirect);
			pb.redirectOutput(ctx.stdoutRedirect);
			pb.redirectError(ctx.stderrRedirect);
			try {
				Process process = pb.start();
				int result = process.waitFor();
				JCRAction next = sc.onExit.apply(result);
				if( next != null ) queue.add(new ActionAndContext(next, ctx));
			} catch( IOException e ) {
				// Might add 'onExecutionError', but for now...
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} else if( action instanceof Null ) {
			// Do nothing!
		} else if( action instanceof Print ) {
			Print printAct = (Print)action;
			getPrintStream(printAct.fd, ctx).print(printAct.text);
		} else if( action instanceof SerialAction ) {
			for( JCRAction sub : ((SerialAction)action).children ) {
				this.queue.add(new ActionAndContext(sub, ctx));
			}
		} else {
			throw new RuntimeException("Un[yet]supported action class: "+action.getClass());
		}
	}
	
	protected void run() throws QuitException {
		ActionAndContext nextAac;
		while( (nextAac = this.queue.poll()) != null ) {
			step(nextAac);
		}
	}
	
	public void run(JCRAction action, Context ctx) throws QuitException {
		this.queue.add(new ActionAndContext(action, ctx));
		run();
	}
}
