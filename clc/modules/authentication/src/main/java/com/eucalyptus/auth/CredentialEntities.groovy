package com.eucalyptus.auth

import java.security.cert.X509Certificate;

import org.bouncycastle.util.encoders.UrlBase64;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.GenericGenerator
import javax.persistence.Table
import javax.persistence.GeneratedValue
import javax.persistence.Column
import javax.persistence.Lob
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.FetchType
import javax.persistence.CascadeType
import javax.persistence.JoinTable
import javax.persistence.JoinColumn
import org.hibernate.sql.Alias

@Entity
@Table( name = "auth_users" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class User implements Serializable {//TODO: srsly should be named UserCredentials to avoid heaps of later confusion
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "auth_user_id" )
  String id
  @Column( name = "auth_user_name", unique=true )
  String userName
  @Column( name = "auth_user_query_id" )
  String queryId
  @Column( name = "auth_user_secretkey" )
  String secretKey
  @Column( name = "auth_user_is_admin" )
  Boolean isAdministrator
  @OneToMany( cascade=[CascadeType.ALL], fetch=FetchType.EAGER )
  @JoinTable(name = "auth_user_has_x509", joinColumns = [ @JoinColumn( name = "auth_user_id" ) ],inverseJoinColumns = [ @JoinColumn( name = "auth_x509_id" ) ])
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  List<X509Cert> certificates = []
  public User(){}
  public User( String userName ){
    this.userName = userName
  }
  
}

@Entity
@Table(name="auth_x509")
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class X509Cert implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "auth_x509_id" )
  String id
  @Column( name = "auth_x509_alias", unique=true )
  String alias
  @Lob
  @Column( name = "auth_x509_pem_certificate" )
  String pemCertificate
  public X509Cert(){}
  public X509Cert( String alias ) {
    this.alias = alias
  }
  public static X509Cert fromCertificate(String alias, X509Certificate x509) {
    X509Cert x = new X509Cert( );
    x.setAlias(alias);
    x.setPemCertificate( new String( UrlBase64.encode( Hashes.getPemBytes( x509 ) ) ) );
    return x;
  }  
}
@Entity
@Table( name = "auth_clusters" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class ClusterCredentials implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "auth_cluster_id" )
  String id
  @Column( name = "auth_cluster_name", unique=true )
  String clusterName
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name="auth_cluster_x509_certificate")
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  X509Cert clusterCertificate
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name="auth_cluster_node_x509_certificate")
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  X509Cert nodeCertificate  
  public ClusterCredentials( ) {}
  public ClusterCredentials( String clusterName ) {
    this.clusterName = clusterName;
  }
}
