/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database entity for a role.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_role" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class RoleEntity extends AbstractPersistent implements Serializable {

  private static final long serialVersionUID = 1L;

  // The Role ID the user facing role id which conforms to length and character restrictions per spec.
  @Column( name = "auth_role_id_external", nullable = false, updatable = false )
  private String roleId;

  // Role name
  @Column( name = "auth_role_name", nullable = false)
  private String name;

  // Role path (prefix to organize role name space)
  @Column( name = "auth_role_path", nullable = false )
  private String path;

  @Column( name="auth_role_secret", nullable = false )
  private String secret;

  @OneToOne( cascade = CascadeType.ALL, optional = false, orphanRemoval = true )
  @JoinColumn( name = "auth_role_assume_role_policy_id", nullable = false )
  private PolicyEntity assumeRolePolicy;

  @OneToMany( cascade = CascadeType.ALL, mappedBy = "role" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private List<PolicyEntity> policies;

  @ManyToOne
  @JoinColumn( name = "auth_role_owning_account", nullable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private AccountEntity account;

  @Column( name = "auth_role_unique_name", unique = true, nullable = false )
  private String uniqueName;

  public RoleEntity( ) {
  }

  public RoleEntity( final String name ) {
    this( );
    this.name = name;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId( final String roleId ) {
    this.roleId = roleId;
  }

  public String getName() {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath( final String path ) {
    this.path = path;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret( final String secret ) {
    this.secret = secret;
  }

  public PolicyEntity getAssumeRolePolicy() {
    return assumeRolePolicy;
  }

  public void setAssumeRolePolicy( final PolicyEntity assumeRolePolicy ) {
    this.assumeRolePolicy = assumeRolePolicy;
  }

  public List<PolicyEntity> getPolicies() {
    return policies;
  }

  public void setPolicies( final List<PolicyEntity> policies ) {
    this.policies = policies;
  }

  public AccountEntity getAccount() {
    return account;
  }

  public void setAccount( final AccountEntity account ) {
    this.account = account;
  }

  @PrePersist
  @PreUpdate
  public void generateOnCommit() {
    if( this.roleId == null ) {
      this.roleId = Crypto.generateAlphanumericId( 21, "ARO" );
    }
    if ( this.secret == null ) {
      this.secret = Crypto.generateSecretKey();
    }
    this.uniqueName = account.getAccountNumber() + ":" + name;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Role(" );
    sb.append( "id=" ).append( this.getId() ).append( ", " );
    sb.append( "roleId=" ).append( this.getRoleId() ).append( ", " );
    sb.append( "name=" ).append( this.getName() ).append( ", " );
    sb.append( "path=" ).append( this.getPath() ).append( ", " );
    sb.append( ")" );
    return sb.toString( );
  }
}
