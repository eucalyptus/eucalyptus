/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.server;

import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.server.rpc.OpenRemoteServiceServlet;
import edu.ucsb.eucalyptus.admin.client.*;
import edu.ucsb.eucalyptus.util.BaseDirectory;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dmitriizagorodnov
 * Date: May 3, 2008
 * Time: 2:57:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class EucalyptusWebBackendImpl extends OpenRemoteServiceServlet implements EucalyptusWebBackend {

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
        try {
            props.load(new FileInputStream(PROPERTIES_FILE));
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
            user.setIsApproved(true);
            user.setIsEnabled(true);
            EucalyptusManagement.commitWebUser(user);
            response = notifyUserApproved(user);
        } else {
            /* if anonymous, then notify admin */
            notifyAdminOfSignup (user);
            response = thanks_for_signup;
        }

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
		db_user.setTemporaryPassword (web_user.getBCryptedPassword());
		EucalyptusManagement.commitWebUser(db_user);
		return notifyUserRecovery(db_user);
    }

    /* ensure the sessionId is (still) valid */
    private SessionInfo verifySession (String sessionId)
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

    private boolean isPasswordExpired (UserInfoWeb user) {
        final long now = System.currentTimeMillis();
        if ((now > 0) && (now >= user.getPasswordExpires().longValue())) {
            return true;
        }
        return false;
    }

    /* ensure the user exists and valid */
    private UserInfoWeb verifyUser (SessionInfo session, String userName, boolean verifyPasswordAge)
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
            if (isPasswordExpired(user)) {
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
        if (!user.getBCryptedPassword().equals( md5Password )) {
            throw new SerializableException("Incorrect password");
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
				user.setIsConfirmed(true);
				EucalyptusManagement.commitWebUser(user);
				response = "Your account is now active.";
			} else {
				user.setBCryptedPassword (user.getTemporaryPassword());
				long now = System.currentTimeMillis();
		        user.setPasswordExpires( new Long(now + pass_expiration_ms) );
				EucalyptusManagement.commitWebUser(user);
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
                new_user.setIsApproved(true);
                new_user.setIsEnabled(true);
                new_user.setIsConfirmed(false);
                EucalyptusManagement.commitWebUser(new_user);
                response = notifyUserApproved(new_user);
            } else if (action.equals("reject")) {
                EucalyptusManagement.deleteWebUser(new_user);
                response = notifyUserRejected(new_user);
            } else if (action.equals("delete")) {
                EucalyptusManagement.deleteWebUser(new_user);
                /* TODO: maybe tell the user that his account was deleted? */
            } else if (action.equals("disable")) {
                new_user.setIsEnabled(false);
                EucalyptusManagement.commitWebUser(new_user);
            } else if (action.equals("enable")) {
                new_user.setIsEnabled(true);
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
                l = EucalyptusManagement.getWebUsers(userId); /* NOTE: userId is currently ignored */
            } else {
                l.add(EucalyptusManagement.getWebUser(user.getUserName()));
            }
        }
        return l;
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

        return user.getCertificateCode();
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
            if ( !oldPassword.equals(user.getBCryptedPassword()) ) {
                throw new SerializableException("Old password is incorrect");
            }
        }
        user.setBCryptedPassword( newPassword );
        final long now = System.currentTimeMillis();
        user.setPasswordExpires( new Long(now + pass_expiration_ms) );
        EucalyptusManagement.commitWebUser( user );

        return "Your password has been changed";
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
            throw new SerializableException("Username '" + userName + "' not found");
        }
		if (! callerRecord.isAdministrator()
		&&  ! callerRecord.getUserName().equals(userName)) {
				throw new SerializableException ("Operation restricted to owner and administrator");
		}

        /* TODO: Any checks? Reset password expiration? */
		oldRecord.setRealName (newRecord.getRealName());
		oldRecord.setEmail (newRecord.getEmail());
		oldRecord.setBCryptedPassword (newRecord.getBCryptedPassword());
		oldRecord.setTelephoneNumber (newRecord.getTelephoneNumber());
		oldRecord.setAffiliation (newRecord.getAffiliation());
		oldRecord.setProjectDescription (newRecord.getProjectDescription());
		oldRecord.setProjectPIName (newRecord.getProjectPIName());
        oldRecord.setIsAdministrator(newRecord.isAdministrator());
        if (!oldRecord.isConfirmed()) { // once confirmed, cannot be unconfirmed
            oldRecord.setIsConfirmed(newRecord.isConfirmed());
        }

        EucalyptusManagement.commitWebUser( oldRecord );

        return "Account updated";
    }

  public List<ClusterInfoWeb> getClusterList(String sessionId) throws SerializableException
  {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb user = verifyUser (session, session.getUserId(), true);
    //:: TODO-1.4: anything more to do here? :://
    return RemoteInfoHandler.getClusterList();
  }

  public void setClusterList(String sessionId, List<ClusterInfoWeb> clusterList ) throws SerializableException
  {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb user = verifyUser (session, session.getUserId(), true);
    //:: TODO-1.4: anything more to do here? :://
    RemoteInfoHandler.setClusterList( clusterList );
  }

  public SystemConfigWeb getSystemConfig( final String sessionId ) throws SerializableException
  {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb user = verifyUser (session, session.getUserId(), true);
    //:: TODO-1.4: anything more to do here? :://
    return EucalyptusManagement.getSystemConfig();
  }

  public void setSystemConfig( final String sessionId, final SystemConfigWeb systemConfig ) throws SerializableException
  {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb user = verifyUser (session, session.getUserId(), true);
    //:: TODO-1.4: anything more to do here? :://
    EucalyptusManagement.setSystemConfig(systemConfig);
  }

  public List<VmTypeWeb> getVmTypes( final String sessionId ) throws SerializableException
  {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb user = verifyUser (session, session.getUserId(), true);
    //:: TODO-1.4: anything more to do here? :://
    return RemoteInfoHandler.getVmTypes();
  }

  public void setVmTypes( final String sessionId, final List<VmTypeWeb> vmTypes ) throws SerializableException
  {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb user = verifyUser (session, session.getUserId(), true);
    //:: TODO-1.4: anything more to do here? :://
    RemoteInfoHandler.setVmTypes(vmTypes);
  }

  public CloudInfoWeb getCloudInfo(final String sessionId, final boolean setExternalHostPort) throws SerializableException
  {
    SessionInfo session = verifySession (sessionId);
    UserInfoWeb user = verifyUser (session, session.getUserId(), true);
    //:: TODO-1.4: anything more to do here? :://
    return EucalyptusManagement.getCloudInfo(setExternalHostPort);	
  }
}