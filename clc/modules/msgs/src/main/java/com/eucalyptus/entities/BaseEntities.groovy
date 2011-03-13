package com.eucalyptus.entities;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.MappedSuperclass;
import javax.persistence.PersistenceContext;
import javax.persistence.Transient;
import javax.persistence.Table;
import javax.persistence.Id;
import org.hibernate.annotations.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.FetchType;
import javax.persistence.CascadeType;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.Version;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import org.hibernate.annotations.GenericGenerator;
import edu.ucsb.eucalyptus.cloud.Network;
import edu.ucsb.eucalyptus.msgs.PacketFilterRule;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasOwningAccount;
import com.eucalyptus.util.HasOwningUser;


@MappedSuperclass
public class AbstractPersistent implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "id" )
  String id;
  @Version
  @Column(name = "version")
  Integer version = 0;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update_timestamp")
  Date lastUpdate;
  
  public AbstractPersistent( ) {
    super( );
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( id == null ) ? 0 : id.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this.is( obj ) ) return true;
    if ( obj == null ) return false;
    if ( !getClass( ).is( obj.getClass( ) ) ) return false;
    AbstractPersistent other = ( AbstractPersistent ) obj;
    if ( id == null ) {
      if ( other.id != null ) return false;
    } else if ( !id.equals( other.id ) ) return false;
    return true;
  }
  
}

