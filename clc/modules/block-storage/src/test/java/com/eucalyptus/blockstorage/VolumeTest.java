/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeResponseType;
import com.eucalyptus.blockstorage.msgs.CreateStorageVolumeType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesResponseType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageVolumesType;
import com.eucalyptus.blockstorage.msgs.StorageVolume;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.EucalyptusCloudException;

@Ignore("Manual development test")
public class VolumeTest {

  static BlockStorageController blockStorage;

  @Test
  public void testVolume() throws Exception {

    String userId = "admin";
    String volumeId = "vol-" + Crypto.getRandom( 10 );
    volumeId = volumeId.replaceAll("\\.", "x");

    CreateStorageVolumeType createVolumeRequest = new CreateStorageVolumeType();
    createVolumeRequest.setUserId(userId);
    createVolumeRequest.setVolumeId(volumeId);
    createVolumeRequest.setSize("1");
    CreateStorageVolumeResponseType createVolumeResponse = blockStorage.CreateStorageVolume(createVolumeRequest);
    System.out.println(createVolumeResponse);
    Thread.sleep(1000);
    DescribeStorageVolumesType describeVolumesRequest = new DescribeStorageVolumesType();

    describeVolumesRequest.setUserId(userId);
    ArrayList<String> volumeSet = new ArrayList<String>();
    volumeSet.add(volumeId);
    describeVolumesRequest.setVolumeSet(volumeSet);
    DescribeStorageVolumesResponseType describeVolumesResponse = blockStorage.DescribeStorageVolumes(describeVolumesRequest);
    StorageVolume vol = describeVolumesResponse.getVolumeSet().get(0);
    System.out.println(vol);
    while (true);
  }

  @BeforeClass
  public static void setUp() {
    blockStorage = new BlockStorageController();
    try {
      BlockStorageController.configure();
    } catch (EucalyptusCloudException e) {
      e.printStackTrace();
    }
  }
}
