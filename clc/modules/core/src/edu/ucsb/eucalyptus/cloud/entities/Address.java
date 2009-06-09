package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.constants.HasName;
import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseItemType;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Entity
@Table( name = "addresses" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Address implements HasName {

  private static Logger LOG = Logger.getLogger( Address.class );

  @Transient
  private final ReadWriteLock canHas = new ReentrantReadWriteLock( true );

  public static String UNALLOCATED_USERID = "nobody";
  public static String UNASSIGNED_INSTANCEID = "available";
  public static String UNASSIGNED_INSTANCEADDR = "0.0.0.0";
  @Id
  @GeneratedValue
  @Column( name = "address_id" )
  private Long id = -1l;
  @Column( name = "address_name" )
  private String name;
  @Column( name = "address_cluster" )
  private String cluster;
  @Column( name = "address_owner_id" )
  private String userId;
  @Column( name = "address_is_assigned" )
  private Boolean assigned;
  @Column( name = "address_instance_id" )
  private String instanceId;
  @Column( name = "address_instance_addr" )
  private String instanceAddress;

  public Address() {}

  public Address( final String name ) {
    this.name = name;
  }

  public Address( final String name, final String cluster, final String userId, final String instanceId, final String instanceAddress ) {
    this.name = name;
    this.cluster = cluster;
    this.userId = userId;
    this.instanceId = instanceId;
    this.instanceAddress = instanceAddress;
  }

  public Address( String address, String cluster ) {
    this.name = address;
    this.cluster = cluster;
    this.userId = UNALLOCATED_USERID;
    this.instanceId = UNASSIGNED_INSTANCEID;
    this.instanceAddress = UNASSIGNED_INSTANCEADDR;
  }

  public Boolean getAssigned() {
    return assigned;
  }

  public void setAssigned( final Boolean assigned ) {
    this.assigned = assigned;
  }

  public String getCluster() {
      return cluster;
  }

  public void release() {
    this.canHas.writeLock().lock();
    try {
      this.userId = UNALLOCATED_USERID;
      this.unassign();
      return;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public void unassign() {
    this.canHas.writeLock().lock();
    try {
      this.instanceId = UNASSIGNED_INSTANCEID;
      this.instanceAddress = UNASSIGNED_INSTANCEADDR;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public boolean isAssigned() {
    this.canHas.writeLock().lock();
    try {
      if ( UNASSIGNED_INSTANCEID.equals( this.instanceId ) || UNASSIGNED_INSTANCEADDR.equals( this.instanceAddress ) ) {
        this.instanceId = UNASSIGNED_INSTANCEID;
        this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        return false;
      }
      return true;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public String getUserId() {
      return userId;
  }

  public void setUserId( final String userId ) {
    this.userId = userId;
  }

  public boolean allocate( String userId ) {
    this.canHas.writeLock().lock();
    try {
      this.unassign();
      if( this.userId != null && !this.userId.equals( UNALLOCATED_USERID ) ) return false;
      this.setUserId( userId );
      return true;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public boolean assign( String instanceId, String instanceAddr ) {
    this.canHas.writeLock().lock();
    try {
      if ( UNASSIGNED_INSTANCEADDR.equals( instanceAddr ) ) return false;
      this.instanceId = instanceId;
      this.instanceAddress = instanceAddr;
      return true;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public String getInstanceId() {
    return this.instanceId;
  }

  public String getName() {
    return this.name;
  }

  public DescribeAddressesResponseItemType getDescription( boolean isAdmin ) {
    return new DescribeAddressesResponseItemType( this.getName(), isAdmin ? String.format( "%s (%s)", this.getInstanceId(),this.getUserId() ) : this.getInstanceId() );
  }

  public int compareTo( final Object o ) {
    Address that = ( Address ) o;
    return this.getName().compareTo( that.getName() );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof Address ) ) return false;

    Address address = ( Address ) o;

    if ( !name.equals( address.name ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public String getInstanceAddress() {
    return instanceAddress;
  }

  public void setInstanceAddress( final String instanceAddress ) {
    this.instanceAddress = instanceAddress;
  }

  public Long getId() {
    return id;
  }

  public void setId( final Long id ) {
    this.id = id;
  }

  public void setCluster( final String cluster ) {
    this.cluster = cluster;
  }

  @Override
  public String toString() {
    return "Address{" +
           "name='" + name + '\'' +
           ", cluster='" + cluster + '\'' +
           ", sourceUserId='" + userId + '\'' +
           ", assigned=" + assigned +
           ", instanceId='" + instanceId + '\'' +
           ", instanceAddress='" + instanceAddress + '\'' +
           '}';
  }
}
