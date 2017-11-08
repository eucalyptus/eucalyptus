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

package com.eucalyptus.auth.euare.persist;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity;
import com.eucalyptus.auth.euare.persist.entities.CertificateEntity_;
import com.eucalyptus.auth.euare.principal.EuareCertificate;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.auth.util.X509CertHelper;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseCertificateProxy implements EuareCertificate {

  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger( DatabaseCertificateProxy.class );
  
  private CertificateEntity delegate;
  
  public DatabaseCertificateProxy( CertificateEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      DatabaseAuthUtils.invokeUnique( CertificateEntity.class, CertificateEntity_.certificateId, this.delegate.getCertificateId( ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString( );
  }
  
  @Override
  public String getCertificateId( ) {
    return this.delegate.getCertificateId( );
  }

  @Override
  public Boolean isActive( ) {
    return this.delegate.isActive( );
  }
  
  @Override
  public void setActive( final Boolean active ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( CertificateEntity.class, CertificateEntity_.certificateId, this.delegate.getCertificateId( ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) {
          t.setActive( active );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setActive for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public Date getCreateDate( ) {
    return this.delegate.getCreateDate( );
  }
  
  public void setCreateDate( final Date createDate ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( CertificateEntity.class, CertificateEntity_.certificateId, this.delegate.getCertificateId( ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) {
          t.setCreateDate( createDate );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setCreateDate for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public UserPrincipal getPrincipal( ) throws AuthException {
    final List<UserPrincipal> results = Lists.newArrayList( );
    try {
      DatabaseAuthUtils.invokeUnique( CertificateEntity.class, CertificateEntity_.certificateId, this.delegate.getCertificateId( ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) {
          try {
            results.add( com.eucalyptus.auth.euare.Accounts.userAsPrincipal( new DatabaseUserProxy( t.getUser() ) ) );
          } catch ( AuthException e ) {
            throw Exceptions.toUndeclared( e );
          }
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getUser for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }

  @Override
  public X509Certificate getX509Certificate( ) {
    return X509CertHelper.toCertificate( this.delegate.getPem( ) );
  }

  public void setX509Certificate( final X509Certificate x509 ) throws AuthException {
    try {
      DatabaseAuthUtils.invokeUnique( CertificateEntity.class, CertificateEntity_.certificateId, this.delegate.getCertificateId( ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) {
          t.setPem( X509CertHelper.fromCertificate( x509 ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setX509Certificate for " + this.delegate );
      throw new AuthException( e );
    }
  }

  @Override
  public String getPem( ) {
    return this.delegate.getPem( );
  }

  
}
