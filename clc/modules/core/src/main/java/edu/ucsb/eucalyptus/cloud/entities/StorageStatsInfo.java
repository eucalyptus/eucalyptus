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
package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "storage_stats_info" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class StorageStatsInfo {
	@Id
	@GeneratedValue
	@Column( name = "storage_stats_info_id" )
	private Long id = -1l;
	@Column(name = "storage_name")
	private String name;
	@Column( name = "number_volumes" )
	private Integer numberOfVolumes;
	@Column( name = "total_space_used" )
	private Long totalSpaceUsed;

	public StorageStatsInfo() {}
	
	public StorageStatsInfo(final String name) {
		this.name = name;
	}
	
	public StorageStatsInfo(final String name, 
			Integer numberOfVolumes,
			Long totalSpaceUsed) {
		this.name = name;
		this.numberOfVolumes = numberOfVolumes;
		this.totalSpaceUsed = totalSpaceUsed;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getNumberOfVolumes() {
		return numberOfVolumes;
	}

	public void setNumberOfVolumes(Integer numberOfVolumes) {
		this.numberOfVolumes = numberOfVolumes;
	}

	public Long getTotalSpaceUsed() {
		return totalSpaceUsed;
	}

	public void setTotalSpaceUsed(Long totalSpaceUsed) {
		this.totalSpaceUsed = totalSpaceUsed;
	}
}
