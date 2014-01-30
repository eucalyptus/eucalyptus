/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.auth.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedType;

/**
 * @author Sang-Min Park
 * 
 */
@Entity
@PersistenceContext(name = "eucalyptus_auth")
@Table(name = "auth_server_cert")
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
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

  @SuppressWarnings("unused")
  private ServerCertificateEntity() {
  }

  private ServerCertificateEntity(final OwnerFullName user){
    super(user, null);
  }
  public ServerCertificateEntity(final OwnerFullName user, final String certName) {
    super(user, certName);
  }
  
  public static ServerCertificateEntity named(final OwnerFullName user){
    return new ServerCertificateEntity(user);
  }

  public static ServerCertificateEntity named(final OwnerFullName user,
      final String certName) {
    final ServerCertificateEntity entity = new ServerCertificateEntity(user,
        certName);
    entity.setUniqueName(entity.createUniqueName());
    return entity;
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

  public static boolean isCertificatePathValid(final String path) {
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
}
