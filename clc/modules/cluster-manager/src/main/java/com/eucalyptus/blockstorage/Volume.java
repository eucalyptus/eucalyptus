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
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.blockstorage;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.util.StorageProperties;

import edu.ucsb.eucalyptus.cloud.state.AbstractIsomorph;
import edu.ucsb.eucalyptus.cloud.state.State;

@Entity
@PersistenceContext(name="eucalyptus_images")
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Volume extends AbstractIsomorph {

  @Column(name="size")
  private Integer size;
  @Column(name="cluster")
  private String cluster;
  @Column(name="parentsnapshot")
  private String parentSnapshot;
  @Column(name="remotedevice")
  @Lob  
  private String remoteDevice;
  @Column(name="localdevice")
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
      case ANNIHILATED: return "deleted";
      case FAIL: return "failed";
      case BUSY: return "in-use";
      default: return "unavailable";
    }
  }

  public void setMappedState( final String state ) {
    if( StorageProperties.Status.failed.toString().equals( state ) ) this.setState( State.FAIL );
    else if(StorageProperties.Status.creating.toString().equals( state ) ) this.setState( State.GENERATING );
    else if(StorageProperties.Status.available.toString().equals( state ) ) this.setState( State.EXTANT );
    else if("in-use".equals( state ) ) this.setState( State.BUSY );
    else this.setState( State.ANNIHILATED );
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
  
  public boolean isReady() {
	  return this.getState().equals(State.EXTANT);
  }
}
