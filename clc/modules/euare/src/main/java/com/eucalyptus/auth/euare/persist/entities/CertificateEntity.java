/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.euare.common.identity.Certificate;
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
  @Lob
  @Type(type="org.hibernate.type.StringClobType")
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
        final List<CertificateEntity> entities = (List<CertificateEntity>)
            Entities.createCriteria( CertificateEntity.class ).add( Restrictions.or(
                Restrictions.isNull( "certificateHashId" ),
                Restrictions.eq( "revoked", true )
            ) ).list( );
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
