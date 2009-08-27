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
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.util.BindingUtil;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "storage_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class
StorageInfo {

	@Id
	@GeneratedValue
	@Column( name = "storage_id" )
	private Long id = -1l;
	@Column( name = "storage_name" )
	private String name;
	@Column( name = "system_storage_volume_size_gb" )
	private Integer maxTotalVolumeSizeInGb;
	@Column( name = "storage_interface" )
	private String storageInterface;
	@Column( name = "system_storage_max_volume_size_gb")
	private Integer maxVolumeSizeInGB;
	@Column( name = "system_storage_volumes_dir" )
	private String volumesDir;
    @Column(name = "zero_fill_volumes")
    private Boolean zeroFillVolumes;
    
	public StorageInfo(){}

	public StorageInfo( final String name )
	{
		this.name = name;
	}

	public StorageInfo(final String name, 
			final Integer maxTotalVolumeSizeInGb,
			final String storageInterface, 
			final Integer maxVolumeSizeInGB,
			final String volumesDir,
			final Boolean zeroFillVolumes) {
		this.name = name;
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
		this.storageInterface = storageInterface;
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
		this.volumesDir = volumesDir;
		this.zeroFillVolumes = zeroFillVolumes;
	}

	public Long getId()
	{
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getMaxTotalVolumeSizeInGb() {
		return maxTotalVolumeSizeInGb;
	}

	public void setMaxTotalVolumeSizeInGb(Integer maxTotalVolumeSizeInGb) {
		this.maxTotalVolumeSizeInGb = maxTotalVolumeSizeInGb;
	}

	public String getStorageInterface() {
		return storageInterface;
	}

	public void setStorageInterface(String storageInterface) {
		this.storageInterface = storageInterface;
	}

	public Integer getMaxVolumeSizeInGB() {
		return maxVolumeSizeInGB;
	}

	public void setMaxVolumeSizeInGB(Integer maxVolumeSizeInGB) {
		this.maxVolumeSizeInGB = maxVolumeSizeInGB;
	}

	public String getVolumesDir() {
		return volumesDir;
	}

	public void setVolumesDir(String volumesDir) {
		this.volumesDir = volumesDir;
	}
	
	public Boolean getZeroFillVolumes() {
		return zeroFillVolumes;
	}

	public void setZeroFillVolumes(Boolean zeroFillVolumes) {
		this.zeroFillVolumes = zeroFillVolumes;
	}

	@Override
	public boolean equals( Object o )
	{
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		StorageInfo that = ( StorageInfo ) o;

		if ( !name.equals( that.name ) ) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public String toString()
	{
		return this.name;
	}

	public static StorageInfo byName( String name )
	{
		return new StorageInfo(name);
	}

}
