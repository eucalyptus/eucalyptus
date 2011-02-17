package edu.ucsb.eucalyptus.admin.server;

import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;

public class Webifier {
  
  private static final Logger LOG = Logger.getLogger( Webifier.class );
  
  public static final String USER_INFO_EMAIL = "Email";
  public static final String USER_INFO_PHONE = "Telephone";
  public static final String USER_INFO_AFFILIATION = "Affiliation";
  public static final String USER_INFO_PROJECT_DESC = "ProjectDescription";
  public static final String USER_INFO_PROJECT_PI = "ProjectPI";
  
  public static UserInfoWeb toWeb( User user ) throws AuthException {
    UserInfoWeb uif = new UserInfoWeb( );
    uif.setUserName( user.getName( ) );
    uif.setEnabled( user.isEnabled( ) );
    uif.setAdministrator( user.isSystemAdmin( ) );
    uif.setPassword( user.getPassword( ) );
    uif.setToken( user.getToken( ) );
    uif.setConfirmed( user.getRegistrationStatus( ) == User.RegistrationStatus.CONFIRMED );
    if ( uif.isConfirmed( ) ) {
      uif.setApproved( true );
    } else {
      uif.setApproved( user.getRegistrationStatus( ) == User.RegistrationStatus.APPROVED );
    }
    uif.setConfirmationCode( user.getConfirmationCode( ) );
    uif.setPasswordExpires( user.getPasswordExpires( ) );
    uif.setAccountName( user.getAccount( ).getName( ) );
    
    Map<String, String> info = user.getInfo( );
    if ( info.containsKey( USER_INFO_EMAIL ) ) {
      uif.setEmail( info.get( USER_INFO_EMAIL ) );
    }
    if ( info.containsKey( USER_INFO_PHONE ) ) {
      uif.setTelephoneNumber( info.get( USER_INFO_PHONE ) );
    }
    if ( info.containsKey( USER_INFO_AFFILIATION ) ) {
      uif.setAffiliation( info.get( USER_INFO_AFFILIATION ) );
    }
    if ( info.containsKey( USER_INFO_PROJECT_DESC) ) {
      uif.setProjectDescription( info.get( USER_INFO_PROJECT_DESC ) );
    }
    if ( info.containsKey( USER_INFO_PROJECT_PI ) ) {
      uif.setProjectPIName( info.get( USER_INFO_PROJECT_PI ) );
    }
    
    return uif;
  }
}
