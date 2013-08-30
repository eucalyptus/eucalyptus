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
