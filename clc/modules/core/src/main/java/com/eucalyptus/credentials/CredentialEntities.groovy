package com.eucalyptus.credentials
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import javax.persistence.*;

@Entity
@Table(name="x509_certificates")
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
class X509Certificate {
  @Id @GeneratedValue(strategy = GenerationType.AUTO)
  public Long id
  public String alias
  public String certificate
}
