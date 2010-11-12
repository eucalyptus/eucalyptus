package com.eucalyptus.auth.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

/**
 * Database X509 certificate entity.
 * 
 * @author wenye
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_auth" )
@Table( name = "auth_cert" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class CertificateEntity extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  @Transient
  private static Logger LOG = Logger.getLogger( CertificateEntity.class );
  
  // Flag for active or inactive
  @Column( name = "auth_certificate_active" )
  Boolean active;
  
  // Flag for revoked certificates
  @Column( name = "auth_certificate_revoked" )
  Boolean revoked;
  
  // The certificate
  @Lob
  @Column( name = "auth_certificate_pem" )
  String pem;
  
  public CertificateEntity( ) {
  }
  
  public CertificateEntity( String pem ) {
    this.pem = pem;
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
  }
  
}
