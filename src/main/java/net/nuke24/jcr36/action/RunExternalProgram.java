package net.nuke24.jcr36.action;

import java.util.Arrays;

import net.nuke24.jcr36.Function;
import net.nuke24.jcr36.StringUtils;

// TODO: Rename to 'run external program' or somesuch
public class RunExternalProgram implements JCRAction {
	public static final Function<Integer, JCRAction> DEFAULT_ON_EXIT = new Function<Integer,JCRAction>() {
		@Override public JCRAction apply(Integer code) {
			return code.intValue() == 0 ? Null.INSTANCE : new Quit(code);
		}
		@Override public String toString() {
			return "(code) => code == 0 ? "+Null.INSTANCE + " : QuitAction(code)";
		}
	};
	
	public final String[] argv;
	public final Function<Integer, JCRAction> onExit;
	
	public RunExternalProgram(String[] argv, Function<Integer, JCRAction> onExit) {
		this.argv = argv;
		this.onExit = onExit;
	}
	
	@Override public boolean equals(Object other) {
		if( !(other instanceof RunExternalProgram) ) return false;
		
		RunExternalProgram osc = (RunExternalProgram)other;
		return this.onExit.equals(osc.onExit) && Arrays.equals(this.argv, osc.argv);
	}
	
	@Override public int hashCode() {
		return getClass().getSimpleName().hashCode() +  31*Arrays.hashCode(argv) + 31*31*onExit.hashCode();
	}
	
	@Override public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName()+"([");
		String sep = "";
		for( String arg : this.argv ) {
			sb.append(sep).append(StringUtils.quote(arg));
			sep = ", ";
		}
		sb.append("])");
		if( this.onExit != DEFAULT_ON_EXIT ) {
			sb.append(" >>= ").append(onExit);
		}
		return sb.toString();
	}
}
