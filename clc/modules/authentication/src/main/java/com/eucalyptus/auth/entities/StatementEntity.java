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
import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database statement entity.
 * 
 * Each policy statement is decomposed into a list of authorizations and a list of
 * conditions. Each authorization contains one action and one resource pattern.
 * Each condition contains one condition type and one key. The purpose of this
 * decomposition is to index the statements easily, to make lookup faster and to
 * match the staged processing of requests.
 * 
 * For example, the following statement
 * {
 *   "Effect":"Allow",
 *   "Action":["RunInstance", "DescribeImages"],
 *   "Resource":["emi-12345678", "emi-ABCDEFGH"],
 *   "Condition":{
 *     "IpAddress":{
 *       "aws:SourceIp":"10.0.0.0/24",
 *     }
 *     "DateLessThanEquals":[
 *       {
 *         "aws:CurrentTime":"2010-11-01T12:00:00Z",
 *       },
 *       {
 *         "aws:EpochTime":"1284562853",
 *       },
 *     ]
 *   }
 * }
 * is decomposed into a list of authorizations:
 * 
 * Effect  Action         ResourceType   ResourcePattern
 * -----------------------------------------------------
 * Allow   RunInstance    Image          emi-12345678
 * Allow   RunInstance    Image          emi-ABCDEFGH
 * Allow   DescribeImages Image          emi-12345678
 * Allow   DescribeImages Image          emi-ABCDEFGH
 * 
 * and a list of conditions:
 * 
 * Type                 Key              Value
 * -----------------------------------------------------
 * IpAddress            aws:SourceIp     10.0.0.0/24
 * DateLessThanEquals   aws:CurrentTime  2010-11-01T12:00:00Z
 * DateLessThanEquals   aws:EpochTime    1284562853
 * 
 * When each authorization is evaluated, the corresponding list of conditions
 * are also evaluated.
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_statement" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class StatementEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;
  
  // Statement ID
  @Column( name = "auth_statement_sid" )
  String sid;
  
  // List of decomposed authorizations
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "statement" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<AuthorizationEntity> authorizations;
  
  // List of decomposed conditions
  @OneToMany( cascade = { CascadeType.ALL }, mappedBy = "statement" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<ConditionEntity> conditions;
  
  // The owning policy
  @ManyToOne
  @JoinColumn( name = "auth_statement_owning_policy" )
  PolicyEntity policy;
  
  public StatementEntity( ) {
  }
  
  public StatementEntity( String sid ) {
    this.sid = sid;
  }

  public StatementEntity( String sid,
                          @Nonnull List<AuthorizationEntity> authorizations,
                          @Nonnull List<ConditionEntity> conditions ) {
    this.sid = sid;
    this.authorizations = authorizations;
    this.conditions = conditions;
    for ( AuthorizationEntity auth : authorizations ) {
      auth.setStatement( this );
    }
    for ( ConditionEntity cond : conditions ) {
      cond.setStatement( this );
    }
  }

  public String getSid( ) {
    return this.sid;
  }
  
  public void setSid( String sid ) {
    this.sid = sid;
  }
  
  public List<AuthorizationEntity> getAuthorizations( ) {
    return this.authorizations;
  }
  
  public void setAuthorizations( List<AuthorizationEntity> authorizations ) {
    this.authorizations = authorizations;
  }
  
  public List<ConditionEntity> getConditions( ) {
    return this.conditions;
  }
  
  public void setConditions( List<ConditionEntity> conditions ) {
    this.conditions = conditions;
  }
  
  public PolicyEntity getPolicy( ) {
    return this.policy;
  }
  
  public void setPolicy( PolicyEntity policy ) {
    this.policy = policy;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "sid=" ).append( this.getSid( ) );
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
  public String getStatementId( ) {
    return this.getId( );
  }

}
