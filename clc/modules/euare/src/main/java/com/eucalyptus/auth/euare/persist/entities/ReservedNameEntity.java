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
