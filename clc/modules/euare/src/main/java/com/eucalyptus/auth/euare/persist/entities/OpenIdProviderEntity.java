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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.persist.entities;

import static org.hamcrest.CoreMatchers.*;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.eucalyptus.util.Parameters;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_openid_provider", indexes =
    @Index(
        name = "auth_openid_provider_account_url_idx",
        columnList = "auth_openid_provider_owning_account, auth_openid_provider_url",
        unique = true )
)
public class OpenIdProviderEntity extends AbstractPersistent implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column( name = "auth_openid_provider_url", nullable = false, updatable = false )
  private String url;

  @Column( name = "auth_openid_provider_host", nullable = false, updatable = false )
  private String host;

  @Column( name = "auth_openid_provider_port", nullable = false, updatable = false )
  private Integer port;

  @Column( name = "auth_openid_provider_path", nullable = false, updatable = false )
  private String path;

  @ElementCollection( fetch = FetchType.EAGER )
  @CollectionTable(
      name = "auth_openid_provider_client_ids",
      joinColumns = @JoinColumn( name = "openidproviderentity_id", referencedColumnName = "id" ) )
  @Column( name = "auth_openid_provider_client_id" )
  @OrderColumn( name = "auth_openid_provider_index")
  private List<String> clientIDs = Lists.newArrayList();

  @ElementCollection( fetch = FetchType.EAGER )
  @CollectionTable(
      name = "auth_openid_provider_thumbprints",
      joinColumns = @JoinColumn( name = "openidproviderentity_id", referencedColumnName = "id" ) )
  @Column( name = "auth_openid_provider_thumbprint" )
  @OrderColumn( name = "auth_openid_provider_index")
  private List<String> thumbprints = Lists.newArrayList();

  @ManyToOne
  @JoinColumn( name = "auth_openid_provider_owning_account", nullable = false, updatable = false )
  private AccountEntity account;

  @SuppressWarnings( "WeakerAccess" )
  protected OpenIdProviderEntity( ) {
  }

  @SuppressWarnings( "WeakerAccess" )
  protected OpenIdProviderEntity( final AccountEntity account, final String host, final Integer port, final String path ) {
    this( );
    setAccount( account );
    setUrl( host + path );
    setHost( host );
    setPort( port );
    setPath( path );
  }

  /**
   * Create a new provider in the given account with the specified url.
   *
   * The host/path is used to construct the url for the provider. The url is
   * the last component in the ARN for the provider.
   *
   * @param account The owning account
   * @param host The host for the provider
   * @param port The port for the provider
   * @param path The path for the provider
   * @return The new entity
   */
  public static OpenIdProviderEntity create(
      @Nonnull final AccountEntity account,
      @Nonnull final String host,
      @Nonnull final Integer port,
      @Nonnull final String path
  ) {
    Parameters.checkParam( "account", account, notNullValue( ) );
    Parameters.checkParam( "host", host, allOf( notNullValue(), not( containsString( ":" ) ) ) );
    Parameters.checkParam( "port", port, notNullValue( ) );
    Parameters.checkParam( "path", host, allOf( notNullValue(), not( containsString( ":" ) ) ) );
    return new OpenIdProviderEntity( account, host, port, path );
  }

  public static EntityRestriction<OpenIdProviderEntity> named(
      @Nonnull  final AccountEntity account,
      @Nullable final String url
  ){
    return Entities.restriction( OpenIdProviderEntity.class )
        .equal( OpenIdProviderEntity_.account, account )
        .equalIfNonNull( OpenIdProviderEntity_.url, url )
        .build( );
  }

  @SuppressWarnings( "RedundantIfStatement" )
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;

    OpenIdProviderEntity that = ( OpenIdProviderEntity ) o;
    if ( !url.equals( that.url ) ) return false;

    return true;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( OpenIdProviderEntity.class )
        .add( "url", getUrl( ) )
        .add( "port", getPort( ) )
        .toString( );
  }

  public String getEntityId( ) {
    return super.getId( );
  }

  public String getUrl( ) {
    return this.url;
  }

  public void setUrl( String url ) {
    this.url = url;
  }

  public String getHost() {
    return host;
  }

  public void setHost( final String host ) {
    this.host = host;
  }

  public Integer getPort( ) {
    return port;
  }

  public void setPort( final Integer port ) {
    this.port = port;
  }

  public String getPath( ) {
    return path;
  }

  public void setPath( final String path ) {
    this.path = path;
  }

  public List<String> getClientIDs( ) {
    return this.clientIDs;
  }

  public List<String> getThumbprints( ) {
    return this.thumbprints;
  }

  public AccountEntity getAccount() {
    return account;
  }

  public void setAccount( final AccountEntity account ) {
    this.account = account;
  }
}
