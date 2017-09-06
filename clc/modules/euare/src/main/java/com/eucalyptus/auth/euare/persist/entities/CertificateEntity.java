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
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import com.eucalyptus.auth.util.Identifiers;
import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.upgrade.Upgrades;
import com.google.common.base.Predicate;

/**
 * Database X509 certificate entity.
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_cert", indexes = {
    @Index( name = "auth_certificate_hash_id_idx", columnList = "auth_certificate_hash_id" ),
    @Index( name = "auth_certificate_owning_user_idx", columnList = "auth_certificate_owning_user" )
} )
public class CertificateEntity extends AbstractPersistent implements Serializable {

  private static final long serialVersionUID = 1L;

  // Flag for active or inactive
  @Column( name = "auth_certificate_active" )
  private Boolean active;
  
  // Flag for revoked certificates (not used from 4.2)
  @Column( name = "auth_certificate_revoked" )
  private Boolean revoked;
  
  // The certificate identifier, random for certificate generated pre 4.2
  @Column( name = "auth_certificate_id" )
  private String certificateId;

  // The certificate identifier derived from the certificate content.
  @Column( name = "auth_certificate_hash_id" )
  private String certificateHashId;

  // The certificate
  @Type(type="text")
  @Column( name = "auth_certificate_pem" )
  private String pem;
  
  // The create date
  @Column( name = "auth_certificate_create_date" )
  private Date createDate;
  
  // The owning user
  @ManyToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "auth_certificate_owning_user" )
  private UserEntity user;
  
  public CertificateEntity( ) {
  }
  
  public CertificateEntity( final String certificateId, final X509Certificate cert ) throws CertificateEncodingException {
    this.certificateId = certificateId;
    this.certificateHashId = certificateId;
    this.pem = X509CertHelper.fromCertificate( cert );
  }
  
  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    
    CertificateEntity that = ( CertificateEntity ) o;    
    if ( !this.getPem( ).equals( that.getPem( ) ) ) return false;
    
    return true;
  }
  
  @Override
  public String toString( ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( "Cert(" );
    sb.append( "ID=" ).append( this.getId( ) ).append( ", " );
    sb.append( "active=" ).append( this.isActive( ) ).append( ", " );
    sb.append( "pem=" ).append( this.getPem( ) );
    sb.append( ")" );
    return sb.toString( );
  }
  
  public String getPem( ) {
    return this.pem;
  }
  
  public void setPem( String pem ) {
    this.pem = pem;
  }
  
  public Boolean isActive( ) {
    return this.active;
  }
  
  public void setActive( Boolean active ) {
    this.active = active;
  }
  
  public Date getCreateDate( ) {
    return this.createDate;
  }
  
  public void setCreateDate( Date createDate ) {
    this.createDate = createDate;
  }
  
  public UserEntity getUser( ) {
    return this.user;
  }
  
  public void setUser( UserEntity user ) {
    this.user = user;
  }

  public String getCertificateId( ) {
    return this.certificateId;
  }

  @Upgrades.EntityUpgrade(entities = CertificateEntity.class,  since = Upgrades.Version.v4_2_0, value = Euare.class)
  public enum CertificateEntityUpgrade420 implements Predicate<Class> {
    INSTANCE;
    private static Logger logger = Logger.getLogger( CertificateEntityUpgrade420.class );
    @SuppressWarnings( "unchecked" )
    @Override
    public boolean apply( Class arg0 ) {
      try ( final TransactionResource tx = Entities.transactionFor( CertificateEntity.class ) ) {
        final List<CertificateEntity> entities = Entities.criteriaQuery( CertificateEntity.class ).where(
            Entities.restriction( CertificateEntity.class ).any(
                Entities.restriction( CertificateEntity.class ).isTrue( CertificateEntity_.revoked ).build( ),
                Entities.restriction( CertificateEntity.class ).isNull( CertificateEntity_.certificateHashId ).build( )
            )
        ).list( );
        for ( final CertificateEntity entity : entities ) {
          if ( entity.revoked ) {
            logger.info( "Deleting revoked certificate: " + entity.getCertificateId( ) );
            Entities.delete( entity );
          } else {
            try {
              entity.certificateHashId = Identifiers.generateCertificateIdentifier( X509CertHelper.toCertificate( entity.getPem( ) ) );
            } catch ( Exception e ) {
              logger.error( "Error generating fingerprint identifier for certificate", e );
            }
          }
        }
        tx.commit( );
      }
      return true;
    }
  }
}
