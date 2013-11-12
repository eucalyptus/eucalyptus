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
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.eucalyptus.util.Exceptions;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;
import com.google.common.base.Predicate;

/**
 * Database account entity.
 */

@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_account" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AccountEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // Account name, it is unique.
  @Column( name = "auth_account_name", unique = true )
  String name;

  @Column( name = "auth_account_number", unique = true )
  String accountNumber;

  @Column( name = "auth_account_canonical_id", length = 64, unique = true )
  String canonicalId;

  // Groups for this account
  @OneToMany( fetch = FetchType.LAZY, mappedBy = "account" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  List<GroupEntity> groups;

  public AccountEntity( ) {
  }
  
  public AccountEntity( String name ) {
    this( );
    this.name = name;
  }

  @PrePersist
  public void generateOnCommit() {
    this.accountNumber = String.format( "%012d", ( long ) ( Math.pow( 10, 12 ) * Math.random( ) ) );
    populateCanonicalId();
  }

    public void populateCanonicalId() {
        if (this.canonicalId == null || "".equals(this.canonicalId)) {
            this.canonicalId = genCanonicalId();
        }
    }

    private static String genCanonicalId( ) {
        StringBuilder buf = new StringBuilder();
        boolean notFinished = true;
        while (notFinished) {
            int rand = ((int) (Math.pow(10, 4) * Math.random()) ) % 100;
            buf.append(Integer.toHexString(rand));
            int len = buf.length();
            if (len < 64) {
                notFinished = true;
            }
            else if (len == 64) {
                notFinished = false;
            }
            else {
                buf.delete(64, len + 1); // end is exclusive, but if start == end, nothing is done
                notFinished = false;
            }
            if (! notFinished) {
                try {
                    Accounts.lookupAccountByCanonicalId(buf.toString());
                    // canonical id is a dupe
                    buf = new StringBuilder();
                    notFinished = true;
                }
                catch (AuthException aex) {
                    // canonical id is not in use
                }
            }
        }

        return buf.toString();
    }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    AccountEntity that = ( AccountEntity ) o;    
    if ( !name.equals( that.name ) ) return false;
    
    return true;
  }

  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Account(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "name=" ).append( this.getName( ) ).append( ", " );
    sb.append( "canonical ID=").append( this.getCanonicalId());
    sb.append( ")" );
    return sb.toString( );
  }
  
  public String getName( ) {
    return this.name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getAccountNumber( ) {
    return this.accountNumber;
  }

  public void setAccountNumber( String accountNumber ) {
    this.accountNumber = accountNumber;
  }

  public String getCanonicalId() {
    return canonicalId;
  }

  public void setCanonicalId(String canonicalId) {
    this.canonicalId = canonicalId;
  }

  public static AccountEntity newInstanceWithAccountNumber( String accountNumber ) {
    AccountEntity a = new AccountEntity( );
    a.setAccountNumber( accountNumber );
    return a;
  }

    @EntityUpgrade( entities = { AccountEntity.class }, since = Upgrades.Version.v3_4_0, value = Euare.class)
    public enum AccountEntityUpgrade implements Predicate<Class> {
        INSTANCE;
        private static Logger LOG = Logger.getLogger(AccountEntity.AccountEntityUpgrade.class);

        @Override
        public boolean apply(@Nullable Class aClass) {
            EntityTransaction tran = Entities.get(AccountEntity.class);
            try {
                List<AccountEntity> accounts = Entities.query(new AccountEntity());
                if (accounts != null && accounts.size() > 0) {
                    for (AccountEntity account : accounts) {
                        if (account.getCanonicalId() == null || account.getCanonicalId().equals("")) {
                            account.setCanonicalId( genCanonicalId( ) );
                            LOG.debug("putting canonical id " + account.getCanonicalId() +
                                    " on account " + account.getAccountNumber());
                        }
                    }
                }
                tran.commit();
            }
            catch (Exception ex) {
                tran.rollback();
                LOG.error("caught exception during upgrade, while attempting to generate and assign canonical ids");
                Exceptions.toUndeclared(ex);
            }
            return true;
        }
    }

}
