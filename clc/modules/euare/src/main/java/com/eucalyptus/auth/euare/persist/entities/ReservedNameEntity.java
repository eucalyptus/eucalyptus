/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.eucalyptus.util.Parameters;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_reserved_name" )
public class ReservedNameEntity extends AbstractPersistent implements Serializable {
  private static final long serialVersionUID = 1L;

  @Column( name = "auth_namespaced_name", unique = true, updatable = false )
  private String namespacedName;

  @Column( name = "auth_namespace", updatable = false )
  private String namespace;

  @Column( name = "auth_name", updatable = false )
  private String name;

  @Column( name = "auth_client_token", unique = true, updatable = false )
  private String clientToken;

  @Temporal( TemporalType.TIMESTAMP)
  @Column( name = "auth_expires", updatable = false )
  private Date expiry;

  public ReservedNameEntity( ) {
  }

  public static ReservedNameEntity create( @Nonnull final String namespace,
                                           @Nonnull final String name,
                                                    final int duration,
                                                    final String clientToken ) {
    Parameters.checkParam( "namespace", namespace, not( isEmptyOrNullString( ) ) );
    Parameters.checkParam( "name", name, not( isEmptyOrNullString( ) ) );
    final ReservedNameEntity reservedNameEntity = new ReservedNameEntity( );
    reservedNameEntity.setNamespacedName( namespace + ":" + name );
    reservedNameEntity.setNamespace( namespace );
    reservedNameEntity.setName( name );
    reservedNameEntity.setClientToken( clientToken );
    reservedNameEntity.setExpiry( new Date( System.currentTimeMillis( ) + TimeUnit.SECONDS.toMillis( duration ) ) );
    return reservedNameEntity;
  }

  public static EntityRestriction<ReservedNameEntity> exampleWithToken( final String clientToken ) {
    return Entities.restriction( ReservedNameEntity.class )
        .equal( ReservedNameEntity_.clientToken, clientToken )
        .build( );
  }

  public Date getExpiry( ) {
    return expiry;
  }

  public void setExpiry( final Date expiry ) {
    this.expiry = expiry;
  }

  public String getNamespacedName( ) {
    return namespacedName;
  }

  public void setNamespacedName( final String namespacedName ) {
    this.namespacedName = namespacedName;
  }

  public String getNamespace( ) {
    return namespace;
  }

  public void setNamespace( final String namespace ) {
    this.namespace = namespace;
  }

  public String getName( ) {
    return name;
  }

  public void setName( final String name ) {
    this.name = name;
  }

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( final String clientToken ) {
    this.clientToken = clientToken;
  }
}
