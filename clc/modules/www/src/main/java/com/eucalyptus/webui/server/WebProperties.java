package com.eucalyptus.webui.server;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.base.Strings;

public class WebProperties {
  
  public static final String VERSION = "version";
  
  private static final Logger LOG = Logger.getLogger( WebProperties.class );
  
  private static String PROPERTIES_FILE =  BaseDirectory.CONF.toString() + File.separator + "eucalyptus-web.properties";
  
  private static final String EUCA_VERSION = "euca.version";
  
  public static final String ACCOUNT_SIGNUP_SUBJECT = "account-signup-subject";
  public static final String ACCOUNT_SIGNUP_SUBJECT_DEFAULT = "[Eucalyptus] New account has been signed up";

  public static final String USER_SIGNUP_SUBJECT = "user-signup-subject";
  public static final String USER_SIGNUP_SUBJECT_DEFAULT = "[Eucalyptus] New user has signed up";

  public static final String ACCOUNT_APPROVAL_SUBJECT = "account-approval-subject";
  public static final String ACCOUNT_APPROVAL_SUBJECT_DEFAULT = "Your Eucalyptus account application was approved";

  public static final String ACCOUNT_REJECTION_SUBJECT = "account-rejection-subject";
  public static final String ACCOUNT_REJECTION_SUBJECT_DEFAULT = "Your Eucalyptus account application was rejected";

  public static final String ACCOUNT_REJECTION_MESSAGE = "account-rejection-message";
  public static final String ACCOUNT_REJECTION_MESSAGE_DEFAULT = "To whom it may concern: \n\n I'm sorry to let your know that " +
                                                                 "your application for an Eucalyptus account was rejected. " +
                                                                 "Please contact your system administrator for further information." +
                                                                 "\n\n --Registration Admin";

  public static final String USER_APPROVAL_SUBJECT = "user-approval-subject";
  public static final String USER_APPROVAL_SUBJECT_DEFAULT = "Your Eucalyptus user application was approved";

  public static final String USER_REJECTION_SUBJECT = "user-rejection-subject";
  public static final String USER_REJECTION_SUBJECT_DEFAULT = "Your Eucalyptus user application was rejected";
  
  public static final String USER_REJECTION_MESSAGE = "user-rejection-message";
  public static final String USER_REJECTION_MESSAGE_DEFAULT = "To whom it may concern: \n\n I'm sorry to let your know that " +
                                                                 "your application for an Eucalyptus user account was rejected. " +
                                                                 "Please contact your system administrator for further information." +
                                                                 "\n\n --Registration Admin";
  
  public static final String PASSWORD_RESET_SUBJECT = "password-reset-subject";
  public static final String PASSWORD_RESET_SUBJECT_DEFAULT = "Request to reset your Eucalyptus password";

  public static final String PASSWORD_RESET_MESSAGE = "password-reset-message";
  public static final String PASSWORD_RESET_MESSAGE_DEFAULT = "You or someone pretending to be you made a request to " + 
                                                              "reset the password on a Eucalyptus elastic cloud system. " +
                                                              "Disregard this message if resetting the password was not your intention, " +
                                                              "but if it was, click the following link to change of password:";

  public static final String RIGHTSCALE_WHOAMI_URL = "rightscale-whoami-url";
  public static final String RIGHTSCALE_WHOAMI_URL_DEFAULT = "https://my.rightscale.com/whoami?api_version=1.0&cloud=0";
  
  public static final String IMAGE_DOWNLOAD_URL = "image-download-url";
  public static final String IMAGE_DOWNLOAD_URL_DEFAULT = "http://www.eucalyptussoftware.com/downloads/eucalyptus-images/list.php?version=";
  
  public static final String TOOL_DOWNLOAD_URL = "tool-download-url";
  public static final String TOOL_DOWNLOAD_URL_DEFAULT = "http://www.eucalyptussoftware.com/downloads/eucalyptus-tools/list.php?version=";
  
  public static HashMap<String, String> getProperties( ) {
    Properties props = new Properties( );
    FileInputStream input = null;
    try {
      input = new FileInputStream( PROPERTIES_FILE );
      props.load( input );
      props.setProperty( VERSION, "Eucalyptus " + System.getProperty( EUCA_VERSION ) );    
    } catch ( Exception e ) {
      LOG.error( "Failed to load web properties", e );
    } finally {
      if ( input != null ) {
        try {
          input.close( );
        } catch ( Exception e ) { }
      }
    }
    return new HashMap<String, String>( ( Map ) props );
  }
  
  public static String getProperty( String key, String defaultValue ) {
    String subject = getProperties( ).get( key );
    if ( Strings.isNullOrEmpty( subject ) ) {
      subject = defaultValue;
    }
    return subject;
  }
  
  public static String getVersion( ) {
    return System.getProperty( EUCA_VERSION );
  }
  
}
