/*
 * For future internationalization support
 */

package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.i18n.client.Messages;

public interface AppMessages extends Messages {
    String m_clickMeButton_text();
	String m_helloAlert_text(String toolkit);
	
	/* login page */
	String pleaseSignIn();
	String usernameField();
	String passwordField();
	String signInButton();
	String rememberMe();
	String applyButton();
	String forAccount();
	String recoverButton();
	String thePassword();
}
