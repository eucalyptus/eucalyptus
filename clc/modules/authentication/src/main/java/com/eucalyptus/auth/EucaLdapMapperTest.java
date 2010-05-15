package com.eucalyptus.auth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
import com.eucalyptus.auth.principal.AvailabilityZonePermission;
import com.eucalyptus.auth.principal.BaseAuthorization;

public class EucaLdapMapperTest {
  
  /**
   * @param args
   * @throws NamingException 
   */
  @SuppressWarnings( "unchecked" )
  public static void main( String[] args ) throws NamingException {
    BasicConfigurator.configure(new FileAppender());
    BasicConfigurator.configure(new ConsoleAppender(new SimpleLayout()));
        
    UserEntity user = new UserEntity( );
    user.setName( "jsmith" );
    user.setQueryId( "qidb64" );
    user.setSecretKey( "keyb64" );
    user.setPassword( "password" );
    user.setAdministrator( true );
    user.setEnabled( true );
    user.setToken( "tokenb64" );

    List<X509Cert> certs = new ArrayList<X509Cert>( );
    X509Cert cert = new X509Cert( );
    cert.setAlias( "cert1" );
    cert.setPemCertificate( "certb64" );
    //cert.setRevoked( false );
    certs.add( cert );
    /*
    cert = new X509Cert( );
    cert.setAlias( "cert2" );
    cert.setPemCertificate( "certb64" );
    cert.setRevoked( true );
    certs.add( cert );
    */
    user.setCertificates( certs );

    UserInfo info = new UserInfo( );
    info.setUserName( "jsmith" );
    info.setEmail( "jsmith@foobar.com" );
    info.setTelephoneNumber( "123" );
    info.setAffiliation( "sales" );
    info.setApproved( true );
    info.setConfirmed( true );
    info.setPasswordExpires( 1234L );
    info.setConfirmationCode( "code" );
    
    Attributes attrs = EucaLdapMapper.entityToAttribute( user, info );
    printAttributes( attrs );
    
    String password = ( String ) attrs.get( LdapConstants.USER_PASSWORD ).get( );
    System.out.println( "Password is " + password );
    attrs.get( LdapConstants.USER_PASSWORD ).set( 0, password.getBytes( ) );
    
    List<Object> result = EucaLdapMapper.attributeToEntity( attrs, ( Object ) UserEntity.class, ( Object ) UserInfo.class );
    if ( result.size( ) < 2 ) {
      System.out.println( "Not enough outpout objects" );
      return;
    }
    user = ( UserEntity ) result.get( 0 );
    info = ( UserInfo ) result.get( 1 );
    System.out.println( "UserEntity.name = " + user.getName( ) );
    System.out.println( "UserEntity.queryId = " + user.getQueryId( ) );
    System.out.println( "UserEntity.secretKey = " + user.getSecretKey( ) );
    System.out.println( "UserEntity.password = " + user.getPassword( ) );
    System.out.println( "UserEntity.administrator = " + user.isAdministrator( ) );
    System.out.println( "UserEntity.enabled = " + user.isEnabled( ) );
    System.out.println( "UserEntity.token = " + user.getToken( ) );
    for ( X509Cert certificate : user.getCertificates( ) ) {
      System.out.println( "UserEntity.certificates = " + certificate.getAlias( ) + " " + certificate.getRevoked( ) + " " + certificate.getPemCertificate( ) );
    }
    System.out.println( "UserInfo.userName = " + info.getUserName( ) );
    System.out.println( "UserInfo.email = " + info.getEmail( ) );
    System.out.println( "UserInfo.realName = " + info.getRealName( ) );
    System.out.println( "UserInfo.telephoneNumber = " + info.getTelephoneNumber( ) );
    System.out.println( "UserInfo.affiliation = " + info.getAffiliation( ) );
    System.out.println( "UserInfo.projectDescription = " + info.getProjectDescription( ) );
    System.out.println( "UserInfo.projectPIName = " + info.getProjectPIName( ) );
    System.out.println( "UserInfo.approved = " + info.isApproved( ) );
    System.out.println( "UserInfo.confirmed = " + info.confirmed );
    System.out.println( "UserInfo.passwordExpires = " + info.getPasswordExpires( ) );
    System.out.println( "UserInfo.confirmationCode = " + info.getConfirmationCode( ) );
    
    GroupEntity group = new GroupEntity( );
    group.setName( "group1" );
    List<UserEntity> userList = new ArrayList<UserEntity>( );
    userList.add( new UserEntity( "user1" ) );
    userList.add( new UserEntity( "user2" ) );
    group.setUserList( userList );
    List<BaseAuthorization> authList = new ArrayList<BaseAuthorization>( );
    authList.add( new AvailabilityZonePermission( "zone1" ) );
    authList.add( new AvailabilityZonePermission( "zone2" ) );
    group.setAuthList( authList );
    
    attrs = EucaLdapMapper.entityToAttribute( group );
    printAttributes( attrs );
    
    result = EucaLdapMapper.attributeToEntity( attrs, GroupEntity.class );
    group = ( GroupEntity ) result.get( 0 );
    System.out.println( "GroupEntity.name = " + group.getName( ) );
    for ( UserEntity userEntity : group.getUserList( ) ) {
      System.out.println( "GroupEntity.userList = " + userEntity.getName( ) );
    }
    for ( BaseAuthorization auth : group.getAuthList( ) ) {
      System.out.println( "GroupEntity.authList = " + auth.getValue( ) );
    }

    String[] names = EucaLdapMapper.getFieldAttributeNames( GroupEntity.class, "authList" );
    for ( String name : names ) {
      System.out.println( "UserEntity.certificates attributes = " + name );
    }
    
    System.out.println( " -------------------------------- " );
    user = new UserEntity( "user" );
    certs = new ArrayList<X509Cert>( );
    cert = new X509Cert( );
    cert.setAlias( "cert1" );
    cert.setPemCertificate( "certb64" );
    cert.setRevoked( true );
    certs.add( cert );
    user.setCertificates( certs );
    attrs = EucaLdapMapper.entityToAttribute( user, null );
    printAttributes( attrs );
  }
  
  private static void printAttributes( Attributes attrs ) throws NamingException {
    NamingEnumeration<Attribute> allAttrs = ( NamingEnumeration<Attribute> ) attrs.getAll( );
    while ( allAttrs.hasMore( ) ) {
      Attribute attr = allAttrs.next( );
      System.out.print( attr.getID( ) + " = " );
      NamingEnumeration<Object> allValues = ( NamingEnumeration<Object> ) attr.getAll( );
      while ( allValues.hasMore( ) ) {
        Object value = allValues.next( );
        System.out.print( value );
        System.out.print( "," );
      }
      System.out.println( "" );
    }
  }
}