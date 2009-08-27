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
    if( StorageProperties.Status.failed.toString().equals( state ) ) this.setState( State.FAIL );
    else if(StorageProperties.Status.creating.toString().equals( state ) ) this.setState( State.GENERATING );
    else if(StorageProperties.Status.available.toString().equals( state ) ) this.setState( State.EXTANT );
    else if("in-use".equals( state ) ) this.setState( State.BUSY );
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
    vol.setSize( (this.getSize() == -1) || (this.getSize() == null)? null : this.getSize().toString() );
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
