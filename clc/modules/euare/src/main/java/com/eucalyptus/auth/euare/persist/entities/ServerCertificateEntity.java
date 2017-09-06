/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare.persist.entities;

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Type;

import com.eucalyptus.auth.euare.ServerCertificates;
import com.eucalyptus.auth.euare.ServerCertificates.VerifiedCertInfo;
import com.eucalyptus.auth.policy.annotation.PolicyResourceType;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import com.google.common.base.Predicate;

/**
 * @author Sang-Min Park
 * 
 */
@Entity
@PersistenceContext(name = "eucalyptus_auth")
@Table(name = "auth_server_cert", indexes = {
    @Index( name = "auth_server_cert_account_id_idx", columnList = "metadata_account_id" ),
    @Index( name = "auth_server_cert_display_name_idx", columnList = "metadata_display_name" ),
})
@PolicyVendor( "iam" )
@PolicyResourceType( "server-certificate" )
public class ServerCertificateEntity extends AbstractOwnedPersistent implements RestrictedType {
  @Transient
  private static final long serialVersionUID = 1L;

  @Column(name = "metadata_server_cert_path", nullable = true)
  private String certificatePath;
  
  @Column(name = "metadata_server_cert_id", nullable=true)
  private String certificateId;

  @Type(type="text")
  @Column(name = "metadata_server_cert_body", nullable = true)
  private String certificateBody;

  @Type(type="text")
  @Column(name = "metadata_server_cert_chain", nullable = true)
  private String certificateChain;

  @Type(type="text")
  @Column(name = "metadata_server_cert_pk", nullable = true)
  private String privateKey;

  @Type(type="text")
  @Column(name = "metadata_session_key", nullable = true)
  private String sessionKey;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "metadata_server_cert_expiration")
  private Date expiration;

  @SuppressWarnings("unused")
  private ServerCertificateEntity() {
  }

  private ServerCertificateEntity(final OwnerFullName user){
    super(user, null);
  }
  public ServerCertificateEntity(final OwnerFullName user, final String certName) {
    super(user, certName);
  }
  
  public static EntityRestriction<ServerCertificateEntity> named(final OwnerFullName user){
    return Entities.restriction( ServerCertificateEntity.class )
        .equalIfNonNull( ServerCertificateEntity_.ownerAccountNumber, user == null ? null : user.getAccountNumber( ) )
        .equalIfNonNull( ServerCertificateEntity_.ownerUserId, user == null ? null : user.getUserId( ) )
        .equalIfNonNull( ServerCertificateEntity_.ownerUserName, user == null ? null : user.getUserName( ) )
        .build( );
  }

  public static EntityRestriction<ServerCertificateEntity> named(
      final OwnerFullName user,
      final String certName
  ) {
    final ServerCertificateEntity entity = new ServerCertificateEntity(user, certName );
    return Entities.restriction( ServerCertificateEntity.class )
        .equalIfNonNull( ServerCertificateEntity_.ownerAccountNumber, user == null ? null : user.getAccountNumber( ) )
        .equalIfNonNull( ServerCertificateEntity_.ownerUserId, user == null ? null : user.getUserId( ) )
        .equalIfNonNull( ServerCertificateEntity_.ownerUserName, user == null ? null : user.getUserName( ) )
        .equalIfNonNull( ServerCertificateEntity_.displayName, certName )
        .equalIfNonNull( ServerCertificateEntity_.uniqueName, entity.createUniqueName( ) )
        .build( );
  }

  public void setCertName(final String certName) {
    if(! isCertificateNameValid(certName))
      throw new IllegalArgumentException();
    
    this.setDisplayName(certName);
    this.setUniqueName(this.createUniqueName());
  }
  
  public String getCertName() {
    return this.displayName;
  }

  public String getCertPath() {
    return this.certificatePath;
  }

  public void setCertPath(final String path) throws IllegalArgumentException {
    if (path == null)
      this.certificatePath = "/";
    else
      this.certificatePath = path;
    if (!isCertificatePathValid(this.certificatePath))
      throw new IllegalArgumentException();
  }

  public void setCertBody(final String body) {
    this.certificateBody = body;
  }

  public String getCertBody() {
    return this.certificateBody;
  }

  public void setCertChain(final String chain) {
    this.certificateChain = chain;
  }

  public String getCertChain() {
    return this.certificateChain;
  }

  public void setPrivateKey(final String pk) {
    this.privateKey = pk;
  }

  public void setSessionKey(final String key) {
    this.sessionKey = key;
  }

  public String getSessionKey() {
    return this.sessionKey;
  }

  public String getPrivateKey() {
    return this.privateKey;
  }
  
  public void setCertId(final String id){
    this.certificateId = id;
  }
  
  public String getCertId(){
    return this.certificateId;
  }

  public Date getExpiration( ) {
    return expiration;
  }

  public void setExpiration( final Date expiration ) {
    this.expiration = expiration;
  }

  public static boolean isCertificatePathValid( final String path) {
    if (path == null)
      return false;
    if (!path.startsWith("/"))
      return false;
    if (path.length() < 1 || path.length() > 512)
      return false;

    return true;
  }

  public static boolean isCertificateNameValid(final String name) {
    if (name == null)
      return false;
    // plus (+), equal (=), comma (,), period (.), at (@), and dash (-).
    if (!name.matches("[A-Za-z0-9\\+=,\\.@\\-]+"))
      return false;
    if (name.length() < 1 || name.length() > 128)
      return false;

    return true;
  }

  @EntityUpgrade( entities = ServerCertificateEntity.class,  since = Upgrades.Version.v4_4_1, value = Euare.class )
  public enum ServerCertificateEntityUpgrade441 implements Predicate<Class> {
    INSTANCE;

    private static Logger logger = Logger.getLogger( ServerCertificateEntityUpgrade441.class );

    @SuppressWarnings( "unchecked" )
    @Override
    public boolean apply( final Class entityClass ) {
      try ( final TransactionResource tx = Entities.transactionFor( ServerCertificateEntity.class ) ) {
        final List<ServerCertificateEntity> entities = Entities.criteriaQuery( ServerCertificateEntity.class )
            .whereRestriction( r -> r.isNull( ServerCertificateEntity_.expiration ) ).list( );
        for ( final ServerCertificateEntity entity : entities ) {
          final String desc = entity.getCertId( ) + "/" + entity.getCertName( );
          logger.info( "Setting expiration for server certificate " + desc );
          try {
            final VerifiedCertInfo certInfo = ServerCertificates.verifyCertificate(
                entity.getCertBody( ),
                entity.getCertChain( ) );
            entity.setExpiration( certInfo.getExpiration( ) );
          } catch ( final Exception e ) {
            logger.error( "Error setting expiration for server certificate " + desc, e );
          }
        }
        tx.commit( );
      }
      return true;
    }
  }
}
