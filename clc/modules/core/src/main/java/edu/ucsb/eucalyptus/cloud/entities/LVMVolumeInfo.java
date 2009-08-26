/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
        return vbladePid;
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
