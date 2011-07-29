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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.PersistenceException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.HttpServerBootstrapper;
import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Service;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.www.Reports;
import com.eucalyptus.www.Reports.ReportCache;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.UnexpectedException;
import edu.ucsb.eucalyptus.admin.client.CloudInfoWeb;
import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.DownloadsWeb;
import edu.ucsb.eucalyptus.admin.client.EucalyptusWebBackend;
import edu.ucsb.eucalyptus.admin.client.GroupInfoWeb;
import edu.ucsb.eucalyptus.admin.client.StorageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.SystemConfigWeb;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import edu.ucsb.eucalyptus.admin.client.WalrusInfoWeb;
import edu.ucsb.eucalyptus.admin.client.reports.ReportInfo;

public class EucalyptusWebBackendImpl extends RemoteServiceServlet implements EucalyptusWebBackend {

	private static Logger LOG = Logger.getLogger( EucalyptusWebBackendImpl.class );
	private static String PROPERTIES_FILE =  BaseDirectory.CONF.toString() + File.separator + "eucalyptus-web.properties";
	private static HashMap sessions = new HashMap();
	private static Properties props = new Properties();
	private static long session_timeout_ms = 1000 * 60 * 60 * 24 * 14L; /* 2 weeks (TODO: put into config?) */
	private static long pass_expiration_ms = 1000 * 60 * 60 * 24 * 365L; /* 1 year (TODO: put into config?) */
	private static long recovery_expiration_ms = 1000 * 60 * 30; // 30 minutes (TODO: put into config?)

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
			UserInfoWeb requestingUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
			if ( !requestingUser.isAdministrator().booleanValue()) {
				user.setAdministrator(false); // in case someone is trying to be sneaky
				throw new SerializableException("Administrative privileges required");
			} else {
				admin = true;
			}
		} catch (Exception e) { } /* that's OK, this was an anonymous request */

		/* add the user */
		long now = System.currentTimeMillis();
		user.setPasswordExpires( new Long(now + pass_expiration_ms) );
        user.setConfirmationCode( Crypto.generateSessionToken( user.getUserName() ) );
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

	private void notifyUserRecovery(UserInfoWeb user)
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
		}
	}

	public String recoverPassword ( UserInfoWeb web_user )
	throws SerializableException
	{
		if (web_user==null) {
			throw new SerializableException("Invalid RPC arguments");
		}

		String response;
		if (web_user.getPassword()==null) { // someone is initiating password recovery
			try {
				UserInfoWeb db_user = EucalyptusManagement.getWebUser(web_user.getUserName(), web_user.getAccountName());
				if (!db_user.isConfirmed() || !db_user.isEnabled()) {
					throw new SerializableException("Illegal request"); // no password recoveries before confirmation or while disabled
				}
				if (db_user.getEmail().equalsIgnoreCase(web_user.getEmail())) {
					long expires = System.currentTimeMillis() + recovery_expiration_ms;
					db_user.setConfirmationCode(String.format("%015d", expires) + Crypto.generateSessionToken( db_user.getUserName() ) );
					EucalyptusManagement.commitWebUser(db_user);
					notifyUserRecovery(db_user);
				}
			} catch (Exception e) { } // pretend all is well regardless of the outcome
			response = "Please, check your email for further instructions.";

		} else { // someone is trying to change the password			
			String code = web_user.getConfirmationCode();
			if (code==null) {
				throw new SerializableException("Insufficient parameters");
			}
			UserInfoWeb db_user;
			try {
				db_user = EucalyptusManagement.getWebUserByCode(code);
				long expires = Long.parseLong(code.substring(0, 15));
				long now = System.currentTimeMillis();
				if (now > expires) {
					throw new SerializableException("Recovery attempt expired");
				}
				db_user.setConfirmationCode("-unset-"); // so the code cannot be reused
				db_user.setPassword (web_user.getPassword());
				db_user.setPasswordExpires( new Long(now + pass_expiration_ms) );
				EucalyptusManagement.commitWebUser(db_user);
			} catch (Exception e) {
				throw new SerializableException("Incorrect code");
			}

			response = "Your password has been reset.";
		}
		return response;
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
	private static UserInfoWeb verifyUser (SessionInfo session, String userName, String accountName, boolean verifyPasswordAge)
	throws SerializableException
	{
		UserInfoWeb user;
		if (userName==null) {
			throw new SerializableException("Invalid RPC arguments: userName is missing");
		}
		try {
			user = EucalyptusManagement.getWebUser(userName, accountName);
		} catch (Exception e) {
			if (session!=null) {
				sessions.remove(session.getSessionId());
			}
			throw new SerializableException("Username '" + userName + "' not found");
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
					!(user.isAdministrator() && user.getEmail().equalsIgnoreCase("n/a"))) { // first-time config will catch that
				throw new SerializableException("Password expired");
			}
		}
		return user;
	}

	public String getNewSessionID (String userId, String password)
	throws SerializableException
	{
		String sessionId = null;
		UserInfoWeb user;

		if (userId == null) {
		  throw new SerializableException("Invalid RPC arguments: user ID is missing");
		}
		if (password == null) {
			throw new SerializableException("Invalid RPC arguments: password is missing");
		}
		
		// Parse userId in the follow forms:
		// 1. "username" (meaning "username" in "eucalyptus" account)
		// 2. "accountname/username"
		String account = null;
		int slash = userId.indexOf( '/' );
		if (slash > 0) {
		  account = userId.substring( 0, slash );
		}
		userId = userId.substring( slash + 1 );
		if (account == null || "".equals(account)) {
		  account = Account.SYSTEM_ACCOUNT;
		}
		// you can get a sessionId with an expired password so you can change it => false
		user = verifyUser (null, userId, account, false);
		if (!user.getPassword().equals( Crypto.generateHashedPassword( password ) ) ) {
			throw new SerializableException("Incorrect password");
		}

		sessionId = ServletUtils.genGUID();
		SessionInfo session = new SessionInfo(sessionId, userId, account, System.currentTimeMillis());
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
			String response = "OK";

			if (action.equals("confirm")) {
				if ( user != null ) {
				  user.setConfirmed(true);
				  user.setConfirmationCode("-unset-"); // so the code cannot be reused
				  EucalyptusManagement.commitWebUser(user);
				}
				response = "Your account is now active.";				
			} else if (action.equals("recover")) { // this is just a way to verify that the code is valid (TODO: remove?)
				if (user == null) {
			      throw new SerializableException("Invalid code");
				}
				response = "Your password has been reset.";
			}
			return response;
		}

		final SessionInfo session = verifySession (sessionId);
		final UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

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
			UserInfoWeb new_user = EucalyptusManagement.getWebUser(userName, session.getAccountId( ));
			if (action.equals("approve")) {
				if (new_user.isApproved()) {
					throw new SerializableException("User already approved");
				}
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
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

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
				l.add(EucalyptusManagement.getWebUser(user.getUserName(), user.getAccountName( )));
			}
		}
		return l;
	}

	public static UserInfoWeb getUserRecord (String sessionId) // a *static* getUserRecord, for ImageStoreService
	throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		return user;
	}

	public List getImageInfo (String sessionId, String userId)
	throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

		/* TODO: right now userId parameter is ignored since we only have public images */
		return EucalyptusManagement.getWebImages(userId);
	}

	/* from here on down, all requests require users to be enabled, approved, and confirmed */

	public String getNewCert(String sessionId)
	throws SerializableException
	{
		/* perform full checks */
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

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
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), false);
		
		/* check old password if the user is changing password voluntarily */
		if ( !isPasswordExpired((UserInfoWeb)user) ) {
			if ( !Crypto.generateHashedPassword(oldPassword).equals(user.getPassword())) {
				throw new SerializableException("Old password is incorrect");
			}
		}
		user.setPassword(Crypto.generateHashedPassword(newPassword));
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
		UserInfoWeb callerRecord = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		String userName = newRecord.getUserName();
		UserInfoWeb oldRecord;
		try {
			oldRecord = EucalyptusManagement.getWebUser(userName, session.getAccountId( ));
		} catch (Exception e) {
			throw new SerializableException("Username '" + userName + "' not found");
		}
		if (! callerRecord.isAdministrator()
				&&  ! callerRecord.getUserName().equals(userName)) {
			throw new SerializableException ("Operation restricted to owner and administrator");
		}

		// set expiration for admin setting password for the first time
		if (oldRecord.isAdministrator() && oldRecord.getEmail().equalsIgnoreCase("n/a")) {
			long now = System.currentTimeMillis();
			oldRecord.setPasswordExpires( new Long(now + pass_expiration_ms) );
		}

		/* TODO: Any checks? */
		// only an admin should be able to change this settings                                                                    
		if (callerRecord.isAdministrator()) {   
//TODO:ASAP:REVIEW:YE
			// set password and expiration for admin when logging in for the first time
//			if (oldRecord.getEmail().equalsIgnoreCase(UserInfo.BOGUS_ENTRY)) {
//				long now = System.currentTimeMillis();
//				oldRecord.setPasswordExpires( new Long(now + pass_expiration_ms) );
//				oldRecord.setPassword (newRecord.getPassword());
//			}
			
			// admin can reset pwd of another user, but
			// to reset his own password he has to use
			// "change password" functionality
			if(!callerRecord.getUserName().equals(userName))
				oldRecord.setPassword (Crypto.generateHashedPassword(newRecord.getPassword()));	

			if(oldRecord.isAdministrator() != newRecord.isAdministrator())                                                     
				oldRecord.setAdministrator(newRecord.isAdministrator());                                                   
			if(oldRecord.isEnabled() != newRecord.isEnabled())                                                                 
				oldRecord.setEnabled(newRecord.isEnabled( ));                                                              
			// once confirmed, cannot be unconfirmed; also, confirmation implies approval and enablement                       
			if (!oldRecord.isConfirmed() && newRecord.isConfirmed()) {                                                         
				oldRecord.setConfirmed(true);                                                                              
				oldRecord.setEnabled(true);                                                                                
				oldRecord.setApproved(true);                                                                               
			}                                                                                                                  
		}  
		
		oldRecord.setRealName (newRecord.getRealName());
		oldRecord.setEmail (newRecord.getEmail());
		oldRecord.setTelephoneNumber (newRecord.getTelephoneNumber());
		oldRecord.setAffiliation (newRecord.getAffiliation());
		oldRecord.setProjectDescription (newRecord.getProjectDescription());
		oldRecord.setProjectPIName (newRecord.getProjectPIName());

		EucalyptusManagement.commitWebUser( oldRecord );

		return "Account of user '" + userName + "' was updated";
	}

	public List<ClusterInfoWeb> getClusterList(String sessionId) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		try {
			return RemoteInfoHandler.getClusterList();
		} catch ( EucalyptusCloudException e ) {
			throw new SerializableException( e.getMessage( ) );
		}
	}

	public void setClusterList(String sessionId, List<ClusterInfoWeb> clusterList ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		try {
			RemoteInfoHandler.setClusterList( clusterList );
		} catch ( EucalyptusCloudException e ) {
			throw new SerializableException( e.getMessage( ) );
		}
	}

	public List<StorageInfoWeb> getStorageList(String sessionId) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		try {
			return RemoteInfoHandler.getStorageList();
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public void setStorageList(String sessionId, List<StorageInfoWeb> storageList ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		try {
			RemoteInfoHandler.setStorageList(storageList);
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public List<WalrusInfoWeb> getWalrusList(String sessionId) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		try {
			return RemoteInfoHandler.getWalrusList();
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public void setWalrusList(String sessionId, List<WalrusInfoWeb> walrusList ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

		try {
			RemoteInfoHandler.setWalrusList(walrusList);
		} catch(EucalyptusCloudException e) {
			throw new SerializableException(e.getMessage());
		}
	}

	public SystemConfigWeb getSystemConfig( final String sessionId ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

		return EucalyptusManagement.getSystemConfig();
	}

	public void setSystemConfig( final String sessionId, final SystemConfigWeb systemConfig ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

		EucalyptusManagement.setSystemConfig(systemConfig);
	}

	public List<VmTypeWeb> getVmTypes( final String sessionId ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

		return RemoteInfoHandler.getVmTypes();
	}

	public void setVmTypes( final String sessionId, final List<VmTypeWeb> vmTypes ) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

		RemoteInfoHandler.setVmTypes(vmTypes);
	}

	public CloudInfoWeb getCloudInfo(final String sessionId, final boolean setExternalHostPort) throws SerializableException
	{
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);

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
		UserInfoWeb user = verifyUser(session, session.getUserId(), session.getAccountId( ), true);
		return getDownloadsFromUrl(downloadsUrl);
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

	@Override
	public void addGroup(String sessionId, GroupInfoWeb group) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!user.isAdministrator()) {
			throw new Exception("Only admin can add a group");
		}
		EucalyptusManagement.addGroup(group);
	}

	@Override
	public void addUser(String sessionId, UserInfoWeb user,
			List<String> groupNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can add a user");
		}
		addUserRecord(sessionId, user);
		boolean inDefaultGroup = false;
		for (String groupName : groupNames) {
		  if ( "default".equals( groupName ) ) {
		    inDefaultGroup = true;
		  }
			try {
				EucalyptusManagement.addUserToGroup(user.getUserName(), groupName);
			} catch (Exception e) {
				// Ignore exception
				LOG.debug(e, e);
			}
		}
		if ( !inDefaultGroup && groupNames.size( ) > 0 ) {
		  EucalyptusManagement.removeUserFromGroup( user.getUserName( ), "default" );
		}
	}

	@Override
	public void deleteGroups(String sessionId, List<String> groupNames)
			throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!user.isAdministrator()) {
			throw new Exception("Only admin can add a group");
		}
		for (String groupName : groupNames) {
			try {
				EucalyptusManagement.deleteGroup(groupName);
			} catch (Exception e) {
				// Ignore exception
				LOG.debug(e, e);
			}
		}
	}

	@Override
	public List<GroupInfoWeb> getGroups(String sessionId, String name)
			throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!user.isAdministrator()) {
			throw new Exception("Only admin can view group list");
		}
		if (name == null || "".equals(name)) {
			return EucalyptusManagement.getAllGroups();
		}
		List<GroupInfoWeb> gis = new ArrayList<GroupInfoWeb>();
		GroupInfoWeb gi = EucalyptusManagement.getGroup(name);
		if (gi != null) {
			gis.add(gi);
		}
		return gis;
	}

	@Override
	public List<String> getGroupsByUser(String sessionId, String userName)
			throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!user.isAdministrator()) {
			throw new Exception("Only admin can view group list");
		}
		if (userName == null || "".equals(userName)) {
			return new ArrayList<String>();
		}
		return EucalyptusManagement.getUserGroups(userName);
	}

	@Override
	public List<UserInfoWeb> getUsersByGroups(String sessionId,
			List<String> groupNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb user = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!user.isAdministrator()) {
			throw new Exception("Only admin can view user list of a group");
		}
		List<UserInfoWeb> users = new ArrayList<UserInfoWeb>();
		Set<String> userSet = new HashSet<String>();
		for (String groupName : groupNames) {
			for (UserInfoWeb ui : EucalyptusManagement.getGroupMembers(groupName)) {
				if (userSet.add(ui.getUserName())) {
					users.add(ui);
				}
			}
		}
		return users;
	}

	@Override
	public void updateGroup(String sessionId, GroupInfoWeb group)
			throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can update a group");
		}
		EucalyptusManagement.updateGroup(group);
	}

	@Override
	public void updateUser(String sessionId, UserInfoWeb user,
			List<String> groupNames) throws Exception {
		updateUserRecord(sessionId, user);
		EucalyptusManagement.updateUserGroups(user.getUserName(), groupNames);
	}
	
	@Override
	public void deleteUsers(final String sessionId, 
			final List<String> userNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can delete users");
		}
		for (String userName : userNames) {
			EucalyptusManagement.deleteUser(userName);
		}
	}
	
	@Override
	public void addUsersToGroups(final String sessionId, 
			final List<String> userNames, final List<String> groupNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can change user's group membership");
		}
		for (String groupName : groupNames) {
			for (String userName : userNames) {
				EucalyptusManagement.addUserToGroup(userName, groupName);
			}
		}
	}
	
	@Override
	public void removeUsersFromGroups(final String sessionId, 
			final List<String> userNames, final List<String> groupNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can change user's group membership");
		}
		for (String groupName : groupNames) {
			for (String userName : userNames) {
				EucalyptusManagement.removeUserFromGroup(userName, groupName);
			}
		}
	}
	
	@Override
	public void enableUsers(final String sessionId, final List<String> userNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can enable users");
		}
		for (String userName : userNames) {
			UserInfoWeb updateUser = EucalyptusManagement.getWebUser(userName, session.getAccountId( ));
			updateUser.setEnabled(true);
			EucalyptusManagement.commitWebUser(updateUser);
		}
	}
	
	@Override
	public void disableUsers(final String sessionId, final List<String> userNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can disable users");
		}
		for (String userName : userNames) {
			UserInfoWeb updateUser = EucalyptusManagement.getWebUser(userName, session.getAccountId( ));
			updateUser.setEnabled(false);
			EucalyptusManagement.commitWebUser(updateUser);
		}
	}
	
	@Override
	public void approveUsers(final String sessionId, final List<String> userNames) throws Exception {
		SessionInfo session = verifySession (sessionId);
		UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
		if (!reqUser.isAdministrator()) {
			throw new Exception("Only admin can approve users");
		}
		for (String userName : userNames) {
			UserInfoWeb updateUser = EucalyptusManagement.getWebUser(userName, session.getAccountId( ));
			updateUser.setEnabled(true);
			updateUser.setApproved(true);
			updateUser.setConfirmed(false);
			EucalyptusManagement.commitWebUser(updateUser);
		}
	}
	
	@Override
	public List<String> getZones(final String sessionId) throws Exception {
		List<String> zones = new ArrayList<String>();
		for ( ServiceConfiguration cluster : ServiceConfigurations.list( ClusterController.class ) ) {
		  zones.add( cluster.getName( ) );
		}
		return zones;
	}

	@Override
  public List<ReportInfo> getReports(final String sessionId) throws Exception {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb reqUser = verifyUser (session, session.getUserId(), session.getAccountId( ), true);
    if (!reqUser.isAdministrator()) {
      throw new Exception("Only admin can view reports.");
    }
    List<ReportInfo> reports = new ArrayList<ReportInfo>();
    for( Component c : Components.list( ) ) {
      for( ServiceConfiguration s : c.lookupServiceConfigurations( ) ) {
        reports.addAll( EucalyptusWebBackendImpl.getServiceLogInfo( s ) );
      }
    }
    SortedSet<String> sortedReports = new TreeSet<String>();
    for( File report : SubDirectory.REPORTS.getFile( ).listFiles( new FilenameFilter() {
      @Override
      public boolean accept( File arg0, String arg1 ) {
        return arg1.endsWith( ".jrxml" );
      }} ) ) {
      sortedReports.add( report.getName( ).replaceAll( ".jrxml", "" ) );
    }
    for( String reportName : sortedReports ) {
      try {
        ReportCache reportCache = Reports.getReportManager( reportName, false );
        reports.add( new ReportInfo( reportCache.getReportGroup( ), reportCache.getReportName( ), reportName, 1 ) );
      }
      catch ( Throwable e ) {
        LOG.error( e, e );
        LOG.error( "Failed to read report file: " + reportName + " because of: " + e.getMessage( ) );
      }
    }
    return reports;
  }

  @Override
  public String processCall( String payload ) throws SerializationException {
    try {
      return super.processCall( payload );
    } catch ( Throwable e ) {
      LOG.error( e, e );
      throw new SerializationException( e );
    }
  }

  private static String SERVICE_GROUP = "service";
  public static List<ReportInfo> getServiceLogInfo( ServiceConfiguration conf ) {
    List<ReportInfo> reports = new ArrayList<ReportInfo>();
    ComponentId compId = conf.getComponentId( );
    if( compId instanceof Walrus ) {
      String serviceFq = "Walrus @ "+conf.getHostName( );
      reports.add( new ReportInfo( SERVICE_GROUP, serviceFq, SERVICE_GROUP, 1, compId.name( ), conf.getName( ), conf.getHostName( ) ) );
    } else if( compId  instanceof com.eucalyptus.component.id.ClusterController ) {
      reports.add( new ReportInfo( SERVICE_GROUP, "CC @ "+conf.getHostName( ), SERVICE_GROUP, 1, compId.name( ), conf.getName( ), conf.getHostName( ) ) );
      final Cluster cluster = Clusters.getInstance( ).lookup( conf.getName( ) );
      for( String nodeTag : cluster.getNodeTags( ) ) {
        URI uri = URI.create( nodeTag );
        reports.add( new ReportInfo( SERVICE_GROUP, "NC @ " + uri.getHost( ), SERVICE_GROUP, 1, "node", conf.getName( ), uri.getHost( ) ) );
      }
      try {
        ServiceConfiguration scConfig = ServiceConfigurations.lookupByName( Storage.class, cluster.getPartition( ) );
        reports.add( new ReportInfo( SERVICE_GROUP, "SC @ " + scConfig.getHostName( ), SERVICE_GROUP, 1, scConfig.getComponentId( ).name( ), scConfig.getName( ), scConfig.getHostName( ) ) );        
      } catch ( PersistenceException e ) {
        LOG.error( e.getMessage( ), e );
      } catch ( NoSuchElementException e ) {
        LOG.error( e.getMessage( ) );
      }
    }
    return reports;
  }

}
