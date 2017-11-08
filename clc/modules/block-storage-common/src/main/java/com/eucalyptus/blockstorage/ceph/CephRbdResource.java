/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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

package com.eucalyptus.blockstorage.ceph;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.ceph.rbd.RbdImage;
import com.eucalyptus.blockstorage.StorageResource;
import com.eucalyptus.blockstorage.ceph.entities.CephRbdInfo;
import com.eucalyptus.blockstorage.ceph.exceptions.EucalyptusCephException;
import com.eucalyptus.blockstorage.exceptions.UnknownSizeException;

public class CephRbdResource extends StorageResource {

  private static final Logger LOG = Logger.getLogger(CephRbdResource.class);

  private String imageName = null;
  private String poolName = null;
  private CephRbdInfo info = null;

  public CephRbdResource(String id, String path) {
    super(id, path, StorageResource.Type.CEPH);
    info = CephRbdInfo.getStorageInfo();
    String[] poolImage = path.split(CephRbdInfo.POOL_IMAGE_DELIMITER);
    if (poolImage == null || poolImage.length != 2) {
      LOG.warn("Invalid format for path CephImageResource, expected pool/image but got " + path);
      throw new EucalyptusCephException("Invalid format for path CephImageResource, expected pool/image but got " + path);
    } else {
      poolName = poolImage[0];
      imageName = poolImage[1];
    }
  }

  @Override
  public Boolean isDownloadSynchronous() {
    return Boolean.FALSE;
  }

  @Override
  public Long getSize() throws UnknownSizeException {
    try (CephRbdConnectionManager conn = CephRbdConnectionManager.getConnection(info, poolName)) {
      RbdImage rbdImage = null;
      try {
        rbdImage = conn.getRbd().open(imageName);
        return rbdImage.stat().size;
      } finally {
        if (rbdImage != null) {
          conn.getRbd().close(rbdImage);
        }
      }
    } catch (Exception e) {
      throw new UnknownSizeException("Failed to determine size of ceph image " + imageName + " in pool " + poolName, e);
    }
  }

  @Override
  public InputStream getInputStream() throws Exception {
    return new CephRbdInputStream(imageName, poolName, info);
  }

  @Override
  public OutputStream getOutputStream() throws Exception {
    return new CephRbdOutputStream(imageName, poolName, info);
  }
}
