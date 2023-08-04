package net.nuke24.jcr36;

import net.nuke24.jcr36.ActionRunner.QuitException;
import net.nuke24.jcr36.action.JCRAction;

public class SimpleCommandRunner {
	public static void main(String[] args) {		
		JCRAction action = SimpleCommandParser.parseDoCmd(args);
		try {
			ActionRunner actionRunner = new ActionRunner();
			actionRunner.run(action, actionRunner.createContextFromEnv());
		} catch( QuitException e ) {
			System.exit(e.exitCode);
		}
	}
}
