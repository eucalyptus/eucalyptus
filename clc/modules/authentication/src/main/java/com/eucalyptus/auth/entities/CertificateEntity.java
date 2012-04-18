package com.eucalyptus.auth.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database X509 certificate entity.
 * 
 * @author wenye
 *
 */
@Entity @javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_cert" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class CertificateEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  // Flag for active or inactive
  @Column( name = "auth_certificate_active" )
  Boolean active;
  
  // Flag for revoked certificates
  @Column( name = "auth_certificate_revoked" )
  Boolean revoked;
  
  // The certificate
  @Column( name = "auth_certificate_id" )
  String certificateId;

  // The certificate
  @Lob
  @Column( name = "auth_certificate_pem" )
  String pem;
  
  // The create date
  @Column( name = "auth_certificate_create_date" )
  Date createDate;
  
  // The owning user
  @ManyToOne
  @JoinColumn( name = "auth_certificate_owning_user" )
  UserEntity user;
  
  public CertificateEntity( ) {
  }
  
  public CertificateEntity( String pem ) {
    this.pem = pem;
  }
  
  public static CertificateEntity newInstanceWithId( final String id ) {
    CertificateEntity c = new CertificateEntity( );
    c.certificateId = id;
    return c;
  }
  
  @PrePersist
  public void generateOnCommit() {
    if( this.certificateId == null ) {
      this.certificateId = Crypto.generateQueryId();
    }
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
    sb.append( "revoked=" ).append( this.isRevoked( ) ).append( ", " );
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
  
  public Boolean isRevoked( ) {
    return this.revoked;
  }
  
  public void setRevoked( Boolean revoked ) {
    this.revoked = revoked;
    if ( this.revoked ) {
      this.active = false;
    }
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
  
}
