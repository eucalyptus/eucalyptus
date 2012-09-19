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
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.type.StringClobType;

import com.eucalyptus.auth.principal.Authorization.EffectType;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database policy entity.
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_policy" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PolicyEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;
  
  // The policy name
  @Column( name = "auth_policy_name" )
  String name;
  
  @Column( name = "auth_policy_version" )
  String policyVersion;
  
  // The original policy text in JSON
  @Column( name = "auth_policy_text" )
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
  String text;
  
  // The set of statements of this policy
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "policy" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<StatementEntity> statements;
  
  // The owning group
  @ManyToOne
  @JoinColumn( name = "auth_policy_owning_group" )
  GroupEntity group;
  
  public PolicyEntity( ) {
  }
  
  public PolicyEntity( String name ) {
    this.name = name;
  }

  public PolicyEntity( String version, String text, List<StatementEntity> statements ) {
    this.policyVersion = version;
    this.text = text;
    this.statements = statements;
  }

  public static PolicyEntity newInstanceWithId( final String id ) {
    PolicyEntity p = new PolicyEntity( );
    p.setId( id );
    return p;
  }
  
  public String getText( ) {
    return this.text;
  }

  public void setText( String text ) {
    this.text = text;
  }
  
  public String getName( ) {
    return this.name;
  }
  
  public void setName( String name ) {
    this.name = name;
  }
  
  public List<StatementEntity> getStatements( ) {
    return this.statements;
  }
  
  public GroupEntity getGroup( ) {
    return this.group;
  }
  
  public void setGroup( GroupEntity group ) {
    this.group = group;
  }

  public String getPolicyVersion( ) {
    return this.policyVersion;
  }

  public void setPolicyVersion( String policyVersion ) {
    this.policyVersion = policyVersion;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) );
    return sb.toString( );
  }
  
  /**
   * NOTE:IMPORTANT: this method has default visibility (rather than public) only for the sake of
   * supporting currently hand-coded proxy classes. Don't share this value with the user.
   * 
   * TODO: remove this if possible.
   * 
   * @return
   * @see {@link AbstractPersistent#getId()}
   */
  public String getPolicyId( ) {
    return this.getId( );
  }

  /**
   * @param policy
   * @return true if the policy contains IAM permission statements, i.e. Effect is "Allow" or "Deny".
   */
  public boolean containsIamPermission( ) {
    for ( StatementEntity statement : this.getStatements( ) ) {
      for ( AuthorizationEntity authorization : statement.getAuthorizations( ) ) {
        if ( authorization.getEffect( ) != EffectType.Limit ) {
          return true;
        }
      }
    }
    return false;
  }
  
}
