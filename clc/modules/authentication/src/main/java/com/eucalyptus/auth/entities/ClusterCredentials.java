package com.eucalyptus.auth.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.entities.AbstractPersistent;

@Entity
@PersistenceContext(name="eucalyptus_auth")
@Table( name = "auth_clusters" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ClusterCredentials extends AbstractPersistent implements Serializable {

  @Transient
  private static final long serialVersionUID = 1L;

  @Column( name = "auth_cluster_name", unique=true )
  String clusterName;
  
  @Column(name="auth_cluster_x509_certificate")
  String clusterCertificate;
  
  @Column(name="auth_cluster_node_x509_certificate")
  String nodeCertificate;
  
  public ClusterCredentials( ) {
  }
  
  public ClusterCredentials( String clusterName ) {
    this.clusterName = clusterName;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( clusterName == null ) ? 0 : clusterName.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( !super.equals( obj ) ) return false;
    if ( !getClass( ).equals( obj.getClass( ) ) ) return false;
    ClusterCredentials other = ( ClusterCredentials ) obj;
    if ( clusterName == null ) {
      if ( other.clusterName != null ) return false;
    } else if ( !clusterName.equals( other.clusterName ) ) return false;
    return true;
  }
  
  public String getClusterCertificate( ) {
    return this.clusterCertificate;
  }
  
  public void setClusterCertificate( String cert ) {
    this.clusterCertificate = cert;
  }
  
  public String getNodeCertificate( ) {
    return this.nodeCertificate;
  }
  
  public void setNodeCertificate( String cert ) {
    this.nodeCertificate = cert;
  }
  
}
