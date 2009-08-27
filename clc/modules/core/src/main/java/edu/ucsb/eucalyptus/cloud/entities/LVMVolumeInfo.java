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
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity
@Table( name = "LVMVolumes" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class LVMVolumeInfo {
    @Id
    @GeneratedValue
    @Column(name = "lvm_volume_id")
    private Long id = -1l;
    @Column(name = "volume_name")
    private String volumeId;
    @Column(name = "lodev_name")
    private String loDevName;
    @Column(name = "lofile_name")
    private String loFileName;
    @Column(name = "pv_name")
    private String pvName;
    @Column(name = "vg_name")
    private String vgName;
    @Column(name = "lv_name")
    private String lvName;
    @Column(name = "size")
    private Integer size;
    @Column(name = "status")
    private String status;
    @Column(name = "vblade_pid")
    private Integer vbladePid;
    @Column(name = "snapshot_of")
    private String snapshotOf;
    @Column(name = "major_number")
    private Integer majorNumber;
    @Column(name = "minor_number")
    private Integer minorNumber;

    public LVMVolumeInfo() {}

    public LVMVolumeInfo(String volumeId) {
        this.volumeId = volumeId;
    }
    public String getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getLoDevName() {
        return loDevName;
    }

    public void setLoDevName(String loDevName) {
        this.loDevName = loDevName;
    }

    public String getLoFileName() {
        return loFileName;
    }

    public void setLoFileName(String loFileName) {
        this.loFileName = loFileName;
    }

    public String getPvName() {
        return pvName;
    }

    public void setPvName(String pvName) {
        this.pvName = pvName;
    }

    public String getVgName() {
        return vgName;
    }

    public void setVgName(String vgName) {
        this.vgName = vgName;
    }

    public String getLvName() {
        return lvName;
    }

    public void setLvName(String lvName) {
        this.lvName = lvName;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVbladePid() {
        return vbladePid != null? vbladePid : -1;
    }

    public void setVbladePid(Integer vbladePid) {
        this.vbladePid = vbladePid;
    }

    public String getSnapshotOf() {
        return snapshotOf;
    }

    public void setSnapshotOf(String snapshotOf) {
        this.snapshotOf = snapshotOf;
    }

    public Integer getMajorNumber() {
        return majorNumber;
    }

    public void setMajorNumber(Integer majorNumber) {
        this.majorNumber = majorNumber;
    }

    public Integer getMinorNumber() {
        return minorNumber;
    }

    public void setMinorNumber(Integer minorNumber) {
        this.minorNumber = minorNumber;
    }
}
