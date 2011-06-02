package com.eucalyptus.auth;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.CertificateEntity;
import com.eucalyptus.auth.principal.Certificate;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.X509CertHelper;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Transactions;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabaseCertificateProxy implements Certificate {

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
      Transactions.one( CertificateEntity.newInstanceWithId( this.delegate.getCertificateId() ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) throws Throwable {
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
      Transactions.one( CertificateEntity.newInstanceWithId( this.delegate.getCertificateId() ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) throws Throwable {
          t.setActive( active );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setActive for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public Boolean isRevoked( ) {
    return this.delegate.isRevoked( );
  }
  
  @Override
  public void setRevoked( final Boolean revoked ) throws AuthException {
    try {
      Transactions.one( CertificateEntity.newInstanceWithId( this.delegate.getCertificateId() ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) throws Throwable {
          t.setRevoked( revoked );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setRevoked for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public Date getCreateDate( ) {
    return this.delegate.getCreateDate( );
  }
  
  @Override
  public void setCreateDate( final Date createDate ) throws AuthException {
    try {
      Transactions.one( CertificateEntity.newInstanceWithId( this.delegate.getCertificateId() ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) throws Throwable {
          t.setCreateDate( createDate );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setCreateDate for " + this.delegate );
      throw new AuthException( e );
    }
  }
  
  @Override
  public User getUser( ) throws AuthException {
    final List<User> results = Lists.newArrayList( );
    try {
      Transactions.one( CertificateEntity.newInstanceWithId( this.delegate.getCertificateId() ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) throws Throwable {
          results.add( new DatabaseUserProxy( t.getUser( ) ) );
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

  @Override
  public void setX509Certificate( final X509Certificate x509 ) throws AuthException {
    try {
      Transactions.one( CertificateEntity.newInstanceWithId( this.delegate.getCertificateId() ), new Tx<CertificateEntity>( ) {
        public void fire( CertificateEntity t ) throws Throwable {
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
