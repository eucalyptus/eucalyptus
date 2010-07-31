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
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.server;

import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.Groups;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.HttpServerBootstrapper;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.Configuration;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import edu.ucsb.eucalyptus.admin.client.CloudInfoWeb;
import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.DownloadsWeb;
import edu.ucsb.eucalyptus.admin.client.EucalyptusWebBackend;
import edu.ucsb.eucalyptus.admin.client.SystemConfigWeb;
import edu.ucsb.eucalyptus.admin.client.StorageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import edu.ucsb.eucalyptus.admin.client.WalrusInfoWeb;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class EucalyptusWebBackendImpl extends RemoteServiceServlet implements EucalyptusWebBackend {

	private static Logger LOG = Logger.getLogger( EucalyptusWebBackendImpl.class );
	private static String PROPERTIES_FILE =  BaseDirectory.CONF.toString() + File.separator + "eucalyptus-web.properties";
	private static HashMap sessions = new HashMap();
	private static Properties props = new Properties();
	private static long session_timeout_ms = 1000 * 60 * 60 * 24 * 14L; /* 2 weeks (TODO: put into config?) */
	private static long pass_expiration_ms = 1000 * 60 * 60 * 24 * 365L; /* 1 year (TODO: put into config?) */

	/* parameters to be read from config file */
	private static String thanks_for_signup;
	private static String signup_request_subject;
	private static String signup_approval_subject;
	private static String signup_approval_header;
	private static String signup_approval_footer;
	private static String signup_rejection_subject;
	private static String signup_rejection_message;
	private static String password_recovery_message;
	private static String password_recovery_subject;

	/* if these are not in config file, we'll use admin's email */
	private static String signup_email;
	private static String reply_email;
	private boolean system_ready;

	private void load_props() throws SerializableException
	{
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(PROPERTIES_FILE);
			props.load(fileInputStream);
			props.setProperty("version", System.getProperty("euca.version"));
			thanks_for_signup =         props.getProperty("thanks-for-signup");
			signup_email =              props.getProperty("signup-email-address");
			reply_email =               props.getProperty("reply-email-address");
			signup_request_subject =    props.getProperty("signup-request-subject");
			signup_approval_subject =   props.getProperty("signup-approval-subject");
			signup_approval_header =    props.getProperty("signup-approval-header");
			signup_approval_footer =    props.getProperty("signup-approval-footer");
			signup_rejection_subject =  props.getProperty("signup-rejection-subject");
			signup_rejection_message =  props.getProperty("signup-rejection-message");
			password_recovery_message = props.getProperty("password-recovery-message");
			password_recovery_subject = props.getProperty("password-recovery-subject");

			if (thanks_for_signup==null) {
				throw new SerializableException("Server configuration is missing 'thanks-for-signup' value");
			}
			if (signup_request_subject==null) {
				throw new SerializableException("Server configuration is missing 'signup-request-subject' value");
			}
			if (signup_approval_subject==null) {
				throw new SerializableException("Server configuration is missing 'signup-approval-subject' value");
			}
			if (signup_approval_header==null) {
				throw new SerializableException("Server configuration is missing 'signup-approval-header' value");
			}
			if (signup_approval_footer==null) {
				throw new SerializableException("Server configuration is missing 'signup-approval-footer' value");
			}
			if (signup_rejection_subject==null) {
				throw new SerializableException("Server configuration is missing 'signup-rejection-subject' value");
			}
			if (signup_rejection_message==null) {
				throw new SerializableException("Server configuration is missing 'signup-rejection-message' value");
			}
			if (password_recovery_message==null) {
				throw new SerializableException("Server configuration is missing 'password-recovery-message' value");
			}
			if (password_recovery_subject==null) {
				throw new SerializableException("Server configuration is missing 'password-recovery-subject' value");
			}
			system_ready = true;
			if ( signup_email==null) {
				try {
					signup_email = EucalyptusManagement.getAdminEmail();
				} catch (Exception e) {
					signup_email = ""; /* signup will not work until admin email address is set */
					system_ready = false;
				}
			}
			if (reply_email==null) {
				reply_email = signup_email;
			}

		} catch (IOException e) {
			throw new SerializableException("Could not read server configuration");
		} catch (IllegalArgumentException e) {
			throw new SerializableException("Malformed escape sequence in server configuration");
		} finally {
			if(fileInputStream != null)
				try {
					fileInputStream.close();
				} catch (IOException e) {
					LOG.error(e);
				}
		}
	}

	public String addUserRecord ( UserInfoWeb user )
	throws SerializableException
	{
		return addUserRecord(null, user);
	}

	private void notifyAdminOfSignup (UserInfoWeb user)
	throws SerializableException
	{
		try {
			String http_eucalyptus = ServletUtils.getRequestUrl(getThreadLocalRequest());
			String approve_link = http_eucalyptus + "?action=approve&user=" + user.getUserName();
			String reject_link = http_eucalyptus + "?action=reject&user=" + user.getUserName();

			String email_message =
				"Someone has requested an account on the Eucalyptus system\n" +
				"\n   Name:          " + user.getRealName() +
				"\n   Username:      " + user.getUserName() +
				"\n   Email address: " + user.getEmail() +
				"\n   Telephone:     " + user.getTelephoneNumber() +
				"\n   Affiliation:   " + user.getAffiliation() +
				"\n   Project PI:    " + user.getProjectPIName() +
				"\n   Project description: " +
				"\n=====\n" + user.getProjectDescription() +
				"\n=====\n\n" +
				"To APPROVE this request, click on the following link:\n\n   " +
				approve_link +
				"\n\n" +
				"To REJECT this request, click on the following link:\n\n   " +
				reject_link +
				"\n\n";

			ServletUtils.sendMail( reply_email, signup_email,
					signup_request_subject + " (" + user.getEmail() + ")",
					email_message);

		} catch (Exception e) {
			LOG.error ("Signup mailing problem: " + e.getMessage()); /* TODO: log properly */
			throw new SerializableException ("Internal problem (failed to notify administrator by email)");
		}
	}

	public String addUserRecord(String sessionId, UserInfoWeb user)
	throws SerializableException
	{
		if (user==null) {
			throw new SerializableException("Invalid RPC arguments");
		}
		if (user.getUserName().equalsIgnoreCase( "eucalyptus" )) {
			throw new SerializableException("User 'eucalyptus' is not allowed");
		}
		// these two won't happen unless the user hacks the client side
		if ( user.getUserName().matches(".*[^\\w\\-\\.@]+.*") ) {
			throw new SerializableException ("Invalid characters in the username");
		}
		if ( user.getUserName().length() < 1 || user.getUserName().length() > 30)
		{
			throw new SerializableException ( "Invalid length of username" );
		}

		load_props(); /* get parameters from config file */
		boolean admin = false;
		try {
			SessionInfo session = verifySession (sessionId);
			UserInfoWeb requestingUser = verifyUser (session, session.getUserId(), true);
			if ( !requestingUser.isAdministrator().booleanValue()) {
				throw new SerializableException("Administrative privileges required");
			} else {
				admin = true;
			}
		} catch (Exception e) { } /* that's OK, this was an anonymous request */

		/* add the user */
		long now = System.currentTimeMillis();
		user.setPasswordExpires( new Long(now + pass_expiration_ms) );
		EucalyptusManagement.addWebUser(user);

		String response;
		if (admin) {
			/* enable the new user right away */
			user.setApproved(true);
			user.setEnabled(true);
			response = notifyUserApproved(user);
		} else {
			/* if anonymous, then notify admin */
			user.setApproved(false);
			user.setEnabled(false);
			notifyAdminOfSignup (user);
			response = thanks_for_signup;
		}
		EucalyptusManagement.commitWebUser(user);

		return response;
	}

	private String notifyUserRecovery(UserInfoWeb user)
	{
		try {
			String http_eucalyptus = ServletUtils.getRequestUrl(getThreadLocalRequest());
			String confirm_link = http_eucalyptus + "?action=recover"
			+ "&code=" + user.getConfirmationCode();

			String email_message = password_recovery_message + "\n\n" +
			confirm_link +
			"\n";

			ServletUtils.sendMail(reply_email, user.getEmail(),
					password_recovery_subject,
					email_message);

		} catch (Exception e) {
			// TODO: log this using the proper procedure
			LOG.error ("Password recovery mailing problem: " + e.getMessage());
			LOG.error ("Confirmation code for user '" + user.getUserName()
					+ "' and address " + user.getEmail()
					+ " is " + user.getConfirmationCode());

			return "Internal problem (failed to notify " + user.getEmail() + " by email)";
		}
		return "Notified '" + user.getUserName() + "' by email, thank you.";
	}

	public String recoverPassword ( UserInfoWeb web_user )
	throws SerializableException
	{
		if (web_user==null) {
			throw new SerializableException("Invalid RPC arguments");
		}

		UserInfoWeb db_user;
		try {
			/* try login first */
			db_user = EucalyptusManagement.getWebUser(web_user.getUserName());
		} catch (Exception e) {
			/* try email then */
			db_user = EucalyptusManagement.getWebUserByEmail(web_user.getEmail());
		}
		db_user.setPassword (web_user.getPassword());
		EucalyptusManagement.commitWebUser(db_user);
		return notifyUserRecovery(db_user);
	}

	/* ensure the sessionId is (still) valid */
	public static SessionInfo verifySession (String sessionId)
	throws SerializableException
	{
		if (sessionId==null) {
			throw new SerializableException("Invalid RPC arguments");
		}
		SessionInfo session = (SessionInfo)sessions.get(sessionId);
		if (session==null) {
			throw new SerializableException("Earlier session not found");
		}
		final long now = System.currentTimeMillis();
		if ((now-session.getLastAccessed()) > session_timeout_ms) {
			sessions.remove(sessionId);
			throw new SerializableException("Earlier session expired");
		}
		session.setLastAccessed(System.currentTimeMillis());
		return session;
	}

	private static boolean isPasswordExpired (UserInfoWeb user) {
		final long now = System.currentTimeMillis();
		if ((now > 0) && (now >= user.getPasswordExpires().longValue())) {
			return true;
		}
		return false;
	}

	/* ensure the user exists and valid */
	private static UserInfoWeb verifyUser (SessionInfo session, String userName, boolean verifyPasswordAge)
	throws SerializableException
	{
		UserInfoWeb user;
		if (userName==null) {
			throw new SerializableException("Invalid RPC arguments: userIname is missing");
		}
		try {
			user = EucalyptusManagement.getWebUser(userName);
		} catch (Exception e) {
			if (session!=null) {
				sessions.remove(session.getSessionId());
			}
			throw new SerializableException("Login incorrect");
		}
		if (!user.isApproved()) {
			throw new SerializableException("User not approved yet");
		}
		if (!user.isEnabled()) {
			throw new SerializableException("Disabled user account");
		}
		if (!user.isConfirmed()) {
			throw new SerializableException("Unconfirmed account (click on the link in confirmation email)");
		}
		if (verifyPasswordAge) {
			if (isPasswordExpired(user) && 
					!(user.isAdministrator() && user.getEmail().equalsIgnoreCase(UserInfo.BOGUS_ENTRY))) { // first-time config will catch that
				throw new SerializableException("Password expired");
			}
		}
		return user;
	}

	public String getNewSessionID (String userId, String md5Password)
	throws SerializableException
	{
		String sessionId = null;
		UserInfoWeb user;

		if (md5Password==null) {
			throw new SerializableException("Invalid RPC arguments: password is missing");
		}
		// you can get a sessionId with an expired password so you can change it => false
		user = verifyUser (null, userId, false);
		if (!user.getPassword().equals( md5Password )) {
			throw new SerializableException("Login incorrect");
		}

		sessionId = ServletUtils.genGUID();
		SessionInfo session = new SessionInfo(sessionId, userId, System.currentTimeMillis());
		session.setStartedOn(session.getLastAccessed());
		sessions.put(session.getSessionId(), session);

		return session.getSessionId();
	}

	private String notifyUserApproved(UserInfoWeb user)
	{
		String confString = " and confirmed";
		try {
			if (!user.isConfirmed().booleanValue()) {
				confString = "";
				String http_eucalyptus = ServletUtils.getRequestUrl(getThreadLocalRequest());
				String confirm_link = http_eucalyptus + "?action=confirm"
				+ "&code=" + user.getConfirmationCode();

				String email_message = signup_approval_header + "\n\n" +
				confirm_link +
				"\n\n" + signup_approval_footer;

				ServletUtils.sendMail(reply_email, user.getEmail(),
						signup_approval_subject,
						email_message);
			}
		} catch (Exception e) {
			// TODO: log this using the proper procedure
			LOG.error ("Approval mailing problem: " + e.getMessage());
			LOG.error ("Confirmation code for user " + user.getUserName()
					+ " is " + user.getConfirmationCode());

			return "Internal problem (failed to notify user " + user.getEmail() + " by email)";
		}
		return "User '" + user.getUserName() + "' was approved" + confString + ", thank you.";
	}

	private String notifyUserRejected(UserInfoWeb user)
	{
		try {
			ServletUtils.sendMail(reply_email, user.getEmail(),
					signup_rejection_subject,
					signup_rejection_message);

		} catch (Exception e) {
			LOG.error ("Rejection mailing problem: " + e.getMessage());
			return "Internal problem (failed to notify user " + user.getEmail() + ")";
		}
		return "User '" + user.getUserName() + "' was rejected.";
	}

	public String performAction (String sessionId, String action, String param)
	throws SerializableException
	{
		load_props();
		if (action==null || param==null) {
			throw new SerializableException("Invalid RPC arguments: action or param are missing");
		}

		/* these don't need a session */
		if (action.equals("recover") ||
				action.equals("confirm")) {
			UserInfoWeb user = EucalyptusManagement.getWebUserByCode(param);
			String response;

			if (action.equals("confirm")) {
				if ( user != null ) {
				  user.setConfirmed(true);
				}
				if (user != null) {
				  EucalyptusManagement.commitWebUser(user);
				}
				response = "Your account is now active.";
			} else {
				if (user != null) {
			      user.setPassword (user.getPassword());
				  long now = System.currentTimeMillis();
				  user.setPasswordExpires( new Long(now + pass_expiration_ms) );
				  EucalyptusManagement.commitWebUser(user);
				}
				response = "Your password has been reset.";
			}
			return response;
		}

		final SessionInfo session = verifySession (sessionId);
		final UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		String response = "Your `" + action + "` request succeeded."; /* generic response */
		if (action.equals("approve")
				|| action.equals("reject")
				|| action.equals ( "delete" )
				|| action.equals ( "disable" )
				|| action.equals ( "enable" )) {
			String userName = param;
			if (!user.isAdministrator()) {
				throw new SerializableException("Administrative privileges required");
			}
			UserInfoWeb new_user = EucalyptusManagement.getWebUser(userName);
			if (action.equals("approve")) {
				new_user.setApproved(true);
				new_user.setEnabled(true);
				new_user.setConfirmed(false);
				EucalyptusManagement.commitWebUser(new_user);
				response = notifyUserApproved(new_user);
			} else if (action.equals("reject")) {
				EucalyptusManagement.deleteWebUser(new_user);
				response = notifyUserRejected(new_user);
			} else if (action.equals("delete")) {
				EucalyptusManagement.deleteWebUser(new_user);
				/* TODO: maybe tell the user that his account was deleted? */
			} else if (action.equals("disable")) {
				new_user.setEnabled(false);
				EucalyptusManagement.commitWebUser(new_user);
			} else if (action.equals("enable")) {
				new_user.setEnabled(true);
				EucalyptusManagement.commitWebUser(new_user);
			}
			response = "Request to " + action + " user '" + userName + "' succeeded.";

		} else if (action.equals("delete_image")
				|| action.equals("enable_image")
				|| action.equals("disable_image")) {
			String imageId = param;
			if (!user.isAdministrator()) {
				throw new SerializableException("Administrative privileges required");
			}
			if (action.equals("delete_image")) {
				EucalyptusManagement.deleteImage(imageId);
			} else if (action.equals("disable_image")) {
				EucalyptusManagement.disableImage(imageId);
			} else if (action.equals("enable_image")) {
				EucalyptusManagement.enableImage(imageId);
			}
			response = "Your request succeeded, thank you.";

		} else {
			throw new SerializableException("Action '" + action + "' is not implemented.");
		}

		return response;
	}

	public void logoutSession(String sessionId)
	throws SerializableException
	{
		if (sessionId==null) {
			throw new SerializableException("Invalid RPC arguments: sessionId is missing");
		}
		SessionInfo session = (SessionInfo)sessions.get(sessionId);
		if (session!=null) {
			sessions.remove(sessionId);
			SessionInfo old = (SessionInfo)sessions.get(sessionId);
		}
	}

	public List getUserRecord (String sessionId, String userId)
	throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		List l = new ArrayList();
		if (userId==null) {
			l.add(user);
		} else {
			if (!user.isAdministrator()) {
				throw new SerializableException("Only administrators can view users");
			}
			if (userId.equals("*")) {
				l.addAll( EucalyptusManagement.getWebUsers(userId) ); /* NOTE: userId is currently ignored */
			} else {
				l.add(EucalyptusManagement.getWebUser(user.getUserName()));
			}
		}
		return l;
	}

	public static UserInfoWeb getUserRecord (String sessionId) // a *static* getUserRecord, for ImageStoreService
	throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);
		return user;
	}

	public List getImageInfo (String sessionId, String userId)
	throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		/* TODO: right now userId parameter is ignored since we only have public images */
		return EucalyptusManagement.getWebImages(userId);
	}

	/* from here on down, all requests require users to be enabled, approved, and confirmed */

	public String getNewCert(String sessionId)
	throws SerializableException
	{
		/* perform full checks */
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		return user.getToken();
	}

	public HashMap getProperties()
	throws SerializableException
	{
		load_props();
		HashMap h = new HashMap();

		for (Enumeration e = props.propertyNames(); e.hasMoreElements() ;) {
			String key = (String)e.nextElement();
			h.put(key, props.getProperty(key));
		}
		h.put("ready", system_ready);

		return h;
	}

	public String changePassword (String sessionId, String oldPassword, String newPassword )
	throws SerializableException
	{
		/* check everything except password expiration */
		SessionInfo session = verifySession (sessionId);
		// naturally, it is OK to change the password if it expired => false
		UserInfoWeb user = verifyUser (session, session.getUserId(), false);

		/* check old password if the user is changing password voluntarily */
		if ( !isPasswordExpired((UserInfoWeb)user) ) {
			if ( !oldPassword.equals(user.getPassword()) ) {
				throw new SerializableException("Old password is incorrect");
			}
		}
		user.setPassword( newPassword );
		final long now = System.currentTimeMillis();
		user.setPasswordExpires( new Long(now + pass_expiration_ms) );
		EucalyptusManagement.commitWebUser( user );

		return "Password has been changed";
	}

	public String updateUserRecord (String sessionId, UserInfoWeb newRecord )
	throws SerializableException
	{
		/* perform full checks */
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb callerRecord = verifyUser (session, session.getUserId(), true);
		String userName = newRecord.getUserName();
		UserInfoWeb oldRecord;
		try {
			oldRecord = EucalyptusManagement.getWebUser(userName);
		} catch (Exception e) {
			throw new SerializableException("Login incorrect");
		}
		if (! callerRecord.isAdministrator()
				&&  ! callerRecord.getUserName().equals(userName)) {
			throw new SerializableException ("Operation restricted to owner and administrator");
		}

		// set expiration for admin setting password for the first time
		if (oldRecord.isAdministrator() && oldRecord.getEmail().equalsIgnoreCase(UserInfo.BOGUS_ENTRY)) {
			long now = System.currentTimeMillis();
			oldRecord.setPasswordExpires( new Long(now + pass_expiration_ms) );
		}

		/* TODO: Any checks? */
		oldRecord.setRealName (newRecord.getRealName());
		oldRecord.setEmail (newRecord.getEmail());
		oldRecord.setPassword (newRecord.getPassword());
		oldRecord.setTelephoneNumber (newRecord.getTelephoneNumber());
		oldRecord.setAffiliation (newRecord.getAffiliation());
		oldRecord.setProjectDescription (newRecord.getProjectDescription());
		oldRecord.setProjectPIName (newRecord.getProjectPIName());
		oldRecord.setAdministrator(newRecord.isAdministrator());
    oldRecord.setEnabled(newRecord.isEnabled( ));

		// once confirmed, cannot be unconfirmed; also, confirmation implies approval and enablement
		if (!oldRecord.isConfirmed() && newRecord.isConfirmed()) {
			oldRecord.setConfirmed(true);
			oldRecord.setEnabled(true);
			oldRecord.setApproved(true);
		}

		EucalyptusManagement.commitWebUser( oldRecord );

		return "Account of user '" + userName + "' was updated";
	}

	public List<ClusterInfoWeb> getClusterList(String sessionId) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);
		try {
			return RemoteInfoHandler.getClusterList();
		} catch ( EucalyptusCloudException e ) {
			throw new SerializableException( e.getMessage( ) );
		}
	}

	public void setClusterList(String sessionId, List<ClusterInfoWeb> clusterList ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);
		try {
			RemoteInfoHandler.setClusterList( clusterList );
		} catch ( EucalyptusCloudException e ) {
			throw new SerializableException( e.getMessage( ) );
		}
	}

	public List<StorageInfoWeb> getStorageList(String sessionId) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);
		try {
			return RemoteInfoHandler.getStorageList();
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public void setStorageList(String sessionId, List<StorageInfoWeb> storageList ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);
		try {
			RemoteInfoHandler.setStorageList(storageList);
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public List<WalrusInfoWeb> getWalrusList(String sessionId) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);
		try {
			return RemoteInfoHandler.getWalrusList();
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public void setWalrusList(String sessionId, List<WalrusInfoWeb> walrusList ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		try {
			RemoteInfoHandler.setWalrusList(walrusList);
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public SystemConfigWeb getSystemConfig( final String sessionId ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		return EucalyptusManagement.getSystemConfig();
	}

	public void setSystemConfig( final String sessionId, final SystemConfigWeb systemConfig ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		EucalyptusManagement.setSystemConfig(systemConfig);
	}

	public List<VmTypeWeb> getVmTypes( final String sessionId ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		return RemoteInfoHandler.getVmTypes();
	}

	public void setVmTypes( final String sessionId, final List<VmTypeWeb> vmTypes ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		RemoteInfoHandler.setVmTypes(vmTypes);
	}

	public CloudInfoWeb getCloudInfo(final String sessionId, final boolean setExternalHostPort) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), true);

		return EucalyptusManagement.getCloudInfo(setExternalHostPort);
	}

	private static List<DownloadsWeb> getDownloadsFromUrl(final String downloadsUrl) {
		List<DownloadsWeb> downloadsList = new ArrayList<DownloadsWeb>();

		HttpClient httpClient = new HttpClient();
		//support for http proxy
		if(HttpServerBootstrapper.httpProxyHost != null && (HttpServerBootstrapper.httpProxyHost.length() > 0)) {
			String proxyHost = HttpServerBootstrapper.httpProxyHost;
			if(HttpServerBootstrapper.httpProxyPort != null &&  (HttpServerBootstrapper.httpProxyPort.length() > 0)) {
				int proxyPort = Integer.parseInt(HttpServerBootstrapper.httpProxyPort);
				httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
			} else {
				httpClient.getHostConfiguration().setProxyHost(new ProxyHost(proxyHost));
			}
		}
		GetMethod method = new GetMethod(downloadsUrl);
		Integer timeoutMs = new Integer(3 * 1000);
		method.getParams().setSoTimeout(timeoutMs);

		try {
			httpClient.executeMethod(method);
			String str = "";
			InputStream in = method.getResponseBodyAsStream();
			byte[] readBytes = new byte[1024];
			int bytesRead = -1;
			while((bytesRead = in.read(readBytes)) > 0) {
				str += new String(readBytes, 0, bytesRead);
			}
			String entries[] = str.split("[\\r\\n]+");
			for (int i=0; i<entries.length; i++) {
				String entry[] = entries[i].split("\\t");
				if (entry.length == 3) {
					downloadsList.add (new DownloadsWeb(entry[0], entry[1], entry[2]));
				}
			}

		} catch (MalformedURLException e) {
			LOG.warn("Malformed URL exception: " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			LOG.warn("I/O exception: " + e.getMessage());
			e.printStackTrace();

		} finally {
			method.releaseConnection();
		}

		return downloadsList;
	}

	public List<DownloadsWeb> getDownloads(final String sessionId, final String downloadsUrl) throws SerializableException {
		SessionInfo session = verifySession(sessionId);
		UserInfoWeb user = verifyUser(session, session.getUserId(), true);
		return getDownloadsFromUrl(downloadsUrl);
	}

	private static String readFileAsString(String filePath) throws java.io.IOException {
	    byte[] buffer = new byte[(int) new File(filePath).length()];
	    BufferedInputStream f = null;
	    try {
	        f = new BufferedInputStream (new FileInputStream(filePath));
	        f.read(buffer);
	    } finally {
	        if (f != null) try { f.close(); } catch (IOException ignored) { }
	    }
	    return new String(buffer);
	}
	
	public String getFileContentsByPath(final String sessionId, final String path) throws SerializableException {
		SessionInfo session = verifySession(sessionId);
		UserInfoWeb user = verifyUser(session, session.getUserId(), true);
		String realPath = BaseDirectory.HOME.toString() + "/var/run/eucalyptus/webapp/" + path;
		LOG.debug("feeding contents of " + realPath);
		// TODO: verify path
		try {
			String result = readFileAsString (realPath);
			LOG.debug("read from " + realPath + " string of size " + result.length());
			return result;
		} catch (java.io.IOException e) {
			LOG.debug("failed to feed " + realPath + " due to exception " + e.getMessage());
			throw new SerializableException (e.getMessage());
		}
	}
	
	/**
	 * Overridden to really throw Jetty RetryRequest Exception (as opposed to sending failure to client).
	 *
	 * @param caught the exception
	 */
	protected void doUnexpectedFailure(Throwable caught)
	{
		throwIfRetryRequest(caught);
		super.doUnexpectedFailure(caught);
	}
	private static final String JETTY_RETRY_REQUEST_EXCEPTION = "org.mortbay.jetty.RetryRequest";
	/**
	 * Throws the Jetty RetryRequest if found.
	 *
	 * @param caught the exception
	 */
	protected void throwIfRetryRequest(Throwable caught)
	{
		if (caught instanceof UnexpectedException )
		{
			caught = caught.getCause();
		}

		if (JETTY_RETRY_REQUEST_EXCEPTION.equals(caught.getClass().getName()))
		{
			throw (RuntimeException)caught;
		}
	}

}
