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

package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Sets;

/**
 * Database authorization entity. A single row of authorization table represents a decomposed
 * unit of the  policy statement. Each authorization contains only one action and one resource
 * pattern. And conditions are not included in the authorization record.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_auth" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AuthorizationEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // The effect of the authorization
  @Enumerated( EnumType.STRING )
  @Column( name = "auth_auth_effect" )
  EffectType effect;

  // The type of resource this authorization applies to, used to restrict search.
  @Column( name = "auth_auth_type" )
  String type;
  
  // If action list is negated, i.e. NotAction
  @Column( name = "auth_auth_not_action" )
  Boolean notAction;
  
  @ElementCollection
  @CollectionTable( name = "auth_auth_action_list" )
  @Column( name = "auth_auth_actions" )
  Set<String> actions;
  
  // If resource list is negated, i.e. NotResource
  @Column( name = "auth_auth_not_resource" )
  Boolean notResource;
  
  @ElementCollection
  @CollectionTable( name = "auth_auth_resource_list" )
  @Column( name = "auth_auth_resources" )
  Set<String> resources;

  // The owning statement
  @ManyToOne
  @JoinColumn( name = "auth_auth_owning_statement" )
  StatementEntity statement;
  
  public AuthorizationEntity( ) {
  }

  public AuthorizationEntity( EffectType effect, String type, Set<String> actions, Boolean notAction, Set<String> resources, Boolean notResource ) {
    this.effect = effect;
    this.type = type;
    this.notAction = notAction;
    this.actions = actions;
    this.notResource = notResource;
    this.resources = resources;
  }

  public AuthorizationEntity( EffectType effect, Set<String> actions, Boolean notAction ) {
    this.effect = effect;
    this.type = null;
    this.notAction = notAction;
    this.actions = actions;
    this.notResource = false;
    this.resources = Sets.newHashSet();
  }

  public AuthorizationEntity( String type ) {
    this.type = type;
  }

  public static AuthorizationEntity newInstanceWithId( final String id ) {
    AuthorizationEntity a = new AuthorizationEntity( );
    a.setId( id );
    return a;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Authorization(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "effect=" ).append( this.effect ).append( ", " );
    sb.append( "type=" ).append( this.type ).append( ", " );
    sb.append( "notAction=" ).append( this.isNotAction( ) ).append( ", " );
    sb.append( "notResource=" ).append( this.isNotResource( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public EffectType getEffect( ) {
    return this.effect;
  }

  public void setEffect( EffectType effect ) {
    this.effect = effect;
  }
  
  public StatementEntity getStatement( ) {
    return this.statement;
  }
  
  public void setStatement( StatementEntity statement ) {
    this.statement = statement;
  }

  public Boolean isNotAction( ) {
    return this.notAction;
  }

  public void setNotAction( Boolean notAction ) {
    this.notAction = notAction;
  }
  
  public Boolean isNotResource( ) {
    return this.notResource;
  }
  
  public void setNotResource( Boolean notResource ) {
    this.notResource = notResource;
  }

  public String getType( ) {
    return this.type;
  }

  public void setType( String type ) {
    this.type = type;
  }
  
  public Set<String> getActions( ) {
    return this.actions;
  }

  public Set<String> getResources( ) {
    return this.resources;
  }

  /**
   * NOTE:IMPORTANT: this method has public visibility (rather than public) only for the sake of
   * supporting currently hand-coded proxy classes. Don't share this value with the user.
   * 
   * TODO: remove this if possible.
   * 
   * @return
   * @see {@link AbstractPersistent#getId()}
   */
  public String getAuthorizationId( ) {
    return this.getId( );
  }
}
