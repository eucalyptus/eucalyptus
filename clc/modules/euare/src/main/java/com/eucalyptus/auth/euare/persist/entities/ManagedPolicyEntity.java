/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.euare.persist.entities;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.google.common.base.MoreObjects;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_managed_policy" )
public class ManagedPolicyEntity extends AbstractPersistent {

  private static final long serialVersionUID = 1L;

  // The policy name
  @Column( name = "auth_policy_name", nullable = false)
  private String name;

  // The policy path
  @Column( name = "auth_policy_path", nullable = false, length = 512  )
  private String path;

  // The policy description
  @Column( name = "auth_policy_desc", length = 1000 )
  private String description;

  // The Policy ID the user facing policy id which conforms to length and character restrictions per spec.
  @Column( name = "auth_policy_id_external", nullable = false, updatable = false, unique = true )
  private String policyId;

  @Column( name = "auth_policy_version" )
  private String policyVersion;

  // The original policy text in JSON
  @Column( name = "auth_policy_text", nullable = false )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  private String text;

  @ManyToOne
  @JoinColumn( name = "auth_policy_owning_account", nullable = false )
  private AccountEntity account;

  @ManyToMany( mappedBy = "attachedPolicies" )
  private List<GroupEntity> groups;

  @ManyToMany( mappedBy = "attachedPolicies" )
  private List<RoleEntity> roles;

  @ManyToMany( mappedBy = "attachedPolicies" )
  private List<UserEntity> users;

  @Column( name = "auth_policy_unique_name", unique = true, nullable = false )
  private String uniqueName;

  protected ManagedPolicyEntity( ) {
  }

  public ManagedPolicyEntity( final String policyName ) {
    setName( policyName );
  }

  public static EntityRestriction<ManagedPolicyEntity> exampleWithName(
      @Nonnull final String accountNumber,
      @Nonnull final String name
  ) {
    return Entities.restriction( ManagedPolicyEntity.class )
        .equal( ManagedPolicyEntity_.uniqueName, buildUniqueName( accountNumber, name ) )
        .build( );
  }

  public static EntityRestriction<ManagedPolicyEntity> exampleWithAttachment(
      @Nullable final Boolean attached
  ) {
    return MoreObjects.firstNonNull( attached, Boolean.FALSE ) ?
        Entities.restriction( ManagedPolicyEntity.class ).any(
          Entities.restriction( ManagedPolicyEntity.class ).isNotEmpty( ManagedPolicyEntity_.groups ).build( ),
          Entities.restriction( ManagedPolicyEntity.class ).isNotEmpty( ManagedPolicyEntity_.roles ).build( ),
          Entities.restriction( ManagedPolicyEntity.class ).isNotEmpty( ManagedPolicyEntity_.users ).build( )
        ).build( ) :
        Entities.restriction( ManagedPolicyEntity.class ).build( );
  }

  public AccountEntity getAccount( ) {
    return account;
  }

  public void setAccount( final AccountEntity account ) {
    this.account = account;
  }

  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public String getPath( ) {
    return path;
  }

  public void setPath( final String path ) {
    this.path = path;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( final String description ) {
    this.description = description;
  }

  public String getPolicyId( ) {
    return policyId;
  }

  public void setPolicyId( final String policyId ) {
    this.policyId = policyId;
  }

  public String getPolicyVersion( ) {
    return policyVersion;
  }

  public void setPolicyVersion( final String policyVersion ) {
    this.policyVersion = policyVersion;
  }

  public String getText( ) {
    return text;
  }

  public void setText( final String text ) {
    this.text = text;
  }

  public List<GroupEntity> getGroups( ) {
    return groups;
  }

  public void setGroups( final List<GroupEntity> groups ) {
    this.groups = groups;
  }

  public List<RoleEntity> getRoles( ) {
    return roles;
  }

  public void setRoles( final List<RoleEntity> roles ) {
    this.roles = roles;
  }

  public List<UserEntity> getUsers( ) {
    return users;
  }

  public void setUsers( final List<UserEntity> users ) {
    this.users = users;
  }

  @PrePersist
  @PreUpdate
  public void generateOnCommit() {
    if( this.policyId == null ) {
      this.policyId = Identifiers.generateIdentifier( "ANP" );
    }
    this.uniqueName = buildUniqueName( account.getAccountNumber( ), name );
  }

  private static String buildUniqueName( final String accountNumber, final String name ) {
    return accountNumber + ":" + name;
  }
}
