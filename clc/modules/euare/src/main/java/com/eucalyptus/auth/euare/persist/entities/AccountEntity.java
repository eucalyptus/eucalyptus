/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.persist.entities;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database account entity.
 */

@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_account" )
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
  Set<GroupEntity> groups;

  public AccountEntity( ) {
  }
  
  public AccountEntity( String name ) {
    this( );
    this.name = name;
  }

  @PrePersist
  public void generateOnCommit() {
    this.accountNumber = Identifiers.generateAccountNumber( );
    if ( this.name == null ) {
      this.name = this.accountNumber;
    }
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
                    com.eucalyptus.auth.euare.Accounts.lookupAccountByCanonicalId( buf.toString() );
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

}
