package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import org.hibernate.annotations.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.User.RegistrationStatus;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Database entity for a user.
 * 
 * @author wenye
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_user" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class UserEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;
  
  // The User ID the user facing group id which conforms to length and character restrictions per spec.
  @Column( name = "auth_user_id_external" )
  String userId;

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
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "user" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<AccessKeyEntity> keys;
  
  // List of certificates
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "user" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<CertificateEntity> certificates;
  
  // Customizable user info in key-value pairs
  @ElementCollection
  @CollectionTable( name = "auth_user_info_map" )
  @MapKeyColumn( name = "auth_user_info_key" )
  @Column( name = "auth_user_info_value" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  Map<String, String> info;
  
  // User's groups
  @ManyToMany( fetch = FetchType.LAZY, mappedBy="users" ) // not owning side
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
  
  @PrePersist
  public void generateOnCommit() {
    if( this.userId == null ) {/** NOTE: first time that AKey is committed it needs to generate its own ID (i.e., not the database id), do this at commit time and generate if null **/
      this.userId = Crypto.generateQueryId();
    }
  }
  
  public static UserEntity newInstanceWithUserId( final String userId ) {
    UserEntity u = new UserEntity( );
    u.userId = userId;
    return u;
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
    sb.append( "UserID=" ).append( this.getUserId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) ).append( ", " );
    sb.append( "path=" ).append( this.getPath( ) ).append( ", " );
    sb.append( "enabled=" ).append( this.isEnabled( ) ).append( ", " );
    sb.append( "regStat=" ).append( this.getRegistrationStatus( ) ).append( ", " );
    sb.append( "passwordExpires=" ).append( this.getPasswordExpires( ) );
    sb.append( ")" );
    return sb.toString( );
  }

  public String getName( ) {
    return this.name;
  }
  
  public void setName( String name ) {
    this.name = name;
  }
  
  public String getPath( ) {
    return this.path;
  }
  
  public void setPath( String path ) {
    this.path = path;
  }
  
  public RegistrationStatus getRegistrationStatus( ) {
    return this.regStat;
  }
  
  public void setRegistrationStatus( RegistrationStatus regStat ) {
    this.regStat = regStat;
  }
  
  public Boolean isEnabled( ) {
    return this.enabled;
  }
  
  public void setEnabled( Boolean enabled ) {
    this.enabled = enabled;
  }
  
  public String getToken( ) {
    return this.token;
  }
  
  public void setToken( String token ) {
    this.token = token;
  }
  
  public String getConfirmationCode( ) {
    return this.confirmationCode;
  }
  
  public void setConfirmationCode( String confirmationCode ) {
    this.confirmationCode = confirmationCode;
  }
  
  public String getPassword( ) {
    return this.password;
  }
  
  public void setPassword( String password ) {
    this.password = password;
  }
  
  public Long getPasswordExpires( ) {
    return this.passwordExpires;
  }
  
  public void setPasswordExpires( Long passwordExpires ) {
    this.passwordExpires = passwordExpires;
  }
  
  public List<AccessKeyEntity> getKeys( ) {
    return this.keys;
  }
  
  public List<CertificateEntity> getCertificates( ) {
    return this.certificates;
  }
  
  public Map<String, String> getInfo( ) {
    return this.info;
  }
  
  public List<GroupEntity> getGroups( ) {
    return this.groups;
  }

  public String getUserId( ) {
    return this.userId;
  }
  
}
