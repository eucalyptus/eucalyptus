/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package edu.ucsb.eucalyptus.cloud.entities;

import javax.persistence.Column;
import org.hibernate.annotations.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@PersistenceContext(name="eucalyptus_storage")
@Table( name = "AOEVolumeInfo" )
@Entity @javax.persistence.Entity
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class AOEVolumeInfo extends LVMVolumeInfo {
    @Column(name="vbladepid")
	private Integer vbladePid;
    @Column(name="majornumber")
    private Integer majorNumber;
    @Column(name="minornumber")
    private Integer minorNumber;

    public AOEVolumeInfo() {}

    public AOEVolumeInfo(String volumeId) {
        this.volumeId = volumeId;
    }

    public Integer getVbladePid() {
        return vbladePid;
    }

    public void setVbladePid(Integer vbladePid) {
        this.vbladePid = vbladePid;
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
