package com.eucalyptus.entities;

import java.io.Serializable
import java.util.Date
import javax.persistence.Column
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass
import javax.persistence.PrePersist
import javax.persistence.PreUpdate
import javax.persistence.Temporal
import javax.persistence.TemporalType
import javax.persistence.Version
import org.hibernate.annotations.GenericGenerator


@MappedSuperclass
public class AbstractPersistent implements Serializable {
  @Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name="system-uuid", strategy = "uuid")
  @Column( name = "id" )
  String id;
  @Version
  @Column(name = "version")
  Integer version;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "creation_timestamp")
  Date creationTimestamp;
  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "last_update_timestamp")
  Date lastUpdateTimestamp;
  
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
  
  @PreUpdate
  @PrePersist
  public void updateTimeStamps() {
    lastUpdateTimestamp = new Date();
    if ( creationTimestamp == null ) {
      this.creationTimestamp = new Date();
    }
  }
}

