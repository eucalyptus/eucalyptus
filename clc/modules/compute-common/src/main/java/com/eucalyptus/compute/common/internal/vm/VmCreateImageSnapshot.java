/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.compute.common.internal.vm;

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
