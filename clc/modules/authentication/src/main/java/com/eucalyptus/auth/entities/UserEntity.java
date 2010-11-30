package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Database entity for a user.
 * 
 * @author wenye
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_user" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class UserEntity extends AbstractPersistent implements User, Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  @Transient
  private static Logger LOG = Logger.getLogger( UserEntity.class );
  
  // User name
  @Column( name = "auth_user_name" )
  String name;
  
  // User path (prefix to organize user name space, see AWS spec)
  @Column( name = "auth_user_path" )
  String path;
  
  // The progress of user registration process: REGISTERED -> APPROVED -> CONFIRMED (and enabled)
  @Enumerated( EnumType.STRING )
  @Column( name = "auth_user_reg_stat" )
  RegistrationStatus regStat;

  // Flag to control the activeness of a user.
  @Column( name = "auth_user_is_enabled" )
  Boolean enabled;
  
  // Web session token
  @Column( name = "auth_user_token" )
  String token;

  // User registration confirmation code
  @Column( name = "auth_user_confirmation_code" )
  String confirmationCode;
  
  // Web login password
  @Column( name = "auth_user_password" )
  String password;

  // Time when password expires
  @Column( name = "auth_user_password_expires" )
  Long passwordExpires;
  
  // List of secret keys
  @OneToMany( cascade = { CascadeType.ALL } ) // unidirectional
  @JoinTable( name = "auth_user_has_keys",
              joinColumns = { @JoinColumn( name = "auth_user_id" ) },
              inverseJoinColumns = { @JoinColumn( name = "auth_access_key_id") } )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<AccessKeyEntity> keys;
  
  // List of certificates
  @OneToMany( cascade = { CascadeType.ALL } ) // unidirectional
  @JoinTable( name = "auth_user_has_certs",
              joinColumns = { @JoinColumn( name = "auth_user_id" ) },
              inverseJoinColumns = { @JoinColumn( name = "auth_cert_id") } )
  List<CertificateEntity> certificates;
  
  // Customizable user info in key-value pairs
  @ElementCollection
  @CollectionTable( name = "auth_user_info_map" )
  @MapKeyColumn( name = "auth_user_info_key" )
  @Column( name = "auth_user_info_value" )
  Map<String, String> info;
  
  // User's groups
  @ManyToMany( fetch = FetchType.EAGER, mappedBy="users" ) // not owning side
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<GroupEntity> groups;
  
  public UserEntity( ) {
    this.keys = Lists.newArrayList( );
    this.certificates = Lists.newArrayList( );
    this.info = Maps.newHashMap( );
    this.groups = Lists.newArrayList( );
  }

  public UserEntity( String name ) {
    this( );
    this.name = name;
  }
  
  public UserEntity( Boolean enabled ) {
    this( );
    this.enabled = enabled;
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    UserEntity that = ( UserEntity ) o;    
    if ( !name.equals( that.name ) ) return false;
    
    return true;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "User(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) ).append( ", " );
    sb.append( "path=" ).append( this.getPath( ) ).append( ", " );
    sb.append( "enabled=" ).append( this.isEnabled( ) ).append( ", " );
    sb.append( "regStat=" ).append( this.getRegistrationStatus( ) ).append( ", " );
    sb.append( "password=" ).append( this.getPassword( ) ).append( ", " );
    sb.append( "passwordExpires=" ).append( this.getPasswordExpires( ) ).append( ", " );
    sb.append( "token=" ).append( this.getToken( ) ).append( ", " );
    sb.append( "confirmationCode=" ).append( this.getConfirmationCode( ) ).append( ", " );
    sb.append( "info=" ).append( this.getInfoMap( ) ).append( ", " );
    sb.append( "keys=" ).append( this.keys ).append( ", " ).append( ", " );
    sb.append( "certificates=" ).append( this.certificates ).append( ", " );
    sb.append( "groups=[" );
    for ( GroupEntity g : this.groups ) {
      sb.append( g.getName( ) ).append( ' ' );
    }
    sb.append( ']' );
    sb.append( ")" );
    return sb.toString( );
  }
  
  @Override
  public String getUserId( ) {
    return this.getId( );
  }
  
  @Override
  public String getName( ) {
    return this.name;
  }
  
  @Override
  public void setName( String name ) {
    this.name = name;
  }

  @Override
  public X509Certificate getX509Certificate( String id ) {
    for ( CertificateEntity cert : this.certificates ) {
      if ( cert.getId( ).equals( id ) ) {
        return X509CertHelper.toCertificate( cert.getPem( ) );
      }
    }
    return null;
  }

  @Override
  public List<X509Certificate> getAllX509Certificates( ) {
    List<X509Certificate> certs = Lists.newArrayList( );
    for ( CertificateEntity cert : this.certificates ) {
      certs.add( X509CertHelper.toCertificate( cert.getPem( ) ) );
    }
    return certs;
  }

  @Override
  public void addX509Certificate( X509Certificate cert ) {
    CertificateEntity newCert = new CertificateEntity( X509CertHelper.fromCertificate( cert ) );
    newCert.setRevoked( false );
    newCert.setActive( true );
    this.certificates.add( newCert );
  }

  @Override
  public void activateX509Certificate( String id ) {
    for ( CertificateEntity cert : this.certificates ) {
      if ( cert.getId( ).equals( id ) ) {
        cert.setActive( true );
      }
    }
  }

  @Override
  public void deactivateX509Certificate( String id ) {
    for ( CertificateEntity cert : this.certificates ) {
      if ( cert.getId( ).equals( id ) ) {
        cert.setActive( false );
      }
    }
  }

  @Override
  public void revokeX509Certificate( String id ) {
    for ( CertificateEntity cert : this.certificates ) {
      if ( cert.getId( ).equals( id ) ) {
        cert.setRevoked( true );
      }
    }
  }

  @Override
  public BigInteger getNumber( ) {
    return new BigInteger( this.getId( ), 16 );
  }

  @Override
  public String getSecretKey( String id ) {
    for ( AccessKeyEntity key : this.keys ) {
      if ( key.getId( ).equals( id ) ) {
        return key.getKey( );
      }
    }
    return null;
  }

  @Override
  public void addSecretKey( String key ) {
    AccessKeyEntity newKey = new AccessKeyEntity( key );
    newKey.setActive( true );
    this.keys.add( newKey );
  }

  @Override
  public void activateSecretKey( String id ) {
    for ( AccessKeyEntity key : this.keys ) {
      if ( key.getId( ).equals( id ) ) {
        key.setActive( true );
        return;
      }
    }
  }

  @Override
  public void deactivateSecretKey( String id ) {
    for ( AccessKeyEntity key : this.keys ) {
      if ( key.getId( ).equals( id ) ) {
        key.setActive( false );
        return;
      }
    }
  }

  @Override
  public void revokeSecretKey( String id ) {
    Iterator<AccessKeyEntity> it = this.keys.iterator( );
    while ( it.hasNext( ) ) {
      if ( it.next( ).getId( ).equals( id ) ) {
        it.remove( );
      }
    }
  }

  @Override
  public User getDelegate( ) {
    return this;
  }

  @Override
  public String getPath( ) {
    return this.path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  @Override
  public RegistrationStatus getRegistrationStatus( ) {
    return this.regStat;
  }

  @Override
  public void setRegistrationStatus( RegistrationStatus stat ) {
    this.regStat = stat;
  }

  @Override
  public Boolean isEnabled( ) {
    return this.enabled;
  }

  @Override
  public void setEnabled( Boolean enabled ) {
    this.enabled = enabled;
  }

  public String getToken( ) {
    return this.token;
  }

  @Override
  public boolean checkToken( String testToken ) {
    return this.getToken( ).equals( testToken );
  }

  public void setToken( String token ) {
    this.token = token;
  }

  @Override
  public String getConfirmationCode( ) {
    return this.confirmationCode;
  }
  
  public void setConfirmationCode( String code ) {
    this.confirmationCode = code;
  }
  
  @Override
  public Long getPasswordExpires( ) {
    return this.passwordExpires;
  }
  
  @Override
  public void setPasswordExpires( Long time ) {
    this.passwordExpires = time;
  }
  
  @Override
  public String getPassword( ) {
    return this.password;
  }

  @Override
  public void setPassword( String password ) {
    this.password = password;
  }

  @Override
  public String getInfo( String key ) {
    return this.info.get( key );
  }

  @Override
  public void setInfo( String key, String value ) {
    this.info.put( key, value );
  }

  @Override
  public Map<String, String> getInfoMap( ) {
    return this.info;
  }
  
  public void addGroup( GroupEntity group ) {
    this.groups.add( group );
  }

  @Override
  public List<? extends Group> getGroups( ) {
    return this.groups;
  }
  
  public List<AccessKeyEntity> getAccessKeys( ) {
    return this.keys;
  }
  
  public List<CertificateEntity> getCertificates( ) {
    return this.certificates;
  }

  @Override
  public void setInfo( Map<String, String> newInfo ) throws AuthException {
    this.info.putAll( newInfo );
  }
  
  @Override
  public Account getAccount( ) {
    if ( this.groups.size( ) < 1 ) {
      throw new RuntimeException( "Unexpected group number of the user" );
    }
    return this.groups.get( 0 ).account;
  }

  @Override
  public String lookupX509Certificate( X509Certificate cert ) {
    String pem = X509CertHelper.fromCertificate( cert );
    for ( CertificateEntity ce : this.certificates ) {
      if ( ce.getPem( ).equals( pem ) ) {
        return ce.getId( );
      }
    }
    return null;
  }

  @Override
  public String lookupSecretKeyId( String key ) {
    for ( AccessKeyEntity k : this.keys ) {
      if ( k.getKey( ).equals( key ) ) {
        return k.getId( );
      }
    }
    return null;
  }

  @Override
  public boolean isSystemAdmin( ) {
    return SYSTEM_ADMIN_ACCOUNT_NAME.equals( this.getAccount( ).getName( ) );
  }

  @Override
  public String getFirstActiveSecretKeyId( ) {
    for ( AccessKeyEntity k : this.keys ) {
      if ( k.isActive( ) ) {
        return k.getId( );
      }
    }
    return null;
  }

  @Override
  public List<String> getActiveX509CertificateIds( ) {
    List<String> results = Lists.newArrayList( );
    for ( CertificateEntity c : this.certificates ) {
      if ( !c.isRevoked( ) && c.isActive( ) ) {
        results.add( c.getId( ) );
      }
    }
    return results;
  }

  @Override
  public List<String> getActiveSecretKeyIds( ) {
    List<String> results = Lists.newArrayList( );
    for ( AccessKeyEntity k : this.keys ) {
      if ( k.isActive( ) ) {
        results.add( k.getId( ) );
      }
    }
    return results;
  }

  @Override
  public List<String> getInactiveX509CertificateIds( ) {
    List<String> results = Lists.newArrayList( );
    for ( CertificateEntity c : this.certificates ) {
      if ( !c.isRevoked( ) && !c.isActive( ) ) {
        results.add( c.getId( ) );
      }
    }
    return results;
  }

  @Override
  public List<String> getInactiveSecretKeyIds( ) {
    List<String> results = Lists.newArrayList( );
    for ( AccessKeyEntity k : this.keys ) {
      if ( !k.isActive( ) ) {
        results.add( k.getId( ) );
      }
    }
    return results;
  }

  @Override
  public boolean isAccountAdmin( ) {
    return ACCOUNT_ADMIN_USER_NAME.equals( this.getName( ) );
  }
  
}
