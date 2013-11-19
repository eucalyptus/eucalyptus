/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.collect.Lists;

/**
 * Database group entity.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_group" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class GroupEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // The Group ID: the user facing group id which conforms to length and character restrictions per spec.
  @Column( name = "auth_group_id_external" )
  String groupId;

  // Group name, not unique since different accounts can have the same group name
  @Column( name = "auth_group_name" )
  String name;
  
  // Group path (prefix to organize group name space, see AWS spec)
  @Column( name = "auth_group_path" )
  String path;
  
  // Indicates if this group is a special user group
  @Column( name = "auth_group_user_group" )
  Boolean userGroup;
  
  // Users in the group
  @ManyToMany( fetch = FetchType.LAZY )
  @JoinTable( name = "auth_group_has_users", joinColumns = { @JoinColumn( name = "auth_group_id" ) }, inverseJoinColumns = @JoinColumn( name = "auth_user_id" ) )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<UserEntity> users;

  // Policies for the group
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "group" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<PolicyEntity> policies;
  
  // The owning account
  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "auth_group_owning_account" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  AccountEntity account;
  
  public GroupEntity( ) {
    this.users = Lists.newArrayList( );
    this.policies = Lists.newArrayList( );
  }
  
  public GroupEntity( String name ) {
    this( );
    this.name = name;
  }
  
  public GroupEntity( Boolean userGroup ) {
    this( );
    this.userGroup = userGroup;
  }

  public static GroupEntity newInstanceWithGroupId( final String id ) {
    GroupEntity g = new GroupEntity( );
    g.groupId = id;
    return g;
  }

  @PrePersist
  public void generateOnCommit() {
    if( this.groupId == null ) {
      this.groupId = Crypto.generateAlphanumericId( 21, "AGP" );
    }
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    GroupEntity that = ( GroupEntity ) o;    
    if ( !name.equals( that.name ) ) return false;
    
    return true;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Group(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) ).append( ", " );
    sb.append( "path=" ).append( this.getPath( ) ).append( ", " );
    sb.append( "userGroup=" ).append( this.isUserGroup( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public String getName( ) {
    return this.name;
  }

  public void setName( String name ) {
    this.name = name;
  }
  
  public String getPath( ) {
    return this.path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public AccountEntity getAccount( ) {
    return this.account;
  }
  
  public void setAccount( AccountEntity account ) {
    this.account = account;
  }
  
  public Boolean isUserGroup( ) {
    return this.userGroup;
  }
  
  public void setUserGroup( Boolean userGroup ) {
    this.userGroup = userGroup;
  }
  
  public List<PolicyEntity> getPolicies( ) {
    return this.policies;
  }
  
  public List<UserEntity> getUsers( ) {
    return this.users;
  }

  public String getGroupId( ) {
    return this.groupId;
  }
  
}
