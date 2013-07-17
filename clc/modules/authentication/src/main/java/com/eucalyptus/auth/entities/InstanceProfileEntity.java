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
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database instance profile entity.
 */
@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_instance_profile" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
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
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private RoleEntity role;

  @ManyToOne
  @JoinColumn( name = "auth_instance_profile_owning_account", nullable = false )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
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
      this.instanceProfileId = Crypto.generateAlphanumericId( 21, "AIP" );
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
