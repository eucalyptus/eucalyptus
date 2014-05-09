/*************************************************************************
 * Copyright 2013-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.vm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parent;

@Embeddable
public class VmCreateImageSnapshot {
	@Parent
	VmInstance vmInstance = null;


	@Column( name = "metadata_vm_createimage_snapshot_device_name" )
	private String     deviceName;
	@Column( name = "metadata_vm_createimage_snapshot_id" )
	private String       snapshotId;
	@Column( name = "metadata_vm_createimage_snapshot_is_root_device")
	private Boolean isRootDevice;
	@Column( name = "metadata_vm_createimage_snapshot_delete_on_terminate")
	private Boolean deleteOnTerminate;
	@Column( name = "metadata_vm_createimage_snapshot_start_time" )
	private Date       startTime;

	private VmCreateImageSnapshot(){ }
	public VmCreateImageSnapshot(final String deviceName, final String snapshotId, final Boolean isRootDevice, final Boolean deleteOnTerminate){
		this.deviceName = deviceName;
		this.snapshotId = snapshotId;
		this.isRootDevice = isRootDevice;
		this.deleteOnTerminate = deleteOnTerminate;
		startTime = new Date(System.currentTimeMillis());
	}

	private VmInstance getVmInstance( ) {
		return this.vmInstance;
	}

	private void setVmInstance( final VmInstance vmInstance ) {
		this.vmInstance = vmInstance;
	}

	public String getDeviceName(){
		return this.deviceName;
	}

	public String getSnapshotId(){
		return this.snapshotId;
	}

	public Date getStartTime(){
		return this.startTime;
	}
	
	public Boolean isRootDevice(){
		return this.isRootDevice;
	}
	public Boolean getDeleteOnTerminate(){
		return this.deleteOnTerminate;
	}

	@Override
	public boolean equals(Object obj){
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass( ) != obj.getClass( ) ) {
			return false;
		}
		VmCreateImageSnapshot other = ( VmCreateImageSnapshot ) obj;
		if ( this.snapshotId == null ) {
			if ( other.snapshotId != null ) {
				return false;
			}
		} else if ( !this.snapshotId.equals( other.snapshotId ) ) {
			return false;
		}
		return true;
	}


	@Override
	public int hashCode( ) {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( this.snapshotId == null )
				? 0
						: this.snapshotId.hashCode( ) );
		return result;
	}

}
