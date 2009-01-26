package edu.ucsb.eucalyptus.cloud.state;

import edu.ucsb.eucalyptus.cloud.cluster.VmInstance;
import edu.ucsb.eucalyptus.msgs.*;
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
  @Transient
  private WeakInteraction attachInfo;

  public Volume() {
    super( null, null );
  }

  public Volume( final String userName, final String displayName, final Integer size, final String cluster, final String parentSnapshot ) {
    super( userName, displayName );
    this.size = size;
    this.cluster = cluster;
    this.parentSnapshot = parentSnapshot;
  }

  public Volume( String userName, String displayName ) {
    super( userName, displayName );
    this.attachInfo = new WeakInteraction<VmInstance,InstanceAttachment>();
  }

  public String mapState( ) {
    switch(this.getState()) {
      case GENERATING: return "creating";
      case EXTANT: return "available";
      case ANNIHILATING: return "deleting";
      case ANNILATED: return "deleted";
      default: return null;
    }
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
    vol.setSize( this.getState().toString() );
    return vol;
  }

  class InstanceAttachment {
    private String blockDevice;

  }

  public Integer getSize() {
    return size;
  }

  public String getCluster() {
    return cluster;
  }

  public WeakInteraction getAttachInfo() {
    return attachInfo;
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
}
