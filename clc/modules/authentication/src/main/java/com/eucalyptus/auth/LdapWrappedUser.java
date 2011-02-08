package com.eucalyptus.auth;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.crypto.Digest;
import com.eucalyptus.auth.ldap.EntryNotFoundException;
import com.eucalyptus.auth.ldap.LdapException;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.google.common.collect.Lists;

public class LdapWrappedUser implements User, WrappedUser {
  private static Logger LOG = Logger.getLogger( LdapWrappedUser.class );
  
  private UserEntity    user;
  private UserInfo      userInfo;
  
  public LdapWrappedUser( UserEntity user, UserInfo userInfo ) {
    this.user = user;
    this.userInfo = userInfo;
  }
  
  @Override
  public boolean checkToken( String testToken ) {
    String token = this.user.getToken( );
    boolean ret = token.equals( testToken );
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setToken( Crypto.generateSessionToken( this.user.getName( ) ) );
      EucaLdapHelper.updateUser( search, null );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
    return ret;
  }
  
  @Override
  public User getDelegate( ) {
    return this.user;
  }
  
  @Override
  public String getPassword( ) {
    return this.user.getPassword( );
  }
  
  @Override
  public String getToken( ) {
    return this.user.getToken( );
  }
  
  @Override
  public Boolean isAdministrator( ) {
    return this.user.isAdministrator( );
  }
  
  @Override
  public Boolean isEnabled( ) {
    return this.user.isEnabled( );
  }
  
  @Override
  public void setAdministrator( Boolean admin ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setAdministrator( admin );
      EucaLdapHelper.updateUser( search, null );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public void setEnabled( Boolean enabled ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setEnabled( enabled );
      EucaLdapHelper.updateUser( search, null );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public void setPassword( String password ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setPassword( password );
      EucaLdapHelper.updateUser( search, null );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public String getName( ) {
    return this.user.getName( );
  }
  
  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    return this.user.getAllX509Certificates( );
  }
  
  @Override
  public X509Certificate getX509Certificate( ) {
    return this.user.getX509Certificate( );
  }
  
  @Override
  public void revokeX509Certificate( ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setCertificates( user.getCertificates( ) );
      search.revokeX509Certificate( );
      EucaLdapHelper.updateUserCertificates( search );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public void setX509Certificate( X509Certificate cert ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setCertificates( user.getCertificates( ) );
      search.setX509Certificate( cert );
      EucaLdapHelper.updateUser( search, null );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public BigInteger getNumber( ) {
    try {
      return new BigInteger( Digest.MD5.get( ).digest( this.user.getName( ).getBytes( "UTF8" ) ) );
    } catch ( UnsupportedEncodingException e ) {
      return new BigInteger( Digest.MD5.get( ).digest( this.user.getName( ).getBytes( ) ) );
    }
  }
  
  @Override
  public String getQueryId( ) {
    return this.user.getQueryId( );
  }
  
  @Override
  public String getSecretKey( ) {
    return this.user.getSecretKey( );
  }
  
  @Override
  public void revokeSecretKey( ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setSecretKey( this.user.getSecretKey( ) );
      EucaLdapHelper.deleteUserAttribute( search );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public void setQueryId( String queryId ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setQueryId( queryId );
      EucaLdapHelper.updateUser( search, null );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public void setSecretKey( String secretKey ) {
    try {
      UserEntity search = new UserEntity( this.user.getName( ) );
      search.setSecretKey( secretKey );
      EucaLdapHelper.updateUser( search, null );
      LdapCache.getInstance( ).removeUser( this.user.getName( ) );
    } catch ( EntryNotFoundException e ) {
      LOG.error( e, e );
    } catch ( LdapException e ) {
      LOG.error( e, e );
    }
  }
  
  @Override
  public UserInfo getUserInfo( ) throws NoSuchUserException {
    return userInfo;
  }
  
  /**
   * Just to make CompositeHelper.goovy happy.
   * 
   * @return
   */
  public Boolean getAdministrator( ) {
    return this.user.isAdministrator( );
  }
  
  /**
   * Just to make CompositeHelper.goovy happy.
   * 
   * @return
   */
  public Boolean getEnabled( ) {
    return this.user.isEnabled( );
  }
  
  public List<String> getEucaGroupIds( ) {
    return this.user.getEucaGroupIds( );
  }
  
  public List<X509Cert> getCertificates( ) {
    return this.user.getCertificates( );
  }
  
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "LdapWrappedUser [ " );
    sb.append( "user = " ).append( user ).append( ", " );
    sb.append(" userInfo = ").append( userInfo ).append( ", " );
    sb.append( "]" );
    return sb.toString( );
  }

  @Override
  public Boolean isSystem( ) {
    return false;
  }
}
