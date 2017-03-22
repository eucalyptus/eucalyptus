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
 ************************************************************************/
package com.eucalyptus.auth.euare.persist.entities;

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
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

  @Lob
  @Type(type = "org.hibernate.type.StringClobType")
  @Column(name = "metadata_server_cert_body", nullable = true)
  private String certificateBody;

  @Lob
  @Type(type = "org.hibernate.type.StringClobType")
  @Column(name = "metadata_server_cert_chain", nullable = true)
  private String certificateChain;

  @Lob
  @Type(type = "org.hibernate.type.StringClobType")
  @Column(name = "metadata_server_cert_pk", nullable = true)
  private String privateKey;

  @Lob
  @Type(type = "org.hibernate.type.StringClobType")
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
