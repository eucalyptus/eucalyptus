package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Widget;

public class AccountingControl implements ContentControl {
		
	private Frame root;
	
	public AccountingControl(String sessionId) {
    	root = new Frame();
	}
	
	@Override
	public Widget getRootWidget() {
		return this.root;
	}
	
	@Override
	public void display() {
		root.setUrl("http://www.google.com");
	}
}
