/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package edu.ucsb.eucalyptus.cloud.state;

import com.eucalyptus.util.StorageProperties;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Snapshot extends AbstractIsomorph {
  @Id
  @GeneratedValue
  private Long id = -1l;

  private String parentVolume;
  private String cluster;

  public Snapshot() {
    super();
  }

  public Snapshot( final String userName, final String displayName ) {
    super( userName, displayName );
  }

  public Snapshot( final String userName, final String displayName, final String parentVolume ) {
    this( userName, displayName );
    this.parentVolume = parentVolume;
  }

  public static Snapshot named( String userName, String volumeId ) {
    Snapshot v = new Snapshot();
    v.setDisplayName( volumeId );
    v.setUserName( userName );
    return v;
  }

  public static Snapshot ownedBy( String userName ) {
    Snapshot v = new Snapshot();
    v.setUserName( userName );
    return v;
  }

  public String mapState() {
    switch ( this.getState() ) {
      case GENERATING:
        return "pending";
      case EXTANT:
        return "completed";
      default:
        return "failed";
    }
  }

  public void setMappedState( final String state ) {
    if ( StorageProperties.Status.creating.toString().equals( state ) ) this.setState( State.GENERATING );
    else if ( StorageProperties.Status.pending.toString().equals( state ) ) this.setState( State.GENERATING );
    else if ( StorageProperties.Status.completed.toString().equals( state ) ) this.setState( State.EXTANT );
    else if ( StorageProperties.Status.available.toString().equals( state ) ) this.setState( State.EXTANT );
    else if ( StorageProperties.Status.failed.toString().equals( state ) ) this.setState( State.FAIL );
  }

  public Object morph( final Object o ) {
    return null;
  }

  public edu.ucsb.eucalyptus.msgs.Snapshot morph( final edu.ucsb.eucalyptus.msgs.Snapshot snap ) {
    snap.setSnapshotId( this.getDisplayName() );
    snap.setStatus( this.mapState() );
    snap.setStartTime( this.getBirthday() );
    snap.setVolumeId( this.getParentVolume() );
    snap.setProgress( this.getState().equals( State.EXTANT ) ? "100%" : "" );
    return snap;
  }

  public String getParentVolume() {
    return parentVolume;
  }

  public void setParentVolume( final String parentVolume ) {
    this.parentVolume = parentVolume;
  }

  public String getCluster( ) {
    return cluster;
  }

  public void setCluster( String cluster ) {
    this.cluster = cluster;
  }
}
