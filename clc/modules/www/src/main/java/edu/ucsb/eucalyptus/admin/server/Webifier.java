package edu.ucsb.eucalyptus.admin.server;

import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.crypto.Crypto;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;

public class Webifier {
  
  private static final Logger LOG = Logger.getLogger( Webifier.class );
  
  public static final String USER_INFO_FULLNAME = "FullName";
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
    if ( info.containsKey( USER_INFO_FULLNAME ) ) {
      uif.setRealName( info.get( USER_INFO_FULLNAME ) );
    }
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
    for ( AccessKey k : user.getKeys( ) ) {
      if ( k.isActive( ) ) {
        uif.setQueryId( k.getAccessKey( ) );
        uif.setSecretKey( k.getSecretKey( ) );
      }
    }
    return uif;
  }
  
  public static void fromWeb( UserInfoWeb uif ) throws AuthException {
    Account account = Accounts.lookupAccountByName( uif.getAccountName( ) );
    User user = account.lookupUserByName( uif.getUserName( ) );
    if ( !user.getName( ).equals( uif.getUserName( ) ) ) {
      user.setName( uif.getUserName( ) );
    }
    if ( !user.getPasswordExpires( ).equals( uif.getPasswordExpires( ) ) ) {
      user.setPasswordExpires( uif.getPasswordExpires( ) );
    }
    Map<String, String> info = Maps.newHashMap( );
    info.putAll( user.getInfo( ) );
    if ( uif.getRealName( ) != null ) {
      info.put( USER_INFO_FULLNAME, uif.getRealName( ) );
    }
    if ( uif.getEmail( ) != null ) {
      info.put( USER_INFO_EMAIL, uif.getEmail( ) );
    }
    if ( uif.getTelephoneNumber( ) != null ) {
      info.put( USER_INFO_PHONE, uif.getTelephoneNumber( ) );
    }
    if ( uif.getAffiliation( ) != null ) {
      info.put( USER_INFO_AFFILIATION, uif.getAffiliation( ) );
    }
    if ( uif.getProjectDescription( ) != null ) {
      info.put( USER_INFO_PROJECT_DESC, uif.getProjectDescription( ) );
    }
    if ( uif.getProjectPIName( ) != null ) {
      info.put( USER_INFO_PROJECT_PI, uif.getProjectPIName( ) );
    }
    user.setInfo( info );
    if ( uif.getPassword( ) != null ) {
      user.setPassword( uif.getPassword( ) );
    }
  }
  
}
