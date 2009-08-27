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
package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.user.client.rpc.IsSerializable;

public class StorageInfoWeb implements IsSerializable {
	private String name;
	private String volumesPath;
	private Integer maxVolumeSizeInGB;
	private Integer totalVolumesSizeInGB;
	private String storageInterface;
	private Boolean zeroFillVolumes;
	private Boolean committed;

	public StorageInfoWeb() {}

	public StorageInfoWeb( final String name, 
			String volumesPath,
			Integer maxVolumeSizeInGB,
			Integer totalVolumesSizeInGB,
			String storageInterface,
			Boolean zeroFillVolumes) {
		this.name = name;
		this.volumesPath = volumesPath;
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
		this.totalVolumesSizeInGB = totalVolumesSizeInGB;
		this.storageInterface = storageInterface;
		this.zeroFillVolumes = zeroFillVolumes;
		this.committed = false;
	}


	public void setCommitted ()
	{
		this.committed = true;
	}

	public Boolean isCommitted ()
	{
		return this.committed;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVolumesPath() {
		return volumesPath;
	}

	public void setVolumesPath(String volumesPath) {
		this.volumesPath = volumesPath;
	}

	public Integer getMaxVolumeSizeInGB() {
		return maxVolumeSizeInGB;
	}

	public void setMaxVolumeSizeInGB(Integer maxVolumeSizeInGB) {
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
	}

	public Integer getTotalVolumesSizeInGB() {
		return totalVolumesSizeInGB;
	}

	public void setTotalVolumesSizeInGB(Integer totalVolumesSizeInGB) {
		this.totalVolumesSizeInGB = totalVolumesSizeInGB;
	}

	public String getStorageInterface() {
		return storageInterface;
	}

	public void setStorageInterface(String storageInterface) {
		this.storageInterface = storageInterface;
	}

	public Boolean getZeroFillVolumes() {
		return zeroFillVolumes;
	}

	public void setZeroFillVolumes(Boolean zeroFillVolumes) {
		this.zeroFillVolumes = zeroFillVolumes;
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		StorageInfoWeb that = ( StorageInfoWeb ) o;

		if ( !name.equals( that.name ) ) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}
}
