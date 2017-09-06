/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare.persist.entities;

import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.annotations.Type;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AuxiliaryDatabaseObject;
import com.eucalyptus.entities.AuxiliaryDatabaseObjects;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_managed_policy", indexes = {
    @Index( name = "auth_policy_owning_account_idx", columnList = "auth_policy_owning_account" ),
} )
@AuxiliaryDatabaseObjects({
    // Hibernate 4.3.11 does not support using the @ForeignKey annotation for delete cascade so
    // defined via an ADO to allow bulk deletion of user/group entities on account delete
    @AuxiliaryDatabaseObject(
        dialect = "org.hibernate.dialect.PostgreSQLDialect",
        create = "alter table ${schema}.auth_group_attached_policies drop constraint fk_i1wnqn77cvdspieke3j8qhy2g, " +
            "add constraint fk_i1wnqn77cvdspieke3j8qhy2g foreign key (auth_group_id) references " +
            "${schema}.auth_group(id) on delete cascade",
        drop = "alter table ${schema}.auth_group_attached_policies drop constraint fk_i1wnqn77cvdspieke3j8qhy2g, " +
            "add constraint fk_i1wnqn77cvdspieke3j8qhy2g foreign key (auth_group_id) references " +
            "${schema}.auth_group(id)"
    ),
    @AuxiliaryDatabaseObject(
        dialect = "org.hibernate.dialect.PostgreSQLDialect",
        create = "alter table ${schema}.auth_user_attached_policies drop constraint fk_fqkuqu15p45o6ff6i5x9y7qy, " +
            "add constraint fk_fqkuqu15p45o6ff6i5x9y7qy foreign key (auth_user_id) references " +
            "${schema}.auth_user(id) on delete cascade",
        drop = "alter table ${schema}.auth_user_attached_policies drop constraint fk_fqkuqu15p45o6ff6i5x9y7qy, " +
            "add constraint fk_fqkuqu15p45o6ff6i5x9y7qy foreign key (auth_user_id) references " +
            "${schema}.auth_user(id)"
    ),
    // no force delete for roles, so no cascade to role policy attachments
})
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

  // The original policy text in JSON
  @Column( name = "auth_policy_text", nullable = false )
  @Type(type="text")
  private String text;

  @Column( name = "auth_policy_attachment_count" )
  private Integer attachmentCount;

  @Temporal( TemporalType.TIMESTAMP)
  @Column(name = "auth_policy_update_timestamp")
  private Date policyUpdated;

  @Column( name = "auth_policy_version_counter" )
  private Integer policyVersionCounter;

  @Column( name = "auth_policy_default_version_number" )
  private Integer defaultPolicyVersionNumber;

  @OneToOne( fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true )
  @JoinColumn( name = "auth_policy_default_version" )
  private ManagedPolicyVersionEntity defaultPolicyVersion;

  @ManyToOne
  @JoinColumn( name = "auth_policy_owning_account", nullable = false )
  private AccountEntity account;

  @OneToMany( mappedBy = "policy" )
  private List<ManagedPolicyVersionEntity> versions;

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
    setAttachmentCount( 0 );
    setPolicyVersionCounter( 0 );
    setVersions( Lists.newArrayList( ) );
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

  public Integer nextPolicyVersion( ) {
    policyVersionCounter = policyVersionCounter + 1;
    setPolicyUpdated( new Date( ) );
    return policyVersionCounter;
  }

  public void applyDefaultPolicyVersion( @Nonnull final ManagedPolicyVersionEntity policyVersion ) {
    setDefaultPolicyVersion( policyVersion );
    setDefaultPolicyVersionNumber( policyVersion.getPolicyVersion( ) );
    setText( policyVersion.getText( ) );
    setPolicyUpdated( new Date( ) );
    getVersions( ).forEach( version -> version.setDefaultPolicy( false ) );
    policyVersion.setDefaultPolicy( true );
  }

  public void decrementAttachmentCount( ) {
    setAttachmentCount( Math.max( 0, MoreObjects.firstNonNull( getAttachmentCount( ), 0 ) - 1 ) );
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

  public String getText( ) {
    return text;
  }

  public void setText( final String text ) {
    this.text = text;
  }

  public Integer getAttachmentCount( ) {
    return attachmentCount;
  }

  public void setAttachmentCount( final Integer attachmentCount ) {
    this.attachmentCount = attachmentCount;
  }

  public Integer getPolicyVersionCounter() {
    return policyVersionCounter;
  }

  public void setPolicyVersionCounter( final Integer policyVersionCounter ) {
    this.policyVersionCounter = policyVersionCounter;
  }

  public Date getPolicyUpdated() {
    return policyUpdated;
  }

  public void setPolicyUpdated( final Date policyUpdated ) {
    this.policyUpdated = policyUpdated;
  }

  public Integer getDefaultPolicyVersionNumber( ) {
    return defaultPolicyVersionNumber;
  }

  public void setDefaultPolicyVersionNumber( final Integer defaultPolicyVersionNumber ) {
    this.defaultPolicyVersionNumber = defaultPolicyVersionNumber;
  }

  public ManagedPolicyVersionEntity getDefaultPolicyVersion( ) {
    return defaultPolicyVersion;
  }

  public void setDefaultPolicyVersion( final ManagedPolicyVersionEntity defaultPolicyVersion ) {
    this.defaultPolicyVersion = defaultPolicyVersion;
  }

  public List<ManagedPolicyVersionEntity> getVersions( ) {
    return versions;
  }

  public void setVersions( final List<ManagedPolicyVersionEntity> versions ) {
    this.versions = versions;
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

  public String accountNumber( ) {
    return uniqueName.substring( 0, uniqueName.lastIndexOf( ':' ) );
  }

  @PrePersist
  @PreUpdate
  public void generateOnCommit() {
    if( this.policyId == null ) {
      this.policyId = Identifiers.generateIdentifier( "ANP" );
    }
    if( this.policyUpdated == null ) {
      this.policyUpdated = getCreationTimestamp( );
    }
    this.uniqueName = buildUniqueName( account.getAccountNumber( ), name );
  }

  private static String buildUniqueName( final String accountNumber, final String name ) {
    return accountNumber + ":" + name;
  }
}
