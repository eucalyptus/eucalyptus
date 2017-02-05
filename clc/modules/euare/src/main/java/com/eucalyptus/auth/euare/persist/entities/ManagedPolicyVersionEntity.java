/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.auth.euare.persist.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
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
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
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
