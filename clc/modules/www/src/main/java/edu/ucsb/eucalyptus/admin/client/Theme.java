package edu.ucsb.eucalyptus.admin.client;

import java.util.HashMap;

public final class Theme {

	public static native String draw_header ()/*-{
		return $wnd.draw_header ();
	}-*/;
}
