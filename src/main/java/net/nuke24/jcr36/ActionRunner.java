package net.nuke24.jcr36;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

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
	
	// Note: This currently assumes each action runs in series.
	// Will need to redesign for parallel running, including to support pipelines!
	protected void step(ActionAndContext aac) throws QuitException {
		JCRAction action = aac.action;
		Context ctx = aac.context;
		if( action instanceof QuitException ) {
			throw new QuitException( ((QuitException)action).exitCode );
		} else if( action instanceof ShellCommand ) {
			ShellCommand sc = (ShellCommand)action;
			ProcessBuilder pb = new ProcessBuilder( sc.argv );
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
		} else if( action instanceof NullAction ) {
			// Do nothing!
		} else if( action instanceof PrintAction ) {
			PrintAction printAct = (PrintAction)action;
			// TODO: Print to ctx.stdout
			System.out.print(printAct.text);
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
