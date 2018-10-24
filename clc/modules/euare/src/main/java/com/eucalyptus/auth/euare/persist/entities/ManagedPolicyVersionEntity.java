/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AuxiliaryDatabaseObject;
import com.eucalyptus.entities.AuxiliaryDatabaseObjects;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_managed_policy_version", indexes = {
    @Index( name = "auth_policy_owning_account_idx", columnList = "auth_policy_owning_account" ),
    @Index( name = "auth_policy_owning_policy_idx", columnList = "auth_policy_owning_policy" ),
} )
@AuxiliaryDatabaseObjects({
    // Hibernate 4.3.11 does not support using the @ForeignKey annotation for delete cascade so
    // defined via an ADO to allow bulk deletion of managed policy entities on account delete
    @AuxiliaryDatabaseObject(
        dialect = "org.hibernate.dialect.PostgreSQLDialect",
        create = "alter table ${schema}.auth_managed_policy_version drop constraint fk_h4h22u1gkx2ycpls77kbr6ln8, " +
            "add constraint fk_h4h22u1gkx2ycpls77kbr6ln8 foreign key (auth_policy_owning_policy) references " +
            "${schema}.auth_managed_policy(id) on delete cascade",
        drop = "alter table ${schema}.auth_managed_policy_version drop constraint fk_h4h22u1gkx2ycpls77kbr6ln8, " +
            "add constraint fk_h4h22u1gkx2ycpls77kbr6ln8 foreign key (auth_policy_owning_policy) references " +
            "${schema}.auth_managed_policy(id)"
    ),
})
public class ManagedPolicyVersionEntity extends AbstractPersistent {

  // The owning policy name
  @Column( name = "auth_policy_name", nullable = false)
  private String policyName;

  @Column( name = "auth_policy_version", nullable = false )
  private Integer policyVersion;

  @Column( name = "auth_policy_default", nullable = false )
  private Boolean defaultPolicy;

  // The original policy text in JSON
  @Column( name = "auth_policy_text", nullable = false )
  @Type(type="text")
  private String text;

  @ManyToOne
  @JoinColumn( name = "auth_policy_owning_account", nullable = false )
  private AccountEntity account;

  @ManyToOne
  @JoinColumn( name = "auth_policy_owning_policy", nullable = false )
  private ManagedPolicyEntity policy;

  protected ManagedPolicyVersionEntity( ) {
  }

  public ManagedPolicyVersionEntity( final ManagedPolicyEntity managedPolicyEntity ) {
    setPolicy( managedPolicyEntity );
    setPolicyName( managedPolicyEntity.getName( ) );
    setPolicyVersion( managedPolicyEntity.nextPolicyVersion( ) );
    setDefaultPolicy( false );
    setText( managedPolicyEntity.getText( ) );
    setAccount( managedPolicyEntity.getAccount( ) );
  }

  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName( final String policyName ) {
    this.policyName = policyName;
  }

  public Integer getPolicyVersion() {
    return policyVersion;
  }

  public void setPolicyVersion( final Integer policyVersion ) {
    this.policyVersion = policyVersion;
  }

  public Boolean getDefaultPolicy() {
    return defaultPolicy;
  }

  public void setDefaultPolicy( final Boolean defaultPolicy ) {
    this.defaultPolicy = defaultPolicy;
  }

  public String getText() {
    return text;
  }

  public void setText( final String text ) {
    this.text = text;
  }

  public AccountEntity getAccount() {
    return account;
  }

  public void setAccount( final AccountEntity account ) {
    this.account = account;
  }

  public ManagedPolicyEntity getPolicy() {
    return policy;
  }

  public void setPolicy( final ManagedPolicyEntity policy ) {
    this.policy = policy;
  }
}
