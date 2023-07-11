package net.nuke24.jcr36;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.nuke24.jcr36.ActionRunner.QuitException;
import net.nuke24.jcr36.action.JCRAction;
import net.nuke24.jcr36.action.Null;
import net.nuke24.jcr36.action.Print;
import net.nuke24.jcr36.action.Quit;
import net.nuke24.jcr36.action.SerialAction;

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
