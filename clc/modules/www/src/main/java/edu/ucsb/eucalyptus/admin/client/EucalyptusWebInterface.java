/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.ucsb.eucalyptus.admin.client.extensions.store.ImageStoreClient;
import edu.ucsb.eucalyptus.admin.client.extensions.store.ImageStoreWidget;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 *
 *
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 *
 *   SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *   BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *   THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *   OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *   WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *   ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/

public class EucalyptusWebInterface implements EntryPoint {

	private static final AppMessages MSG = (AppMessages) GWT.create(AppMessages.class);

    private static String cookie_name = "eucalyptus-session-id";
    private static int minPasswordLength = 5;  /* TODO: put into config? */

    /* configuration parameters to be set from the server */
    private static Boolean server_ready = new Boolean(false);
    private static String signup_greeting;
    private static String version;
    private static String cloud_name;
    private static String certificate_download_text;
    private static String rest_credentials_text;
    private static String user_account_text;
    private static String admin_first_time_config_text;
    private static String admin_email_change_text;
	private static String admin_cloud_ip_setup_text;
    private static boolean request_telephone;
    private static boolean request_project_leader;
    private static boolean request_affiliation;
    private static boolean request_project_description;
	private static boolean show_cloud_registration;
	private static String cloud_registration_text;
    private static Image logo = null;
	private static Image textless_logo = null;
	private static String rightscale_base_url = null;
	private static String rightscaleUrl = null;
    private static String extensions = null;

    /* global variables */
    private static HashMap props;
    private static HashMap urlParams;
    private static String sessionId;
    private static String currentAction;
    private static UserInfoWeb loggedInUser;
	private static CloudInfoWeb cloudInfo;
	private static TabBar allTabs;
    private static int currentTabIndex = 0;
	private static int credsTabIndex = -1;
    private static int imgTabIndex = -1;
	private static int usrTabIndex = -1;
	private static int confTabIndex = -1;
    private static int downTabIndex = -1;
    private static int storeTabIndex = -1;
    private static boolean sortUsersLastFirst = true;

    /* UI selections remembered for future use */
    private static boolean previousSkipConfirmation = false; // do not skip email confirmation by default

    /* globally visible UI widgets */
    private Label label_box = new Label();
    private CheckBox check_box = new CheckBox("", false);
    private Label remember_label = new Label(MSG.rememberMe());
    private static Label statusMessage = new Label();

    public void onModuleLoad()
    {
        sessionId = Cookies.getCookie( cookie_name );
        urlParams = GWTUtils.parseParamString( GWTUtils.getParamString() );

        /* if specified, 'page' will tell us which tab to select */
        String page = ( String ) urlParams.get( "page" );
        if (page!=null) { currentTabIndex = Integer.parseInt(page); }

        currentAction = ( String ) urlParams.get( "action" );

        displayStatusPage("Loading data from server...");
        EucalyptusWebBackend.App.getInstance().getProperties(
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        props = ( HashMap ) result;
                        try {
                            load_props(); /* verify properties */

							/* these don't need sessions */
							if ( currentAction!=null && (currentAction.equals ("confirm")
							|| currentAction.equals ("recover")) ) {
								executeAction( currentAction );

							} else {
								/* if we have don't have sessionId saved in a cookie */
								if ( sessionId == null )
								{
									displayLoginPage();
								}
								else /* we have a cookie - try using it */
								{
									check_box.setChecked(true);
									attemptLogin();
								}
							}
                        } catch (Exception e) {
                            displayErrorPageFinal ("Internal error (1): " + e.getMessage());
                        }
                    }
                    public void onFailure( Throwable caught )
                    {
                        displayErrorPageFinal ("Internal error (2): " + caught.getMessage());
                    }
                }
        );
    }

    void load_props() throws Exception
    {
        if (props == null) {
            throw new Exception("Invalid server configuration");
        }
        version = (String)props.get("version");
        cloud_name = (String)props.get("cloud-name");
        signup_greeting = (String)props.get("signup-greeting");
        certificate_download_text = (String)props.get("certificate-download-text");
        rest_credentials_text = (String)props.get("rest-credentials-text");
        user_account_text = (String)props.get("user-account-text");
        admin_first_time_config_text = (String)props.get("admin-first-time-config-text");
        admin_email_change_text = (String)props.get("admin-email-change-text");
		admin_cloud_ip_setup_text = (String)props.get("admin-cloud-ip-setup-text");
        server_ready = (Boolean)props.get("ready");

        if (server_ready==null) {
            throw new Exception("Internal server error (cannot determine server readiness)");
        }
        if (cloud_name==null) {
            throw new Exception("Server configuration is missing 'cloud-name' value");
        }
        if (signup_greeting==null) {
            throw new Exception("Server configuration is missing 'signup-greeting' value");
        }
        if (certificate_download_text==null) {
            throw new Exception("Server configuration is missing 'certificate-dowload-text' value");
        }
        if (rest_credentials_text==null) {
            throw new Exception("Server configuration is missing 'rest-credentials-text' value");
        }
        if (user_account_text==null) {
            throw new Exception("Server configuration is missing 'user-account-text' value");
        }
        if (admin_first_time_config_text==null) {
            throw new Exception("Server configuration is missing 'admin-first-time-config-text' value");
        }
        if (admin_email_change_text==null) {
            throw new Exception("Server configuration is missing 'admin-email-change-text' value");
        }
        if (admin_cloud_ip_setup_text==null) {
            throw new Exception("Server configuration is missing 'admin-cloud-ip-setup-text' value");
        }

        /* optional parameters (booleans will be 'yes' if not specified) */
        request_telephone = str2bool((String)props.get("request-telephone"));
        request_project_leader = str2bool((String)props.get("request-project-leader"));
        request_affiliation = str2bool((String)props.get("request-affiliation"));
        request_project_description = str2bool((String)props.get("request-project-description"));
        String file = (String)props.get("logo-file");
        if (file!=null) {
            logo = new Image(file);
        }
		file = (String)props.get("logo-textless-file");
		if (file!=null) {
			textless_logo = new Image(file);
		}
		
		show_cloud_registration = str2bool((String)props.get("show-cloud-registration"));
		cloud_registration_text = (String)props.get("cloud-registration-text");
        rightscale_base_url = (String)props.get("rightscale-registration-base-url");
		if (cloud_registration_text==null || rightscale_base_url==null) {
			show_cloud_registration = false;
		}
        extensions = (String)props.get("extensions");

    }

    private boolean str2bool(String s)
    {
        if (s==null) {
            return true;
        }
        if (s.equalsIgnoreCase("no")
                || s.equalsIgnoreCase("n")
                || s.equalsIgnoreCase("0")) {
            return false;
        } else {
            return true;
        }
    }

    public void displayLoginPage()
    {
        if ( currentAction == null ) {
            displayLoginPage(MSG.pleaseSignIn() + ":");
        } else {
            if ( currentAction.equals( "approve" )
                    || currentAction.equals( "reject" )
                    || currentAction.equals( "disable" )
                    || currentAction.equals( "delete" )) {
                displayLoginPage("Please, log into Eucalyptus to " + currentAction + " the user");

            } else if ( currentAction.equals( "delete_image")) {
                displayLoginPage("Please, log into Eucalyptus to delete the image");

            } else if ( currentAction.equals( "confirm" ) ) {
                displayLoginPage("Please, log into Eucalyptus to confirm your acccount");

            } else { /* unknown action - will be caught upon login */
                displayLoginPage(MSG.pleaseSignIn() + ":");
            }
            label_box.setStyleName ("euca-greeting-warning"); /* highlight the message */
        }
    }

    public void displayLoginPage(String greeting)
    {
        History.newItem("login");
        label_box.setText( greeting );
        label_box.setStyleName("euca-greeting-normal");
        final TextBox login_box = new TextBox();
        login_box.setFocus(true); // this box gets focus first
        final PasswordTextBox pass_box = new PasswordTextBox();

        ClickListener LoginButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                /* perform checks */
                if ( login_box.getText().length() < 1 )
                {
                    displayLoginErrorPage("Username is empty!");
                    return;
                }
                if ( pass_box.getText().length() < 1 )
                {
                    displayLoginErrorPage("Password is empty!");
                    return;
                }

                label_box.setText("Contacting the server...");
                label_box.setStyleName("euca-greeting-pending");
                EucalyptusWebBackend.App.getInstance().getNewSessionID(
                        login_box.getText(),
                        GWTUtils.md5(pass_box.getText()),
                        new AsyncCallback() {
                            public void onSuccess( Object result )
                            {
                                sessionId = ( String ) result;
                                long expiresMs = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000); /* week */
                                Date expires = new Date(expiresMs);
                                if (check_box.isChecked()) {
                                    Cookies.setCookie( cookie_name, sessionId, expires);
                                } else {
                                    /* this cookie should expire at the end of the session */
                                    /* TODO: does this work right in all browsers? */
                                    Cookies.setCookie( cookie_name, sessionId, new Date(0));
                                }
                                attemptLogin();
                            }

                            public void onFailure( Throwable caught )
                            {
                                displayLoginErrorPage((String)caught.getMessage());
                            }
                        }
                );
            }
        };

        ClickListener RecoverButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                displayPasswordRecoveryPage();
            }
        };

        Button submit_button = new Button( MSG.signInButton(), LoginButtonListener );
        Hyperlink signup_button = new Hyperlink( MSG.applyButton(), "apply" );
        signup_button.addClickListener( AddUserButtonListener );
        Hyperlink recover_button = new Hyperlink( MSG.recoverButton(), "recover" );
        recover_button.addClickListener( RecoverButtonListener );
        remember_label.setStyleName("euca-remember-text");

        /* enable login by pressing Enter */
        EucalyptusKeyboardListener sl = new EucalyptusKeyboardListener(submit_button);
        submit_button.addKeyboardListener(sl);
        login_box.addKeyboardListener(sl);
        pass_box.addKeyboardListener(sl);

        Grid g = new Grid( 4, 2 );
        g.setCellSpacing(4);
        g.setWidget(0, 0, new Label(MSG.usernameField()+":"));
        g.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        g.setWidget(1, 0, new Label(MSG.passwordField()+":"));
        g.getCellFormatter().setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        g.setWidget(0, 1, login_box );
        g.setWidget(1, 1, pass_box );
        g.setWidget(2, 0, check_box);
        g.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        g.setWidget(2, 1, remember_label);
        g.setWidget(3, 1, submit_button);
        VerticalPanel panel = new VerticalPanel();
        panel.add (g);
        panel.setStyleName("euca-login-panel");
        panel.setCellHorizontalAlignment(g, HasHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(g, HasVerticalAlignment.ALIGN_MIDDLE);

        HorizontalPanel hpanel = new HorizontalPanel();
        hpanel.setSpacing(2);
        hpanel.add( signup_button );
        hpanel.add( new HTML("&nbsp;" + MSG.forAccount() + "&nbsp;&nbsp;|&nbsp;&nbsp;") );
        hpanel.add( recover_button );
        hpanel.add( new HTML("&nbsp;" + MSG.thePassword()) );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        if (version!=null) { 
            Label version_label = new Label ("Version " + version);
            version_label.setStyleName ("euca-small-text");
            vpanel.add (version_label);
        }
        vpanel.add (label_box);
        vpanel.add (panel);
        if (server_ready.booleanValue()) {
            vpanel.add (hpanel);
        }

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }

    public void displayLoginErrorPage ( String message )
    {
        if (message.equals("Earlier session not found") ||
                message.equals("Earlier session expired")) {
            displayLoginPage();
        } else {
            displayLoginPage("Error: " + message);
            label_box.setStyleName("euca-greeting-warning");
        }
    }

	/* this handles sign-ups, adding of users by admin, and editing of users */
    public void displayUserRecordPage( Panel parent, UserInfoWeb userToEdit)
    {
		final String oldPassword;
        final boolean admin;
		final boolean newUser;
        final boolean showSkipConfirmed;
        boolean isAdminChecked = false; // not admin by default
        boolean skipConfirmationChecked = previousSkipConfirmation;

        if (loggedInUser != null
                && loggedInUser.isAdministrator()) {
            admin = true;
        } else {
            admin = false;
        }
		if (userToEdit==null) {
			newUser = true;
            showSkipConfirmed = true;
			userToEdit = new UserInfoWeb();
			oldPassword = "";
			if ( admin ) {
	            label_box.setText ("Please, fill out the form to add a user");
	        } else {
	            label_box.setText ( signup_greeting ); // Please, fill out the form:
	        }
		} else {
			newUser = false;
			oldPassword = userToEdit.getPassword();
            isAdminChecked = userToEdit.isAdministrator();
            showSkipConfirmed = !userToEdit.isConfirmed();
            skipConfirmationChecked = userToEdit.isConfirmed();

            String status;
            if (!userToEdit.isApproved()) {
                status = "unapproved";
            } else if (!userToEdit.isEnabled()) {
                status = "disabled";
            } else if (!userToEdit.isConfirmed()) {
                status = "unconfirmed";
            } else {
                status = "active";
            }
            if (userToEdit.isAdministrator()) {
                status += " & admin";
            }
			label_box.setText ("Editing information for user '" + userToEdit.getUserName() +"' (" + status + ")" );
		}
        label_box.setStyleName("euca-greeting-normal");

        int rowsMandatory = 5;
        if (admin) {
            rowsMandatory++; // for admin checkbox
            if (showSkipConfirmed) {
                rowsMandatory++; // for skip confirmation checkbox
            }
        }
        final Grid g1 = new Grid ( rowsMandatory, 3 );
        g1.getColumnFormatter().setWidth(0, "180");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        final Label label_mandatory = new Label( "Mandatory fields:" );
        label_mandatory.setStyleName("euca-section-header");

        final int userName_row = i;
        g1.setWidget( i, 0, new Label( "Username:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox userName_box = new TextBox();
		userName_box.setText (userToEdit.getUserName());
        userName_box.setWidth("180");
		if ( ! newUser ) {
			userName_box.setEnabled (false);
		}
        g1.setWidget( i++, 1, userName_box );

        // optional row
        final CheckBox userIsAdmin = new CheckBox("Administrator");
        userIsAdmin.setChecked(isAdminChecked);
        userIsAdmin.setStyleName("euca-remember-text");
        if (admin) {
            g1.setWidget ( i++, 1, userIsAdmin);
        }

        final int password1_row = i;
        g1.setWidget( i, 0, new Label( "Password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword1_box = new PasswordTextBox();
		cleartextPassword1_box.setText (userToEdit.getPassword());
        cleartextPassword1_box.setWidth ("180");
		if ( (! admin && ! newUser ) || userToEdit.isAdministrator().booleanValue()) {
			cleartextPassword1_box.setEnabled (false);
		}
        g1.setWidget( i++, 1, cleartextPassword1_box );

        final int password2_row = i;
        g1.setWidget( i, 0, new Label( "Password, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword2_box = new PasswordTextBox();
		cleartextPassword2_box.setText (userToEdit.getPassword());
        cleartextPassword2_box.setWidth("180");
		if ( ( ! admin && ! newUser ) || userToEdit.isAdministrator().booleanValue()) {
			cleartextPassword2_box.setEnabled (false);
		}
        g1.setWidget( i++, 1, cleartextPassword2_box );

        final int realName_row = i;
        g1.setWidget( i, 0, new Label( "Full Name:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox realName_box = new TextBox();
		realName_box.setText (userToEdit.getRealName());
        realName_box.setWidth("180");
        g1.setWidget( i++, 1, realName_box );

        final int emailAddress_row = i;
        g1.setWidget( i, 0, new Label( "Email address:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox emailAddress_box = new TextBox();
		emailAddress_box.setText (userToEdit.getEmail());
        emailAddress_box.setWidth("180");
        g1.setWidget( i++, 1, emailAddress_box );

        // optional row
        final CheckBox skipConfirmation = new CheckBox("Skip email confirmation");
        skipConfirmation.setChecked(skipConfirmationChecked);
        skipConfirmation.setStyleName("euca-remember-text");
        if (admin && showSkipConfirmed) {
            g1.setWidget ( i++, 1, skipConfirmation);
        }

        /* these widgets are allocated, but not necessarily used */
        final Grid g2 = new Grid();
        final Label label_optional = new Label( "Optional fields:" );
        label_optional.setStyleName("euca-section-header");
        final TextBox telephoneNumber_box = new TextBox();
        final TextBox projectPIName_box = new TextBox();
        final TextBox affiliation_box = new TextBox();
        final TextArea projectDescription_box = new TextArea();

        int extra_fields = 0;
        if (request_telephone)           { extra_fields++; }
        if (request_project_leader)      { extra_fields++; }
        if (request_affiliation)         { extra_fields++; }
        if (request_project_description) { extra_fields++; }

        if (extra_fields > 0) {
            g2.resize(extra_fields, 2);
            g2.getColumnFormatter().setWidth(0, "180");
            g2.getColumnFormatter().setWidth(1, "360");
            i = 0;

            if (request_telephone) {
                g2.setWidget( i, 0, new Label( "Telephone Number:" ));
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
                telephoneNumber_box.setWidth("180");
				telephoneNumber_box.setText (userToEdit.getTelephoneNumber());
                g2.setWidget( i++, 1, telephoneNumber_box );
            }

            if (request_project_leader) {
                g2.setWidget( i, 0, new Label( "Project Leader:" ) );
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				projectPIName_box.setText (userToEdit.getProjectPIName());
                projectPIName_box.setWidth("180");
                g2.setWidget( i++, 1, projectPIName_box );
            }

            if (request_affiliation) {
                g2.setWidget( i, 0, new Label( "Affiliation:" ) );
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				affiliation_box.setText (userToEdit.getAffiliation());
                affiliation_box.setWidth("360");
                g2.setWidget( i++, 1, affiliation_box );
            }

            if (request_project_description) {
                g2.setWidget( i, 0, new Label( "Project Description:" ) );
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				projectDescription_box.setText (userToEdit.getProjectDescription());
                projectDescription_box.setWidth("360");
                projectDescription_box.setHeight("50");
                g2.setWidget( i++, 1, projectDescription_box );
            }
        }

        ClickListener SignupButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 4; j++ )
                {
                    g1.clearCell( j, 2 ); /* clear previous right-hand-side annotations */
                }

                // perform checks
                if ( userName_box.getText().length() < 1 )
                {
                    Label l = new Label( "Username is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( userName_row, 2, l);
                    formOk = false;
                } else {
					// do this in the else-clause so the empty username doesn't match here
	                if ( cleartextPassword1_box.getText().toLowerCase().matches(".*" +
	                        userName_box.getText().toLowerCase() + ".*")) {
	                    Label l = new Label ( "Password may not contain the username!");
	                    l.setStyleName("euca-error-hint");
	                    g1.setWidget( password1_row, 2, l );
	                    formOk = false;
	                }
				}

				if ( userName_box.getText().matches(".*[^\\w\\-\\.@]+.*") ) {
					Label l = new Label ("Invalid characters in the username!");
					l.setStyleName ("euca-error-hint");
					g1.setWidget (userName_row, 2, l);
					formOk = false;
				}

                if ( userName_box.getText().length() > 30)
                {
                    Label l = new Label( "Username is too long, sorry!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( userName_row, 2, l);
                    formOk = false;
                }

                if ( cleartextPassword1_box.getText().length() < minPasswordLength )
                {
                    Label l = new Label( "Password must be at least " + minPasswordLength + " characters long!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }
                if ( !cleartextPassword1_box.getText().equals( cleartextPassword2_box.getText() ) )
                {
                    Label l = new Label( "Passwords do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password2_row, 2, l );
                    formOk = false;
                }
                if ( realName_box.getText().equalsIgnoreCase(cleartextPassword1_box.getText())
                		|| userName_box.getText().equalsIgnoreCase(cleartextPassword1_box.getText())) {
                    Label l = new Label ( "Password may not be your name or username!");
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }

                if ( realName_box.getText().length() < 1 )
                {
                    Label l = new Label( "Name is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( realName_row, 2, l );
                    formOk = false;
                }
                if ( emailAddress_box.getText().length() < 1 )
                {
                    Label l = new Label( "Email address is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( emailAddress_row, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");
					String enteredPassword = cleartextPassword1_box.getText();
					String encryptedPassword = GWTUtils.md5(enteredPassword);
					if ( enteredPassword.equals(oldPassword)) {
						encryptedPassword = enteredPassword; // it was not changed in the edit
					}
                    final UserInfoWeb userToSave = new UserInfoWeb(
                            userName_box.getText(),
                            realName_box.getText(),
                            emailAddress_box.getText(),
                            encryptedPassword);
                    if ( admin ) {
                        userToSave.setAdministrator( userIsAdmin.isChecked());
                        if ( showSkipConfirmed ) {
                            previousSkipConfirmation = skipConfirmation.isChecked(); // remember value for the future
                            userToSave.setConfirmed(previousSkipConfirmation);
                        }
                    }
                    if ( telephoneNumber_box.getText().length() > 0 )
                    {
                        userToSave.setTelephoneNumber( telephoneNumber_box.getText() );
                    }
                    if ( affiliation_box.getText().length() > 0 )
                    {
                        userToSave.setAffiliation( affiliation_box.getText() );
                    }
                    if ( projectDescription_box.getText().length() > 0 )
                    {
                        userToSave.setProjectDescription( projectDescription_box.getText() );
                    }
                    if ( projectPIName_box.getText().length() > 0 )
                    {
                        userToSave.setProjectPIName( projectPIName_box.getText() );
                    }
					if (newUser) {
						EucalyptusWebBackend.App.getInstance().addUserRecord(
							sessionId, /* will be null if anonymous user signs up */
							userToSave,
						new AsyncCallback() {
							public void onSuccess( Object result )
							{
								displayDialog( "Thank you!", ( String ) result );
							}

							public void onFailure( Throwable caught )
							{
								String m = caught.getMessage();
								if ( m.equals( "User already exists" ) )
								{
									g1.setWidget( userName_row, 2, new Label( "Username is taken!" ) );
									label_box.setText( "Please, fix the error and resubmit:" );
									label_box.setStyleName("euca-greeting-warning");
								} else {
									displayErrorPage(m);
								}
							}
						}
						);
					} else {
						EucalyptusWebBackend.App.getInstance().updateUserRecord(
							sessionId,
							userToSave,
						new AsyncCallback() {
                            public void onSuccess(Object result) {
                                if (loggedInUser.getUserName().equals(userToSave.getUserName())) {
                                    loggedInUser.setRealName(userToSave.getRealName());
                                    loggedInUser.setEmail(userToSave.getEmail());
                                    loggedInUser.setPassword(userToSave.getPassword());
                                    loggedInUser.setTelephoneNumber(userToSave.getTelephoneNumber());
                                    loggedInUser.setAffiliation(userToSave.getAffiliation());
                                    loggedInUser.setProjectDescription(userToSave.getProjectDescription());
                                    loggedInUser.setProjectPIName(userToSave.getProjectPIName());
                                    displayDialog("", (String) result);

                                } else { // admin updating a user
                                    displayBarAndTabs("");
                                    statusMessage.setText( ( String ) result );
                                }
                            }

                            public void onFailure( Throwable caught )
							{
								String m = caught.getMessage();
								displayErrorPage(m);
							}
						}
						);
					}
                }
                else
                {
                    label_box.setText( "Please, fix the errors and resubmit:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

		Button submit_button;
		if (newUser) {
	        if (admin) {
				submit_button = new Button ( "Add user", SignupButtonListener);
			} else {
				submit_button = new Button ( "Sign up", SignupButtonListener);
			}
		} else {
			submit_button = new Button ( "Update Record", SignupButtonListener );
		}

        Button cancel_button = new Button( "Cancel", DefaultPageButtonListener );
        VerticalPanel mpanel = new VerticalPanel();
        mpanel.add( label_mandatory );
        mpanel.add( g1 );

        VerticalPanel opanel = new VerticalPanel();
        if (extra_fields > 0) {
            opanel.add( label_optional );
            opanel.add( g2 );
        }

        HorizontalPanel bpanel = new HorizontalPanel();
        bpanel.add( submit_button );
        bpanel.add( new HTML( "&nbsp;&nbsp;or&nbsp;&nbsp;" ) );
        bpanel.add( cancel_button );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        vpanel.add(new HTML("<br/>"));
        if (logo!=null) { addLogoWithText(vpanel); vpanel.add(new HTML("<br/>"));};
        vpanel.add (label_box);
        vpanel.add (mpanel);
        vpanel.add (opanel);
        vpanel.add (bpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
//        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        parent.clear();
        parent.add( wrapper );
    }

    public void displayPasswordRecoveryPage()
    {
        label_box.setText ("Please, choose the new password");
        label_box.setStyleName("euca-greeting-normal");

        final Grid g1 = new Grid ( 3, 3 );
        g1.getColumnFormatter().setWidth(0, "230");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        final int usernameOrEmail_row = i;
        g1.setWidget( i, 0, new Label( "Username OR email address:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox usernameOrEmail_box = new TextBox();
        usernameOrEmail_box.setWidth("180");
        g1.setWidget( i++, 1, usernameOrEmail_box );

        final int password1_row = i;
        g1.setWidget( i, 0, new Label( "New password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword1_box = new PasswordTextBox();
        cleartextPassword1_box.setWidth("180");
        g1.setWidget( i++, 1, cleartextPassword1_box );

        final int password2_row = i;
        g1.setWidget( i, 0, new Label( "The password, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword2_box = new PasswordTextBox();
        cleartextPassword2_box.setWidth("180");
        g1.setWidget( i++, 1, cleartextPassword2_box );

        ClickListener RecoverButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 3; j++ )
                {
                    g1.clearCell( j, 2 ); /* clear previous right-hand-side annotations */
                }

                /* perform checks */
                if ( usernameOrEmail_box.getText().length() < 1 )
                {
                    Label l = new Label( "Username is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( usernameOrEmail_row, 2, l);
                    formOk = false;
                }
				/* no spaces in username */
				if ( usernameOrEmail_box.getText().matches(".*[ \t]+.*") ) {
					Label l = new Label ("Username cannot have spaces, sorry!");
					l.setStyleName ("euca-error-hint");
					g1.setWidget (usernameOrEmail_row, 2, l);
					formOk = false;
				}

                if ( cleartextPassword1_box.getText().length() < minPasswordLength )
                {
                    Label l = new Label( "Password must be at least " + minPasswordLength + " characters long!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }
                if ( !cleartextPassword1_box.getText().equals( cleartextPassword2_box.getText() ) )
                {
                    Label l = new Label( "Passwords do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password2_row, 2, l );
                    formOk = false;
                }
                if ( cleartextPassword1_box.getText().toLowerCase().matches(".*" +
                        usernameOrEmail_box.getText().toLowerCase() + ".*")) {
                    Label l = new Label ( "Password may not contain the username!");
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");

                    UserInfoWeb user = new UserInfoWeb(
                            usernameOrEmail_box.getText(),
                            "", /* don't care about the name */
                            usernameOrEmail_box.getText(), /* same as login */
                            GWTUtils.md5(cleartextPassword1_box.getText()) );
                    EucalyptusWebBackend.App.getInstance().recoverPassword(
                            user,
                            new AsyncCallback() {
                                public void onSuccess( Object result )
                                {
                                    displayDialog( "Thank you!", ( String ) result );
                                }

                                public void onFailure( Throwable caught )
                                {
                                    String m = caught.getMessage();
                                	displayErrorPage(m);
                                }
                            }
                    );

                }
                else
                {
                    label_box.setText( "Please, fix the errors and resubmit:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

        Button submit_button = new Button ( "Recover Password", RecoverButtonListener );
        Button cancel_button = new Button ( "Cancel", DefaultPageButtonListener );
        VerticalPanel mpanel = new VerticalPanel();
        mpanel.add( g1 );

        HorizontalPanel bpanel = new HorizontalPanel();
        bpanel.add( submit_button );
        bpanel.add( new HTML( "&nbsp;&nbsp;or&nbsp;&nbsp;" ) );
        bpanel.add( cancel_button );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (mpanel);
        vpanel.add (bpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }

    private Button displayDialog ( String greeting, String message )
    {
		return displayDialog (greeting, message, null);
    }

 	private Button displayDialog ( String greeting, String message, Button firstButton )
    {
		if ( message==null || message.equalsIgnoreCase("") ) {
            message = "Server is not accessible!"; // TODO: any other reasons why message would be empty?
        }
        label_box.setText( greeting );
        label_box.setStyleName("euca-greeting-normal");
        Label m = new Label ( message );
        m.setWidth("300");

        VerticalPanel panel = new VerticalPanel();
        panel.add(m);
        panel.setStyleName("euca-login-panel");
        panel.setCellHorizontalAlignment(m, HasHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(m, HasVerticalAlignment.ALIGN_MIDDLE);
        Button ok_button = new Button( "Ok", DefaultPageButtonListener );

		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.setSpacing (10);
		if (firstButton!=null) {
			hpanel.add (firstButton);
		}
		hpanel.add (ok_button);

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (panel);
        vpanel.add (hpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );

        return ok_button;
	}

    public void attemptLogin()
    {
        displayStatusPage("Logging into the server...");
        EucalyptusWebBackend.App.getInstance().getUserRecord(
                sessionId,
                null, /* get user record associated with this sessionId */
                new AsyncCallback() {
					public void onSuccess( Object result )
					{
						loggedInUser = ( UserInfoWeb ) ( (List) result).get(0);
						if ( currentAction == null ) {
							displayDefaultPage ("");
						} else {
							executeAction( currentAction );
						}
					}

					public void onFailure( Throwable caught )
					{
						if (caught.getMessage().equals("Password expired")) {
							displayPasswordChangePage( true );
						} else {
							displayLoginErrorPage( caught.getMessage() );
						}
					}
                }
        );
    }

    public void attemptAction( String action, String param )
    {
        displayStatusPage( "Contacting the server..." );
        EucalyptusWebBackend.App.getInstance().performAction(
                sessionId,
                action,
                param,
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        displayMessagePage( ( String ) result );
                    }

                    public void onFailure( Throwable caught )
                    {
                        displayErrorPage( caught.getMessage() );
                    }
                }
        );
    }

    public void attemptActionNoReload( String action, String param, final VerticalPanel parent )
    {
        statusMessage.setText( "Contacting the server..." );
        EucalyptusWebBackend.App.getInstance().performAction(
                sessionId,
                action,
                param,
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        displayBarAndTabs("");
                        statusMessage.setText( ( String ) result );
                    }

                    public void onFailure( Throwable caught )
                    {
                        displayErrorPage( caught.getMessage() );
                    }
                }
        );
    }

    public void executeAction( String action )
    {
        /* NOTE: some of the checks are repeated by the server,
         * this is just to avoid unnecessary RPCs */
        if ( action.equals ( "approve" )
                || action.equals ( "reject" )
                || action.equals ( "delete" )
                || action.equals ( "disable" )
                || action.equals ( "enable" ) )
        {
            String userName = ( String ) urlParams.get( "user" );
            if ( !loggedInUser.isAdministrator().booleanValue() )
            {
                displayErrorPage( "Administrative privileges required" );
            }
            else if ( userName == null )
            {
                displayErrorPage( "Username not specified" );
            }
            else
            {
                attemptAction( action, userName );
            }
        }
        else if ( action.equals ( "delete_image")
                || action.equals ( "disable_image")
                || action.equals ( "enable_image") )
        {
            String imageId = ( String ) urlParams.get ("id");
            if ( !loggedInUser.isAdministrator().booleanValue() )
            {
                displayErrorPage( "Administrative privileges required" );
            }
            else if ( imageId == null )
            {
                displayErrorPage( "Image ID not specified" );
            }
            else
            {
                attemptAction( action, imageId );
            }
        }
        else if ( action.equals( "confirm" )
 			|| action.equals ("recover") )
        {
            String confirmationCode = ( String ) urlParams.get( "code" );
            if ( confirmationCode == null )
            {
                displayErrorPage( "Confirmation code not specified" );
            }
            else
            {
                attemptAction( action, confirmationCode );
            }
        }
        else
        {
            displayErrorPage( "Action '" + action + "' not recognized" );
        }
    }

    public void displayDefaultPage (String message)
    {
		displayStatusPage("Loading default page...");

        /* If there is an action encoded in the URL, then redirect to
         * a URL without that action, so it is not repeated upon a reload
         * However, reserve the currently selected tab in URL.
         */
        if (currentAction!=null) {
            String extra = "";
            if (currentTabIndex!=0) {
                extra = "?page=" + currentTabIndex;
            }
            GWTUtils.redirect (GWT.getModuleBaseURL() + extra);
        }

        if (loggedInUser!=null) {
            if ( loggedInUser.isAdministrator().booleanValue() )
            {
                if (loggedInUser.getEmail().equalsIgnoreCase( "" ) ) {
                    displayFirstTimeConfiguration();
                } else {
                    displayBarAndTabs(message);
                }
            }
            else
            {
                displayBarAndTabs(message);
            }
        } else {
            displayLoginPage();
        }
    }

    ClickListener AddUserButtonListener = new ClickListener() {
        public void onClick( Widget sender )
        {
            displayUserRecordPage (RootPanel.get(), null);
        }
    };

    ClickListener DefaultPageButtonListener = new ClickListener() {
        public void onClick( Widget sender )
        {
            displayDefaultPage ("");
        }
    };

    public void displayErrorPage( String message )
    {
        displayDialog("Error!", message);
        label_box.setStyleName("euca-greeting-error");
    }

    public void displayErrorPageFinal( String message )
    {
        Button ok_button = displayDialog("Error!", message);
        label_box.setStyleName("euca-greeting-error");
        ok_button.setVisible(false);
    }

    public void displayMessagePage( String message )
    {
        displayDialog("", message);
        label_box.setStyleName("euca-greeting-normal");
    }

    ClickListener LogoutButtonListener = new ClickListener() {
        public void onClick( Widget sender )
        {
            EucalyptusWebBackend.App.getInstance().logoutSession(
                    sessionId,
                    new AsyncCallback() {
                        public void onSuccess( Object result )
                        {
                            displayLoginPage();
                        }

                        public void onFailure( Throwable caught )
                        {
                            displayLoginPage();
                        }
                    }
            );
            sessionId = null; /* invalidate sessionId */
            loggedInUser = null; /* invalidate user */
            Cookies.removeCookie( cookie_name ); // TODO: this isn't working for some reason
        }
    };

    public void displayBarAndTabs(String message)
    {
        /* top bar */
		displayStatusPage("Drawing the tabs...");

        HorizontalPanel top_bar = new HorizontalPanel();
        top_bar.getElement().setClassName("euca-top-bar");

		HorizontalPanel name_panel = new HorizontalPanel();
        name_panel.getElement().setClassName("euca-top-bar-left");
		name_panel.setSpacing (5);
        name_panel.add (new HTML(Theme.draw_header()));
		top_bar.add (name_panel);
		
        top_bar.setCellHorizontalAlignment(name_panel, HorizontalPanel.ALIGN_LEFT);
        top_bar.setCellVerticalAlignment(name_panel, HorizontalPanel.ALIGN_TOP); // michael left this as MIDDLE

        HorizontalPanel upanel = new HorizontalPanel();
        upanel.getElement().setClassName("euca-top-bar-right");
        Label user_name = new HTML("Logged in as <b>"
                + loggedInUser.getUserName()
                + "</b>&nbsp;&nbsp;|&nbsp;&nbsp;");
        Hyperlink logout_button = new Hyperlink("Logout", "logout");
        logout_button.addClickListener(LogoutButtonListener);
        upanel.add(user_name);
        upanel.add(logout_button);
        top_bar.add(upanel);
        top_bar.setCellHorizontalAlignment(upanel, HorizontalPanel.ALIGN_RIGHT);
        top_bar.setCellVerticalAlignment(upanel, HorizontalPanel.ALIGN_TOP);

        final HorizontalPanel messageBox = new HorizontalPanel();
        messageBox.setStyleName("euca-message-bar");
        messageBox.setSpacing(3);
        messageBox.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        messageBox.add (statusMessage);
        statusMessage.setStyleName("euca-small-text");

        final VerticalPanel wrapper = new VerticalPanel();
        wrapper.setStyleName("euca-tab-contents");
//        wrapper.setSize("100%", "80%");
//        wrapper.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
//        wrapper.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

		// set up tab layout so that *TabIndex variables are set in the beginning
		int nTabs = 0;
        allTabs = new TabBar();
        
        
        allTabs.addTab ("Credentials"); credsTabIndex = nTabs++;
        allTabs.addTab ("Images"); imgTabIndex = nTabs++;
        /////allTabs.addTab ("Instances"); instTabIndex = nTabs++;
        if (loggedInUser.isAdministrator().booleanValue()) {
			allTabs.addTab ("Users"); usrTabIndex = nTabs++;
			allTabs.addTab ("Configuration"); confTabIndex = nTabs++;
            allTabs.addTab ("Extras"); downTabIndex = nTabs++;
            if (extensions!=null && extensions.contains ("store") ) {
                allTabs.addTab ("Store"); storeTabIndex = nTabs++;
            }
        }
		// can happen if admin user re-logs in as regular without reloading
		if (currentTabIndex > (nTabs-1) ) {
			currentTabIndex = 0;
		}
		allTabs.addTabListener(new TabListener() {
            public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
                String error = "This tab is not implemented yet, sorry!";
                statusMessage.setText("");
                wrapper.clear();
                currentTabIndex = tabIndex;
                if (tabIndex==credsTabIndex) { displayCredentialsTab(wrapper); }
                else if (tabIndex==imgTabIndex) { displayImagesTab(wrapper); }
                else if (tabIndex==usrTabIndex) { displayUsersTab(wrapper); }
				else if (tabIndex==confTabIndex) { displayConfTab(wrapper); }
                else if (tabIndex==downTabIndex) { displayDownloadsTab(wrapper); }
                else if (tabIndex==storeTabIndex) { displayStoreTab(wrapper); }
                else { displayErrorPage("Invalid tab!"); }
            }
            public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
                return true; /* here we could do checking for clicks on disabled tabs */
            }
        });

        RootPanel.get().clear();
        RootPanel.get().add( top_bar );
        RootPanel.get().add( allTabs );
        RootPanel.get().add( messageBox );
        RootPanel.get().add( wrapper );
        allTabs.selectTab(currentTabIndex);
    }

	private Image addLogoWithText(CellPanel name_panel) {
		logo.getElement().setAttribute("alt", cloud_name);
		name_panel.add(logo);
		return logo;
	}

	private void addTextlessLogoAndHeading(HorizontalPanel name_panel) {
		Label welcome = new Label( cloud_name);
		if (textless_logo!=null) {
			name_panel.add (textless_logo);
		}
		name_panel.add(welcome);
		name_panel.setCellVerticalAlignment(welcome, HorizontalPanel.ALIGN_MIDDLE);
	}

	public void displayCredentialsTab (final VerticalPanel parent)
	{
		EucalyptusWebBackend.App.getInstance().getCloudInfo(
			sessionId,
			false, // do not check external IP for now
		new AsyncCallback() {
			public void onSuccess( Object result )
			{
				cloudInfo = ( CloudInfoWeb ) result;
				actuallyDisplayCredentialsTab (parent);
			}
			public void onFailure( Throwable caught )
			{
				displayErrorPage( caught.getMessage() );
			}
		}
		);
	}

	private class RightscaleDialog extends DialogBox {

		private boolean cancelled;

		public RightscaleDialog () {

			cancelled = false;
			setHTML ("<img src=\"themes/share/pending-FFCC33.gif\" align=\"middle\"> &nbsp; Checking the external IP address...");

			final Button okButton = new Button("OK",
			new ClickListener() {
				public void onClick(Widget sender) {
					RightscaleDialog.this.hide();
					if (rightscaleUrl!=null) {
						Window.open (rightscaleUrl, "_blank", "");
					}
				}
			});
			okButton.setEnabled (false);
			Button cancelButton = new Button("Cancel",
			new ClickListener() {
				public void onClick(Widget sender) {
					RightscaleDialog.this.hide();
					cancelled = true;
				}
			});

			HorizontalPanel buttonPanel = new HorizontalPanel();
			buttonPanel.add (okButton);
			buttonPanel.add (cancelButton);
			setWidget (buttonPanel);

			EucalyptusWebBackend.App.getInstance().getCloudInfo(
				sessionId,
				true, // DO check external IP
				new AsyncCallback () {
					public void onSuccess( Object result )
					{
						if (cancelled) {
							return;
						}
						cloudInfo = ( CloudInfoWeb ) result;
						String ex = cloudInfo.getExternalHostPort();
						String in = cloudInfo.getInternalHostPort();
						String text = "";
						String ip;

						if (ex==null) {
							ip = in;
							text = "<b>Warning:</b> Rightscale could not discover the external IP address of your cloud.  Hence, the pre-filled cloud URL <i>may</i> be incorrect.  Check your firewall settings.</p> ";

						} else if (!ex.equals(in)) {
							ip = ex;
							text = "<b>Warning:</b> The external cloud IP discovered by Rightscale (" + ex + ") is different from the IP found by Eucalyptus (" + in + ").  Hence, the pre-filled cloud URL <i>may</i> be incorrect.  Check your firewall settings.</p> ";

						} else {
							ip = ex;
						}
						String callbackUrl = "https://"
							+ ip
							+ cloudInfo.getServicePath();
						rightscaleUrl = rightscale_base_url
							+ GWTUtils.escape (callbackUrl) // URL.encode() wasn't quite right
							+ "&registration_version=1.0&retry=1&secret_token="
							+ GWTUtils.escape (cloudInfo.getCloudId());
						String pre = "<h3>Cloud registration</h3> You are about to open a new window to Rightscale's Web site, on which you will be able to complete registraton. </p> ";
						setHTML (pre + text);
						okButton.setEnabled (true);
						center();
					}
					public void onFailure( Throwable caught )
					{
						displayErrorPage( caught.getMessage() );
					}
				}
			);
		}
	}

	public void actuallyDisplayCredentialsTab (VerticalPanel parent)
	{
        History.newItem("credentials");

        VerticalPanel credspanel = new VerticalPanel ();
        credspanel.setSpacing(5);
        parent.add (credspanel);
        credspanel.getElement().setClassName ("euca-tab-with-text");

		VerticalPanel ppanel = new VerticalPanel();
		ppanel.add (new HTML ("<h3>User account Information</h3>"));
		
		Grid userGrid = new Grid(3,2);
		userGrid.setText(0, 0, "Login:");
		userGrid.setHTML(0, 1, "<b>" + loggedInUser.getUserName() +"</b>");
		userGrid.setText(1, 0, "Name:");
		userGrid.setHTML(1, 1, "<b>" + loggedInUser.getRealName() +"</b>");
		userGrid.setText(2, 0, "Email:");
		userGrid.setHTML(2, 1, "<b>" + loggedInUser.getEmail() +"</b>");
		ppanel.add(userGrid);
		ppanel.add (new HTML ("<p>" + user_account_text + "</p>"));
		ppanel.setStyleName("euca-text");
		ppanel.addStyleName("content");
		
        Button passwordButton = new Button ( "Change Password",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        displayPasswordChangePage (false);
                    }
                });
		Button editButton = new Button ( "Edit Account Information",
			new ClickListener() {
					public void onClick (Widget sender) {
						displayUserRecordPage (RootPanel.get(), loggedInUser);
					}
			});
        ppanel.setSpacing( 0 );
		ppanel.add(editButton);
        ppanel.add(passwordButton);

		VerticalPanel cpanel = new VerticalPanel();
		cpanel.add ( new HTML (certificate_download_text) );
        cpanel.setStyleName( "euca-text" );
        cpanel.addStyleName("content");
		Button certButton = new Button ("Download Credentials", new ClickListener() {
				public void onClick (Widget sender) {
					Window.open(GWT.getModuleBaseURL() +
						"getX509?user=" + loggedInUser.getUserName() +
						"&keyValue=" + loggedInUser.getUserName() +
						"&code=" + loggedInUser.getToken(),
						"_self", "");
				}
		});

        VerticalPanel rpanel = new VerticalPanel();
		rpanel.setSpacing (0);
        HTML credentials_html = new HTML (rest_credentials_text);
        credentials_html.setStyleName("content");
		rpanel.add( credentials_html );
		Grid g0 = new Grid (2, 2);
		g0.setWidget (0, 0, new HTML ("<b><font size=\"2\">Query ID:</font></b>"));
		final HTML queryId = new HTML ("<font color=#666666 size=\"2\">" + loggedInUser.getQueryId() + "</font>");
		queryId.setVisible (false);
		g0.setWidget (0, 1, queryId);
		g0.setWidget (1, 0, new HTML ("<b><font size=\"2\">Secret Key:</font></b>"));
		final HTML secretKey = new HTML ("<font color=#666666 size=\"2\">" + loggedInUser.getSecretKey() + "</font>");
		secretKey.setVisible (false);
		g0.setWidget (1, 1, secretKey);
		rpanel.add (g0);
        rpanel.setStyleName( "euca-text" );
		final Button secretButton = new Button ( "Show keys" );
		secretButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                if (secretKey.isVisible()) {
					secretKey.setVisible(false);
					queryId.setVisible(false);
					secretButton.setText ("Show keys");
				} else {
					secretKey.setVisible(true);
					queryId.setVisible(true);
					secretButton.setText ("Hide keys");
				}
            }
        });

		int gridRows = 3;
		if (loggedInUser.isAdministrator() && show_cloud_registration) {
			gridRows++;
		}
		credspanel.add(ppanel);
		
		credspanel.add(cpanel);
		credspanel.add(certButton);
		credspanel.add(rpanel);
		credspanel.add(secretButton);
		
//        final Grid g = new Grid( gridRows, 2 );
//        g.getColumnFormatter().setWidth(0, "400");
//        g.getColumnFormatter().setWidth(1, "200");
//        g.setCellSpacing( 30 );
//
//        g.setWidget( 0, 0, ppanel ); g.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);
//       // g.setWidget( 0, 1, ppanel2); g.getCellFormatter().setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_TOP);
//
//        g.setWidget( 1, 0, cpanel ); g.getCellFormatter().setVerticalAlignment(1, 0, HasVerticalAlignment.ALIGN_TOP);
//        g.setWidget( 1, 1, certButton ); g.getCellFormatter().setVerticalAlignment(1, 1, HasVerticalAlignment.ALIGN_TOP);
//
//        g.setWidget( 2, 0, rpanel ); g.getCellFormatter().setVerticalAlignment(2, 0, HasVerticalAlignment.ALIGN_TOP);
//		g.setWidget( 2, 1, secretButton ); g.getCellFormatter().setVerticalAlignment(2, 1, HasVerticalAlignment.ALIGN_TOP);

		if (loggedInUser.isAdministrator() && show_cloud_registration) {
	        VerticalPanel cloud_panel = new VerticalPanel();
			cloud_panel.setSpacing (0);
	        cloud_panel.add( new HTML (cloud_registration_text) );
			Grid g1 = new Grid (2, 2);
			g1.setWidget (0, 0, new HTML ("<b><font size=\"2\">Cloud URL:</font></b>"));
			final HTML cloudUrl = new HTML ("<font color=#666666 size=\"2\">https://"
				+ cloudInfo.getInternalHostPort()
				+ cloudInfo.getServicePath()
				+ "</font>");
			g1.setWidget (0, 1, cloudUrl);
			g1.setWidget (1, 0, new HTML ("<b><font size=\"2\">Cloud ID:</font></b>"));
			final HTML cloudId = new HTML ("<font color=#666666 size=\"2\">"
			 	+ cloudInfo.getCloudId()
				+ "</font>");
			g1.setWidget (1, 1, cloudId);
			cloud_panel.add (g1);
	        cloud_panel.setStyleName( "euca-text" );
			final Button cloudButton = new Button ( "Register" );
			cloudButton.addClickListener(new ClickListener() {
	            public void onClick(Widget sender) {
					new RightscaleDialog().center();
				}
	        });
//			g.setWidget (3, 0, cloud_panel );
//			g.getCellFormatter().setVerticalAlignment(3, 0, HasVerticalAlignment.ALIGN_TOP);
			credspanel.add(cloud_panel);
			VerticalPanel vp = new VerticalPanel();
			vp.setSpacing (0);
			HorizontalPanel hp = new HorizontalPanel();
			hp.setSpacing (0);
			hp.add (cloudButton);
			hp.add (new HTML ("with"));
			Image logo = new Image ("themes/share/rightscale-logo-blue.gif");
			logo.setStyleName("euca-inline-image");
			hp.add (logo);
			vp.add (hp);
//			g.setWidget( 3, 1, vp );
//			g.getCellFormatter().setVerticalAlignment(3, 1, HasVerticalAlignment.ALIGN_TOP);
			credspanel.add(vp);
		}

        //credspanel.add(g);
    }

	public void displayErrorTab(VerticalPanel parent, String message)
    {
        parent.add(new Label(message));
    }

    public void displayTestingTab(VerticalPanel parent)
    {
        parent.add(new Label("truth = [" + props.get("truth") + "]"));
    }

    public void displayStatusPage(String message)
    {
        label_box.setText( message );
        label_box.setStyleName("euca-greeting-pending");

        final VerticalPanel wrapper = new VerticalPanel();
        wrapper.setSize("100%", "100%");
        wrapper.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        wrapper.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        wrapper.add(label_box);

        RootPanel.get().clear();
        RootPanel.get().add(wrapper);
    }

    public void displayPasswordChangePage(boolean mustChange)
    {
        if (mustChange) {
            label_box.setText( "You are required to change your password" );
            label_box.setStyleName("euca-greeting-error");
        } else {
            label_box.setText( "Please, change your password" );
            label_box.setStyleName("euca-greeting-normal");
        }
        final Grid g1 = new Grid ( 3, 3 );
        g1.getColumnFormatter().setWidth(0, "180");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        final int oldPassword_row = i;
        g1.setWidget( i, 0, new Label( "Old password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox oldPassword_box = new PasswordTextBox();
        oldPassword_box.setWidth("180");
        if (!mustChange) { // don't ask for old password if the change is involuntary
            g1.setWidget( i++, 1, oldPassword_box );
        }

        final int newPassword1_row = i;
        g1.setWidget( i, 0, new Label( "New password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox newCleartextPassword1_box = new PasswordTextBox();
        newCleartextPassword1_box.setWidth("180");
        g1.setWidget( i++, 1, newCleartextPassword1_box );

        final int newPassword2_row = i;
        g1.setWidget( i, 0, new Label( "New password, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox newCleartextPassword2_box = new PasswordTextBox();
        newCleartextPassword2_box.setWidth("180");
        g1.setWidget( i++, 1, newCleartextPassword2_box );

        ClickListener ChangeButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 3; j++ )
                {
                    g1.clearCell( j, 2 ); // clear previous right-hand-side annotations
                }

                // perform checks 
                if ( newCleartextPassword1_box.getText().length() < minPasswordLength )
                {
                    Label l = new Label( "Password is too short!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( newPassword1_row, 2, l );
                    formOk = false;
                }
                if ( !newCleartextPassword1_box.getText().equals( newCleartextPassword2_box.getText() ) )
                {
                    Label l = new Label( "Passwords do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( newPassword2_row, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");

                    EucalyptusWebBackend.App.getInstance().changePassword(
                            sessionId,
                            GWTUtils.md5(oldPassword_box.getText()),
                            GWTUtils.md5(newCleartextPassword1_box.getText()),
                            new AsyncCallback<String>() {
                                public void onSuccess( final String result )
                                {
                                    // password change succeded - pull in the new user record 
                                    label_box.setText( "Refreshing user data..." );
                                    EucalyptusWebBackend.App.getInstance().getUserRecord(
                                            sessionId,
                                            null,
                                            new AsyncCallback<List<UserInfoWeb>>() {
                                                public void onSuccess( List<UserInfoWeb> users )
                                                {
                                                    loggedInUser = users.get(0);
													displayMessagePage ( ( String ) result );
                                                }
                                                public void onFailure( Throwable caught )
                                                {
                                                    displayLoginErrorPage( caught.getMessage() );
                                                }
                                            });
                                }
                                public void onFailure( Throwable caught )
                                {
                                    String m = caught.getMessage();
                                    label_box.setText( m );
                                    label_box.setStyleName("euca-greeting-warning");
                                }
                            }
                    );
                }
                else
                {
                    label_box.setText( "Please, fix the errors and try again:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

        Button change_button = new Button( "Change password", ChangeButtonListener );
        Button cancel_button = new Button( "Cancel", DefaultPageButtonListener );

        HorizontalPanel bpanel = new HorizontalPanel();
        bpanel.add( change_button );
        if (!mustChange) {
            bpanel.add( new HTML( "&nbsp;&nbsp;or&nbsp;&nbsp;" ) );
            bpanel.add( cancel_button );
        }

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (g1);
        vpanel.add (bpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }


    public void displayAdminEmailChangePage()
    {
        label_box.setText( "One more thing!" );
        label_box.setStyleName("euca-greeting-error");

        final Grid g1 = new Grid ( 2, 3 );
        g1.getColumnFormatter().setWidth(0, "180");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        g1.setWidget( i, 0, new Label( "Email address:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox emailAddress1_box = new TextBox();
        emailAddress1_box.setWidth("180");
        g1.setWidget( i++, 1, emailAddress1_box );

        g1.setWidget( i, 0, new Label( "The address, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox emailAddress2_box = new TextBox();
        emailAddress2_box.setWidth("180");
        g1.setWidget( i++, 1, emailAddress2_box );

        ClickListener ChangeButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 2; j++ )
                {
                    g1.clearCell( j, 2 ); /* clear previous right-hand-side annotations */
                }

                /* perform checks */
                if ( emailAddress1_box.getText().length() < 3 )
                {
                    Label l = new Label( "Invalid address!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( 0, 2, l );
                    formOk = false;
                }
                if ( !emailAddress1_box.getText().equals( emailAddress2_box.getText() ) )
                {
                    Label l = new Label( "Addresses do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( 1, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    loggedInUser.setEmail( emailAddress1_box.getText() );
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");
                    EucalyptusWebBackend.App.getInstance().updateUserRecord(
                            sessionId,
                            loggedInUser,
                            new AsyncCallback() {
                                public void onSuccess( final Object result )
                                {
                                    displayWalrusURLChangePage ();
                                }
                                public void onFailure( Throwable caught )
                                {
                                    loggedInUser.setEmail( "" );
                                    displayLoginErrorPage( caught.getMessage() );
                                }
                            });
                }
                else
                {
                    label_box.setText( "Please, fix the errors and try again:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

        Button change_button = new Button( "Change address", ChangeButtonListener );
        HTML message = new HTML (admin_email_change_text);
        message.setWidth( "460" );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
//        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (message);
        vpanel.add (g1);
        vpanel.add (change_button);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER); // michael commented out
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }

	public static SystemConfigWeb conf = new SystemConfigWeb ();
    public void displayWalrusURLChangePage()
    {
        label_box.setText( "One last thing!  Really!!!" );
        label_box.setStyleName("euca-greeting-error");

		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.add (new Label ("Walrus URL:"));
		final TextBox box = new TextBox ();
		box.setVisibleLength (55);
		hpanel.add (box);

		EucalyptusWebBackend.App.getInstance().getSystemConfig(sessionId,
			new AsyncCallback( ) {
				public void onSuccess ( final Object result ) {
					conf = (SystemConfigWeb) result;
					box.setText (conf.getCloudHost());
				}
				public void onFailure ( Throwable caught ) { }
			}
		);

        Button change_button = new Button( "Confirm URL",
 			new ClickListener() {
	            public void onClick( Widget sender )
	            {
					conf.setCloudHost(box.getText());
					EucalyptusWebBackend.App.getInstance().setSystemConfig(sessionId,
						conf,
						new AsyncCallback() {
							public void onSuccess ( final Object result ) {
								currentTabIndex = 3; // TODO: change this to confTabIndex
								displayDefaultPage ("");
							}
							public void onFailure ( Throwable caught ) {
								displayErrorPage ("Failed to save the URL (check 'Configuration' tab).");
							}
						}
					);
				}
			}
		);
        HTML message = new HTML (admin_cloud_ip_setup_text);
        message.setWidth( "460" );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (message);
        vpanel.add (hpanel);
        vpanel.add (change_button);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

		RootPanel.get().clear();
		RootPanel.get().add (wrapper);
	}

    public void displayUsersTab(final VerticalPanel parent)
    {
        History.newItem("users");
        final HTML msg = new HTML("Contacting the server...");
        EucalyptusWebBackend.App.getInstance().getUserRecord(
                sessionId,
                "*", /* we want all user records */
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        List usersList = (List)result;
                        displayUsersList (usersList, parent);
                    }
                    public void onFailure( Throwable caught )
                    {
                        displayErrorPage (caught.getMessage());
                    }
                });

        parent.add(msg);
    }

    public void displayImagesTab (final VerticalPanel parent)
    {
        History.newItem("images");
        final HTML msg = new HTML ("Contacting the server...");
        EucalyptusWebBackend.App.getInstance().getImageInfo(
                sessionId,
                loggedInUser.getUserName(),
                new AsyncCallback() {
                    public void onSuccess (Object result)
                    {
                        List imagesList = (List)result;
                        displayImagesList (imagesList, parent);
                    }
                    public void onFailure (Throwable caught)
                    {
                        displayErrorPage (caught.getMessage());
                    }
                });

        parent.add(msg);
    }

    private class EucalyptusDialog extends DialogBox {

		private boolean cancelled;

		public EucalyptusDialog (String mainMsg, String extraMsg, Button okButton) {

            super(true);
            cancelled = false;
			setHTML (mainMsg);
            Button cancelButton = new Button ("Cancel", new ClickListener() {
                	public void onClick(Widget sender) {
					EucalyptusDialog.this.hide();
					cancelled = true;
				}
            });
			HorizontalPanel buttonPanel = new HorizontalPanel();
			buttonPanel.add (okButton);
			buttonPanel.add (cancelButton);
			setWidget (buttonPanel);
            center();
		}
	}

	public void displayConfirmDeletePage( final String userName )
    {
		Button deleteButton = new Button ("Delete", new ClickListener() {
            public void onClick(Widget sender) {
				GWTUtils.redirect (GWT.getModuleBaseURL()
					+ "?action=delete"
					+ "&user=" + userName
					+ "&page=" + currentTabIndex);
			}
        });

		Button okButton = displayDialog ("Sure?",
			"Do you want to delete user '" + userName + "'?", deleteButton);
		okButton.setText ("Cancel");
		label_box.setStyleName("euca-greeting-warning");
    }

    public void displayConfirmDeletePageNoReload ( final String userName, final VerticalPanel parent )
    {
		Button deleteButton = new Button ("Delete", new ClickListener() {
            public void onClick(Widget sender) {
                attemptActionNoReload("delete", userName, parent);
			}
        });

		Button okButton = displayDialog ("Sure?",
			"Do you want to delete user '" + userName + "'?", deleteButton);
		okButton.setText ("Cancel");
		label_box.setStyleName("euca-greeting-warning");
    }

    private HTML userActionButton (String action, UserInfoWeb user)
    {
        return new HTML ("<a class=\"euca-action-link\" href=\"" + GWT.getModuleBaseURL()
                + "?action=" + action.toLowerCase()
                + "&user=" + user.getUserName()
                + "&page=" + currentTabIndex
                + "\">" + action + "</a>");
    }

    private HTML userActionButtonNoReload (final String action, final UserInfoWeb user, final VerticalPanel parent)
    {
        HTML link = new HTML (action);
        link.setStyleName("euca-action-link");
        link.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                attemptActionNoReload(action.toLowerCase(), user.getUserName(), parent);
            }
        });
        return link;
    }

	class EditCallback implements ClickListener {

		private EucalyptusWebInterface parent;
		private UserInfoWeb u;

		EditCallback ( final EucalyptusWebInterface parent, UserInfoWeb u )
		{
			this.parent = parent;
			this.u = u;
		}

		public void onClick( final Widget widget )
		{
			displayUserRecordPage (RootPanel.get(), u);
		}
	}

    public void displayUsersList(final List usersList, final VerticalPanel parent)
    {
        String sortSymbol = "&darr;"; // HTML down arrow
        if (sortUsersLastFirst) {
            sortSymbol = "&uarr;"; // HTML up arrow
        }
        parent.clear();
        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(5);
        parent.add(new HTML("<h3>Users</h3>"));
        parent.add(vpanel);
        int nusers = usersList.size();
        if (nusers>0) {
            Anchor sort_button = new Anchor( sortSymbol, true);
			sort_button.setStyleName ("euca-small-text");
			sort_button.addClickListener( new ClickListener() {
				public void onClick(Widget sender) {
                    sortUsersLastFirst = !sortUsersLastFirst;
					displayUsersList (usersList, parent);
				}
			});

            final Grid g = new Grid(nusers + 1, 6);
            g.setStyleName("euca-table");
            g.setCellPadding(6);
            g.setWidget(0, 0, sort_button);
            g.setWidget(0, 1, new Label("Username"));
            g.setWidget(0, 2, new Label("Email"));
            g.setWidget(0, 3, new Label("Name"));
            g.setWidget(0, 4, new Label("Status"));
            g.setWidget(0, 5, new Label("Actions"));
            g.getRowFormatter().setStyleName(0, "euca-table-heading-row");

            for (int i=0; i<nusers; i++) {
                int userIndex = i;
                if (sortUsersLastFirst) {
                    userIndex = nusers - i - 1;
                }
                final UserInfoWeb u = (UserInfoWeb) usersList.get(userIndex);
                int row = i+1;
                if ((row%2)==1) {
                    g.getRowFormatter().setStyleName(row, "euca-table-odd-row");
                } else {
                    g.getRowFormatter().setStyleName(row, "euca-table-even-row");
                }
				Label indexLabel = new Label(Integer.toString(userIndex));
				indexLabel.setStyleName("euca-small-text");
				g.setWidget(row, 0, indexLabel);
				Label userLabel = new Label(u.getUserName());
                g.setWidget(row, 1, userLabel);
				Label emailLabel = new Label(u.getEmail());
                g.setWidget(row, 2, emailLabel);
				Label nameLabel = new Label(u.getRealName());
                g.setWidget(row, 3, nameLabel);
                String status;
                if (!u.isApproved().booleanValue()) {
                    status = "unapproved";
                } else if (!u.isEnabled().booleanValue()) {
                    status = "disabled";
                } else if (!u.isConfirmed().booleanValue()) {
                    status = "unconfirmed";
                } else {
                    status = "active";
                }
                if (u.isAdministrator().booleanValue()) {
                     status += " & admin";
                }
                g.setWidget(row, 4, new Label(status) );

                // actions
                HorizontalPanel ops = new HorizontalPanel();
                ops.setSpacing (3);

				Label editLabel = new Label ("Edit");
				editLabel.addClickListener (new EditCallback(this, u));
				editLabel.setStyleName ("euca-action-link");
				ops.add(editLabel);

				// admin can't be disabled or deleted (that breaks things)
				if (!u.isAdministrator().booleanValue()) {
					HTML act_button = userActionButtonNoReload ("Disable", u, parent);
	                if (!u.isApproved().booleanValue()) {
	                    act_button = userActionButtonNoReload ("Approve", u, parent);
	                } else if (!u.isEnabled().booleanValue()) {
	                    act_button = userActionButtonNoReload ("Enable", u, parent);
	                }
	                ops.add(act_button);

					Anchor del_button = new Anchor ( "Delete" );
					del_button.setStyleName ("euca-action-link");
					del_button.addClickListener( new ClickListener() {
						public void onClick(Widget sender) {
							displayConfirmDeletePageNoReload (u.getUserName(), parent);
						}
					});
					ops.add(del_button);
				}

				g.setWidget(row, 5, ops );

                // view
                HorizontalPanel views = new HorizontalPanel();
                views.setSpacing (3);
                HTML inst_button = userActionButton ("Instances", u);
                views.add(inst_button);
                HTML img_button = userActionButton ("Images", u);
                views.add(img_button);
                //g.setWidget(row, 5, views); TODO: implement 'views'
            }
            vpanel.add(g);
        } else {
            vpanel.add(new Label("No users found"));
        }
        vpanel.add(new Button ("Add user", AddUserButtonListener));
    }

    private HTML imageActionButton (String action, ImageInfoWeb img)
    {
        return new HTML ("<a class=\"euca-action-link\" href=\"" + GWT.getModuleBaseURL()
                + "?action=" + action.toLowerCase() + "_image"
                + "&id=" + img.getImageId()
                + "&page=" + currentTabIndex
                + "\">" + action + "</a>");
    }

    public void displayImagesList(List imagesList, final VerticalPanel parent)
    {
        parent.clear();
        int nimages = imagesList.size();
        boolean showActions = false;
        if (loggedInUser.isAdministrator().booleanValue()) {
            showActions = true;
        }

        if (nimages>0) {
            final Grid g = new Grid(nimages + 1, 6);
            g.setStyleName("euca-table");
            g.setCellPadding(6);
            g.setWidget(0, 0, new Label("Id"));
            g.setWidget(0, 1, new Label("Name"));
            g.setWidget(0, 2, new Label("Kernel"));
            g.setWidget(0, 3, new Label("Ramdisk"));
            g.setWidget(0, 4, new Label("State"));
            if ( showActions )
                g.setWidget(0, 5, new Label("Actions"));
            g.getRowFormatter().setStyleName(0, "euca-table-heading-row");

            for (int i=0; i<nimages; i++) {
                ImageInfoWeb img = (ImageInfoWeb) imagesList.get(i);
                int row = i+1;
                if ((row%2)==1) {
                    g.getRowFormatter().setStyleName(row, "euca-table-odd-row");
                } else {
                    g.getRowFormatter().setStyleName(row, "euca-table-even-row");
                }
                g.setWidget(row, 0, new Label (img.getImageId()) );
                g.setWidget(row, 1, new Label (img.getImageLocation()) );
                g.setWidget(row, 2, new Label (img.getKernelId()) );
                g.setWidget(row, 3, new Label (img.getRamdiskId()) );
                g.setWidget(row, 4, new Label (img.getImageState()));
                if ( showActions ) {
                    HorizontalPanel ops = new HorizontalPanel();
                    ops.setSpacing (3);
                    HTML act_button = imageActionButton ("Disable", img);
                    if (img.getImageState().equalsIgnoreCase("deregistered")) {
                        act_button = imageActionButton ("Enable", img);
                    }
                    ops.add(act_button);
                    // TODO: uncomment when deletion is implemented
                    //HTML del_button = imageActionButton ("Delete", img);
                    //ops.add(del_button);
                    g.setWidget(row, 5, ops );
                }
            }
            parent.add(g);
        } else {
            parent.add(new Label("No images found"));
        }
    }

    public void displayConfTab (final VerticalPanel parent)
    {
		History.newItem("conf");
        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        //vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER); // michael had this commented out
		vpanel.add (new SystemConfigTable (sessionId));
		vpanel.add (new WalrusInfoTable (sessionId));
		vpanel.add (new ClusterInfoTable (sessionId));
		vpanel.add (new VmTypeTable (sessionId));

		parent.clear();
		parent.add (vpanel);
	}

    public void displayDownloadsTab (final VerticalPanel parent)
    {
		History.newItem("extras");
        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        //vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        
		vpanel.add (new DownloadsTable(sessionId,
                "http://www.eucalyptussoftware.com/downloads/eucalyptus-images/list.php?version=" + URL.encode(version),
                "http://open.eucalyptus.com/wiki/EucalyptusUserImageCreatorGuide_v1.6",
                "Eucalyptus-certified Images",
                50));
        vpanel.add (new DownloadsTable(sessionId,
                "http://www.eucalyptussoftware.com/downloads/eucalyptus-tools/list.php?version=" + URL.encode(version),
                "http://open.eucalyptus.com/wiki/ToolsEcosystem",
                "Eucalyptus-compatible Tools",
                50));

		parent.clear();
		parent.add (vpanel);
	}

    public void displayStoreTab (final VerticalPanel parent) 
    {
        History.newItem ("store");
        ImageStoreClient imageStoreClient = new ImageStoreClient(sessionId);
        ImageStoreWidget imageStore = new ImageStoreWidget(imageStoreClient);
        parent.clear();
        parent.add(imageStore);
    }
        
    public void displayFirstTimeConfiguration()
    {
    	displayStatusPage("Loading first-time configuration page...");

    	VerticalPanel gpanel = new VerticalPanel();
    	gpanel.setSpacing(25);
    	gpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    	// password grid 

    	final Grid g1 = new Grid ( 2, 3 );
    	g1.getColumnFormatter().setWidth(0, "240");
    	g1.getColumnFormatter().setWidth(1, "180");
    	g1.getColumnFormatter().setWidth(2, "180");
    	int i = 0;

    	final int newPassword1_row = i;
    	g1.setWidget( i, 0, new Label( "Administrator's new password:" ) );
    	g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
    	final PasswordTextBox newCleartextPassword1_box = new PasswordTextBox();
    	newCleartextPassword1_box.setFocus(true); // this box gets focus first
    	newCleartextPassword1_box.setWidth("180");
    	g1.setWidget( i++, 1, newCleartextPassword1_box );

    	final int newPassword2_row = i;
    	g1.setWidget( i, 0, new Label( "The password, again:" ) );
    	g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
    	final PasswordTextBox newCleartextPassword2_box = new PasswordTextBox();
    	newCleartextPassword2_box.setWidth("180");
    	g1.setWidget( i++, 1, newCleartextPassword2_box );

    	gpanel.add(g1);

    	// email address grid

    	final Grid g2 = new Grid ( 1, 3 );
    	g2.getColumnFormatter().setWidth(0, "240");
    	g2.getColumnFormatter().setWidth(1, "180");
    	g2.getColumnFormatter().setWidth(2, "180");
    	i = 0;

    	final int emailAddress_row = i;
    	g2.setWidget( i, 0, new Label( "Administrator's email address:" ) );
    	g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);       
    	final TextBox emailAddress_box = new TextBox();
    	emailAddress_box.setWidth("180");
    	g2.setWidget( i++, 1, emailAddress_box );

    	VerticalPanel epanel = new VerticalPanel();
    	epanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    	epanel.setSpacing(5);
    	epanel.add(g2);
    	HTML emailChangeMsg = new HTML( admin_email_change_text );
    	emailChangeMsg.setWidth ("300");
    	emailChangeMsg.setStyleName("euca-small-text");
    	epanel.add(emailChangeMsg);
    	gpanel.add(epanel);

    	// cloud URL grid

    	final Grid g3 = new Grid ( 1, 3 );
    	g3.getColumnFormatter().setWidth(0, "240");
    	g3.getColumnFormatter().setWidth(1, "180");
    	g3.getColumnFormatter().setWidth(2, "180");
    	i = 0;

    	final int cloudUrl_row = i;
    	g3.setWidget( i, 0, new Label( "Cloud Host:" ) );
    	g3.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);       
    	final TextBox cloudUrl_box = new TextBox();
    	cloudUrl_box.setWidth("180");
    	g3.setWidget( i++, 1, cloudUrl_box );

    	VerticalPanel cpanel = new VerticalPanel();
    	cpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    	cpanel.setSpacing(5);
    	cpanel.add(g3);
    	HTML cloudUrlMsg = new HTML ( admin_cloud_ip_setup_text );
    	cloudUrlMsg.setWidth ("300");
    	cloudUrlMsg.setStyleName("euca-small-text");
    	cpanel.add (cloudUrlMsg);
    	gpanel.add(cpanel);

    	// pull in guessed cloud URL

    	EucalyptusWebBackend.App.getInstance().getSystemConfig(sessionId,
    			new AsyncCallback( ) {
    		public void onSuccess ( final Object result ) {
    			conf = (SystemConfigWeb) result;
    			cloudUrl_box.setText (conf.getCloudHost());
    		}
    		public void onFailure ( Throwable caught ) { }
    	}
    	);

    	// user clicked submit

    	Button change_button = new Button( "Submit",
    			new ClickListener() {
    		public void onClick( Widget sender )
    		{
    			boolean formOk = true;


    			g1.clearCell( 0, 2 ); // clear previous right-hand-side annotations
    			g1.clearCell( 1, 2 ); // clear previous right-hand-side annotations
    			g2.clearCell( 0, 2 ); // clear previous right-hand-side annotations
    			g3.clearCell( 0, 2 ); // clear previous right-hand-side annotations


    			// perform checks

    			if ( newCleartextPassword1_box.getText().length() < minPasswordLength )
    			{
    				Label l = new Label( "Password is too short!" );
    				l.setStyleName("euca-error-hint");
    				g1.setWidget( newPassword1_row, 2, l );
    				formOk = false;
    			}
    			if ( !newCleartextPassword1_box.getText().equals( newCleartextPassword2_box.getText() ) )
    			{
    				Label l = new Label( "Passwords do not match!" );
    				l.setStyleName("euca-error-hint");
    				g1.setWidget( newPassword2_row, 2, l );
    				formOk = false;
    			}

    			if ( emailAddress_box.getText().length() < 1 )
    			{
    				Label l = new Label( "Email address is empty!" );
    				l.setStyleName("euca-error-hint");
    				g2.setWidget( emailAddress_row, 2, l );
    				formOk = false;
    			}

    			if ( cloudUrl_box.getText().length() < 1 )
    			{
    				Label l = new Label( "Cloud URL is empty!" );
    				l.setStyleName("euca-error-hint");
    				g3.setWidget( cloudUrl_row, 2, l );
    				formOk = false;
    			}

    			if ( !formOk )
    			{
    				label_box.setText( "Please, fix the errors and try again:" );
    				label_box.setStyleName("euca-greeting-warning");
    				return;
    			}

    			label_box.setText( "Checking with the server..." );
    			label_box.setStyleName("euca-greeting-pending");

    			loggedInUser.setEmail( emailAddress_box.getText() );
    			loggedInUser.setPassword(GWTUtils.md5(newCleartextPassword1_box.getText()));

    			EucalyptusWebBackend.App.getInstance().updateUserRecord(
    					sessionId,
    					loggedInUser,
    					new AsyncCallback() {
    						public void onSuccess( final Object result )
    						{
    							// password change succeded - pull in the new user record
    							label_box.setText( "Refreshing user data..." );
    							EucalyptusWebBackend.App.getInstance().getUserRecord(
    									sessionId,
    									null,
    									new AsyncCallback() {
    										public void onSuccess( Object result2 )
    										{
    											loggedInUser = ( UserInfoWeb ) ( (List) result2).get(0);

    											// update cloud URL
    											conf.setCloudHost(cloudUrl_box.getText());
    											EucalyptusWebBackend.App.getInstance().setSystemConfig(sessionId,
    													conf,
    													new AsyncCallback() {
    												public void onSuccess ( final Object result ) {
    													currentTabIndex = 3; // TODO: change this to confTabIndex when it's available
    													displayDefaultPage ("");
    												}
    												public void onFailure ( Throwable caught ) {
    													displayErrorPage ("Failed to save the cloud URL (check 'Configuration' tab).");
    												}
    											}
    											);

    										}
    										public void onFailure( Throwable caught )
    										{
    											displayLoginErrorPage( caught.getMessage() );
    										}
    									});
    						}
    						public void onFailure( Throwable caught )
    						{
    							loggedInUser.setEmail( "" );
    							String m = caught.getMessage();
    							label_box.setText( m );
    							label_box.setStyleName("euca-greeting-warning");
    						}
    					});
    		}
    	}
    	);

    	EucalyptusKeyboardListener sl = new EucalyptusKeyboardListener(change_button);
    	change_button.addKeyboardListener(sl);
    	newCleartextPassword1_box.addKeyboardListener(sl);
    	newCleartextPassword2_box.addKeyboardListener(sl);
    	emailAddress_box.addKeyboardListener(sl);
    	cloudUrl_box.addKeyboardListener(sl);

    	HTML message = new HTML (admin_first_time_config_text);
    	message.setWidth( "600" );

    	VerticalPanel vpanel = new VerticalPanel();
    	vpanel.setSpacing(15);
    	vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    	if (logo!=null) { vpanel.add (logo); }
    	label_box.setText( "First-time Configuration" );
		label_box.setStyleName("euca-greeting-normal");
    	vpanel.add (label_box);
    	vpanel.add (message);
    	vpanel.add (gpanel);
    	vpanel.add (change_button);

    	VerticalPanel wrapper = new VerticalPanel();
    	wrapper.add (vpanel);
    	wrapper.setSize("100%", "100%");
    	wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
    	wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);  // michael commented out

    	RootPanel.get().clear();
    	RootPanel.get().add (wrapper);
    }
}
