package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;

/**
 * Database group entity.
 * 
 * @author wenye
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class GroupEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // The Group ID: the user facing group id which conforms to length and character restrictions per spec.
  @Column( name = "auth_group_id_external" )
  String groupId;

  // Group name, not unique since different accounts can have the same group name
  @Column( name = "auth_group_name" )
  String name;
  
  // Group path (prefix to organize group name space, see AWS spec)
  @Column( name = "auth_group_path" )
  String path;
  
  // Indicates if this group is a special user group
  @Column( name = "auth_group_user_group" )
  Boolean userGroup;
  
  // Users in the group
  @ManyToMany( fetch = FetchType.LAZY )
  @JoinTable( name = "auth_group_has_users", joinColumns = { @JoinColumn( name = "auth_group_id" ) }, inverseJoinColumns = @JoinColumn( name = "auth_user_id" ) )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<UserEntity> users;

  // Policies for the group
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "group" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<PolicyEntity> policies;
  
  // The owning account
  @ManyToOne
  @JoinColumn( name = "auth_group_owning_account" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  AccountEntity account;
  
  public GroupEntity( ) {
    this.users = Lists.newArrayList( );
    this.policies = Lists.newArrayList( );
  }
  
  public GroupEntity( String name ) {
    this( );
    this.name = name;
  }
  
  public GroupEntity( Boolean userGroup ) {
    this( );
    this.userGroup = userGroup;
  }

  public static GroupEntity newInstanceWithGroupId( final String id ) {
    GroupEntity g = new GroupEntity( );
    g.groupId = id;
    return g;
  }

  @PrePersist
  public void generateOnCommit() {
    if( this.groupId == null ) {
      this.groupId = Crypto.generateQueryId();
    }
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    GroupEntity that = ( GroupEntity ) o;    
    if ( !name.equals( that.name ) ) return false;
    
    return true;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Group(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) ).append( ", " );
    sb.append( "path=" ).append( this.getPath( ) ).append( ", " );
    sb.append( "userGroup=" ).append( this.isUserGroup( ) );
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

  public AccountEntity getAccount( ) {
    return this.account;
  }
  
  public void setAccount( AccountEntity account ) {
    this.account = account;
  }
  
  public Boolean isUserGroup( ) {
    return this.userGroup;
  }
  
  public void setUserGroup( Boolean userGroup ) {
    this.userGroup = userGroup;
  }
  
  public List<PolicyEntity> getPolicies( ) {
    return this.policies;
  }
  
  public List<UserEntity> getUsers( ) {
    return this.users;
  }

  public String getGroupId( ) {
    return this.groupId;
  }
  
}
