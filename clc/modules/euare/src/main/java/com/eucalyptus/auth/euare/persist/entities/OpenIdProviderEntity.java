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

import static com.eucalyptus.upgrade.Upgrades.Version.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.AuxiliaryDatabaseObject;
import com.eucalyptus.entities.AuxiliaryDatabaseObjects;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.collect.Lists;
import groovy.sql.Sql;

/**
 * Database entity for a user.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_openid_provider", indexes = {
    @Index( name = "auth_openid_provider_url_idx", columnList = "auth_openid_provider_url" ),
    @Index( name = "auth_openid_provider_owning_account_idx", columnList = "auth_openid_provider_owning_account" )
} )
public class OpenIdProviderEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // User name
  @Column( name = "auth_openid_provider_url" )
  String url;

  // List of client ids
  @ElementCollection
  @CollectionTable( name = "auth_openid_provider_client_ids" )
  @Column( name = "auth_openid_provider_client_id" )
  @JoinColumn( name = "auth_openid_provider_url" )
  @OrderColumn( name = "auth_openid_provider_index")
  private List<String> clientIDs = Lists.newArrayList();

  // List of thumbprints
  @ElementCollection
  @CollectionTable( name = "auth_openid_provider_thumbprints" )
  @Column( name = "auth_openid_provider_thumbprint" )
  @JoinColumn( name = "auth_openid_provider_url" )
  @OrderColumn( name = "auth_openid_provider_index")
  private List<String> thumbprints = Lists.newArrayList();

  @ManyToOne
  @JoinColumn( name = "auth_openid_provider_owning_account", nullable = false )
  private AccountEntity account;

  public OpenIdProviderEntity( ) {
    this.clientIDs = new ArrayList( );
    this.thumbprints = new ArrayList( );
  }

  public OpenIdProviderEntity( final String url ) {
    this( );
    this.url = url;
  }

  public static OpenIdProviderEntity newInstanceWithUrl( final String url ) {
    OpenIdProviderEntity u = new OpenIdProviderEntity( );
    u.url = url;
    return u;
  }

  public static EntityRestriction<OpenIdProviderEntity> named(final String url){
    return Entities.restriction( OpenIdProviderEntity.class )
        .equalIfNonNull( OpenIdProviderEntity_.url, url )
        .build( );
  }

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
    StringBuilder sb = new StringBuilder( );
    sb.append( "OpenIdProvider(" );
    sb.append( "Url=" ).append( this.getUrl( ) ).append( ", " );
    sb.append( ")" );
    return sb.toString( );
  }

  public String getUrl( ) {
    return this.url;
  }

  public void setUrl( String url ) {
    this.url = url;
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
