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

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.CascadeType;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table( name = "WalrusSnapshotSet" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class WalrusSnapshotSet {
    @Id
    @GeneratedValue
    @Column(name = "walrus_snapshot_set_id")
    private Long id = -1l;
   //same as the bucket name
    @Column(name = "snapshotset_id")
    private String snapshotSetId;

    @OneToMany( cascade = CascadeType.ALL )
    @JoinTable(
            name = "snapshotset_has_snapshots",
            joinColumns = { @JoinColumn( name = "walrus_snapshot_set_id" ) },
            inverseJoinColumns = @JoinColumn( name = "walrus_snapshot_id" )
    )
    @Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
    private List<WalrusSnapshotInfo> snapshotSet = new ArrayList<WalrusSnapshotInfo>();

    public WalrusSnapshotSet() {}

    public WalrusSnapshotSet(String snapshotSetId) {
        this.snapshotSetId = snapshotSetId;
    }

    public String getSnapshotSetId() {
        return snapshotSetId;
    }

    public void setSnapshotSetId(String snapshotSetId) {
        this.snapshotSetId = snapshotSetId;
    }

    public List<WalrusSnapshotInfo> getSnapshotSet() {
        return snapshotSet;
    }

    public void setSnapshotSet(List<WalrusSnapshotInfo> snapshotSet) {
        this.snapshotSet = snapshotSet;
    }
}