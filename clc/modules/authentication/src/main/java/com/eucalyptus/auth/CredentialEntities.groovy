package com.eucalyptus.auth

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.annotations.GenericGenerator
import javax.persistence.Entity
import javax.persistence.Table
import javax.persistence.Id
import javax.persistence.GeneratedValue
import javax.persistence.Column
import javax.persistence.Lob
import javax.persistence.OneToMany
import javax.persistence.FetchType
import javax.persistence.CascadeType
import org.hibernate.sql.Alias
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
}

@Entity
@Table( name = "auth_users" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class User implements Serializable {
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
  @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
  List<X509Cert> certificates = []
  public User(){}
  public User( String userName ){
    this.userName = userName
  }
  
}
