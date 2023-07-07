package net.nuke24.jcr36;

import net.nuke24.jcr36.ActionRunner.QuitException;

public class SimpleCommandRunner {
	public static void main(String[] args) {
		JCRAction action = SimpleCommandParser.parse(args, code -> {
			if( code == 0 ) return NullAction.INSTANCE;
				
			return new SerialAction(
				// TODO: To stdout, and include command, env, etc 
				new PrintAction("Command failed with code "+code),
				new QuitAction(code)
			);
		});
		try {
			ActionRunner actionRunner = new ActionRunner();
			actionRunner.run(action, actionRunner.createContextFromEnv());
		} catch( QuitException e ) {
			System.exit(e.exitCode);
		}
	}
}
