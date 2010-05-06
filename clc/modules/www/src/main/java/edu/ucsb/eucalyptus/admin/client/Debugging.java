package edu.ucsb.eucalyptus.admin.client;

/**
 * Debugging utils.
 */
public class Debugging {

	public static boolean enable = true;
	
	public static void log(String msg) {
		if (enable) {
			console_log(msg);
		}
	}
	
	public static native void console_log(String msg) /*-{
		console.log(msg);
	}-*/;
}
