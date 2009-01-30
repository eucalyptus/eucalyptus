package edu.ucsb.eucalyptus.cloud.state;

import org.hibernate.annotations.*;

import javax.persistence.Entity;
import javax.persistence.*;

@Entity
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Volume extends AbstractIsomorph {

  @Id
  @GeneratedValue
  private Long id = -1l;
  private Integer size;
  private String cluster;
  private String parentSnapshot;
  private String remoteDevice;
  private String localDevice;

  public Volume() {
    super( );
  }

  public Volume( final String userName, final String displayName, final Integer size, final String cluster, final String parentSnapshot ) {
    super( userName, displayName );
    this.size = size;
    this.cluster = cluster;
    this.parentSnapshot = parentSnapshot;
  }

  public Volume( String userName, String displayName ) {
    super( userName, displayName );
  }

  public static Volume named( String userName, String volumeId ) {
    Volume v = new Volume(  );
    v.setDisplayName( volumeId );
    v.setUserName( userName );
    return v;
  }


  public static Volume ownedBy( String userName ) {
    Volume v = new Volume(  );
    v.setUserName( userName );
    return v;
  }

  public String mapState( ) {
    switch(this.getState()) {
      case GENERATING: return "creating";
      case EXTANT: return "available";
      case ANNIHILATING: return "deleting";
      case ANNILATED: return "deleted";
      case FAIL: return "failed";
      default: return "unavailable";
    }
  }

  public void setMappedState( final String state ) {
    if("failed".equals( state ) ) this.setState( State.FAIL );
    else if("creating".equals( state ) ) this.setState( State.GENERATING );
    else if("available".equals( state ) ) this.setState( State.EXTANT );
    else this.setState( State.ANNILATED );
  }

  public Object morph( final Object o ) {
    return null;
  }

  public edu.ucsb.eucalyptus.msgs.Volume morph( final edu.ucsb.eucalyptus.msgs.Volume vol ) {
    vol.setAvailabilityZone( this.getCluster() );
    vol.setCreateTime( this.getBirthday() );
    vol.setVolumeId( this.getDisplayName() );
    vol.setSnapshotId( this.getParentSnapshot() );
    vol.setStatus( this.mapState() );
    vol.setSize( this.getSize().toString() );
    return vol;
  }

  public Integer getSize() {
    return size;
  }

  public String getCluster() {
    return cluster;
  }

  public void setSize( final Integer size ) {
    this.size = size;
  }

  public void setCluster( final String cluster ) {
    this.cluster = cluster;
  }

  public String getParentSnapshot() {
    return parentSnapshot;
  }

  public void setParentSnapshot( final String parentSnapshot ) {
    this.parentSnapshot = parentSnapshot;
  }

  public String getRemoteDevice() {
    return remoteDevice;
  }

  public void setRemoteDevice( final String remoteDevice ) {
    this.remoteDevice = remoteDevice;
  }

  public String getLocalDevice() {
    return localDevice;
  }

  public void setLocalDevice( final String localDevice ) {
    this.localDevice = localDevice;
  }
}
