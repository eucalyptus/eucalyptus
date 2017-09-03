/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.persist.entities;

import static com.eucalyptus.upgrade.Upgrades.Version.v4_2_0;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.AuxiliaryDatabaseObject;
import com.eucalyptus.entities.AuxiliaryDatabaseObjects;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.collect.Sets;
import groovy.sql.Sql;

/**
 * Database group entity.
 */
@Entity
@AuxiliaryDatabaseObjects({
    @AuxiliaryDatabaseObject(
        dialect = "org.hibernate.dialect.PostgreSQLDialect",
        create = "create index auth_group_users_user_idx on ${schema}.auth_group_has_users ( auth_user_id )",
        drop = "drop index if exists ${schema}.auth_group_users_user_idx"
    ),
    @AuxiliaryDatabaseObject(
        dialect = "org.hibernate.dialect.PostgreSQLDialect",
        create = "create index auth_group_users_group_idx on ${schema}.auth_group_has_users ( auth_group_id )",
        drop = "drop index if exists ${schema}.auth_group_users_group_idx"
    ),
})
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_group", indexes = {
    @Index( name = "auth_group_name_idx", columnList = "auth_group_name" ),
    @Index( name = "auth_group_owning_account_idx", columnList = "auth_group_owning_account" )
} )
public class GroupEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // The Group ID: the user facing group id which conforms to length and character restrictions per spec.
  @Column( name = "auth_group_id_external", unique = true )
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
  
  @Column( name = "auth_group_unique_name", unique = true )
  String uniqueName;
  
  // Users in the group
  @ManyToMany( fetch = FetchType.LAZY )
  @JoinTable( name = "auth_group_has_users", joinColumns = { @JoinColumn( name = "auth_group_id" ) }, inverseJoinColumns = @JoinColumn( name = "auth_user_id" ) )
  Set<UserEntity> users;

  // Policies for the group
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "group" )
  Set<PolicyEntity> policies;

  // Attached policies for the group
  @NotFound( action = NotFoundAction.IGNORE )
  @JoinTable( name = "auth_group_attached_policies",
      joinColumns =        @JoinColumn( name = "auth_group_id" ),
      inverseJoinColumns = @JoinColumn( name = "auth_managed_policy_id" ) )
  @ManyToMany
  Set<ManagedPolicyEntity> attachedPolicies;

  // The owning account
  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "auth_group_owning_account" )
  AccountEntity account;
  
  public GroupEntity( ) {
    this.users = Sets.newHashSet( );
    this.policies = Sets.newHashSet( );
  }
  
  public GroupEntity( final String accountId, final String name ) {
    this( );
    this.name = name;
    this.uniqueName = String.format("%s:%s", accountId, name);
  }
  
  public GroupEntity( Boolean userGroup ) {
    this( );
    this.userGroup = userGroup;
  }

  @PrePersist
  public void generateOnCommit() {
    if( this.groupId == null ) {
      this.groupId = Identifiers.generateIdentifier( "AGP" );
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
    if (this.uniqueName!=null && this.uniqueName.indexOf(":")>0) {
      final String[] tokens = this.uniqueName.split(":");
      final String accountId = tokens[0];
      this.uniqueName = String.format("%s:%s", accountId, name);
    }
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
  
  public Set<PolicyEntity> getPolicies( ) {
    return this.policies;
  }

  public Set<ManagedPolicyEntity> getAttachedPolicies( ) {
    return attachedPolicies;
  }

  public Set<UserEntity> getUsers( ) {
    return this.users;
  }

  public String getGroupId( ) {
    return this.groupId;
  }
  
  @Upgrades.PreUpgrade( value = Euare.class, since = v4_2_0 )
  public static class GroupPreUpgrade420 implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( GroupPreUpgrade420.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection("eucalyptus_auth");
        sql.execute( "alter table auth_group add constraint uk_4ns2wloivviwxjbx7jeg5ip31 unique ( auth_group_id_external )" );
        sql.execute( "create index auth_group_users_user_idx on auth_group_has_users ( auth_user_id )" );
        sql.execute( "create index auth_group_users_group_idx on auth_group_has_users ( auth_group_id )" );
      } catch (Exception ex) {
        logger.error( "Error creating group indexes", ex );
      } finally {
        if (sql != null) {
          sql.close();
        }
      }
      return true;
    }
  }
}
