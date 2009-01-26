package edu.ucsb.eucalyptus.cloud.state;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;

@Entity
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Snapshot extends AbstractIsomorph {
  @Id
  @GeneratedValue
  private Long id = -1l;
  private String parentVolume;

  public Snapshot(  ) {
    super( null, null );
  }

  public Snapshot( final String userName, final String displayName ) {
    super( userName, displayName );
  }

  public Snapshot( final String userName, final String displayName, final String parentVolume ) {
    this( userName, displayName );
    this.parentVolume = parentVolume;
  }

  public String mapState( ) {
    switch(this.getState()) {
      case GENERATING: return "pending";
      case EXTANT: return "completed";
      default: return null;
    }
  }

  public Object morph( final Object o ) {
    return null;
  }

  public edu.ucsb.eucalyptus.msgs.Snapshot morph( final edu.ucsb.eucalyptus.msgs.Snapshot snap ) {
    snap.setProgress( "100%!1!11" );
    snap.setSnapshotId( this.getDisplayName() );
    snap.setStatus( this.mapState() );
    snap.setStartTime( this.getBirthday() );
    snap.setVolumeId( this.getParentVolume() );
    return snap;
  }

  public String getParentVolume() {
    return parentVolume;
  }

  public void setParentVolume( final String parentVolume ) {
    this.parentVolume = parentVolume;
  }
}
