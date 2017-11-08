/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare.persist.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database instance profile entity.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_instance_profile", indexes = {
    @Index( name = "auth_instance_profile_name_idx", columnList = "auth_instance_profile_name" ),
    @Index( name = "auth_instance_profile_role_idx", columnList = "auth_instance_profile_role" ),
    @Index( name = "auth_instance_profile_owning_account_idx", columnList = "auth_instance_profile_owning_account" )
} )
public class InstanceProfileEntity extends AbstractPersistent implements Serializable {

  private static final long serialVersionUID = 1L;

  // The Instance Profile ID the user facing id which conforms to length and character restrictions per spec.
  @Column( name = "auth_instance_profile_id_external", nullable = false, updatable = false )
  private String instanceProfileId;

    // Instance Profile name
  @Column( name = "auth_instance_profile_name", nullable = false)
  private String name;

  // Instance Profile path (prefix to organize profile name space)
  @Column( name = "auth_instance_profile_path", nullable = false )
  private String path;

  @ManyToOne
  @JoinColumn( name = "auth_instance_profile_role", nullable = true )
  private RoleEntity role;

  @ManyToOne
  @JoinColumn( name = "auth_instance_profile_owning_account", nullable = false )
  private AccountEntity account;

  @Column( name = "auth_instance_profile_unique_name", unique = true, nullable = false )
  private String uniqueName;

  public InstanceProfileEntity( ) {
  }

  public InstanceProfileEntity( final String name ) {
    this( );
    this.name = name;
  }

  public String getInstanceProfileId() {
    return instanceProfileId;
  }

  public void setInstanceProfileId( final String instanceProfileId ) {
    this.instanceProfileId = instanceProfileId;
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

  public RoleEntity getRole() {
    return role;
  }

  public void setRole( final RoleEntity role ) {
    this.role = role;
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
    if( this.instanceProfileId == null ) {
      this.instanceProfileId = Identifiers.generateIdentifier( "AIP" );
    }
    this.uniqueName = account.getAccountNumber() + ":" + name;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "InstanceProfile(" );
    sb.append( "id=" ).append( this.getId() ).append( ", " );
    sb.append( "instanceProfileId=" ).append( this.getInstanceProfileId() ).append( ", " );
    sb.append( "name=" ).append( this.getName() ).append( ", " );
    sb.append( "path=" ).append( this.getPath() ).append( ", " );
    sb.append( ")" );
    return sb.toString( );
  }
}
