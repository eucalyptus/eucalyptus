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

package com.eucalyptus.blockstorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.exceptions.UnknownSizeException;
import com.eucalyptus.blockstorage.util.StorageProperties;

import edu.ucsb.eucalyptus.util.SystemUtil;
import edu.ucsb.eucalyptus.util.SystemUtil.CommandOutput;

public class BlockDeviceResource extends StorageResource {

  private static final int ATTEMPTS = 10;
  private static Logger LOG = Logger.getLogger(BlockDeviceResource.class);

  public BlockDeviceResource(String id, String name) {
    super(id, name, StorageResource.Type.BLOCK);
  }

  @Override
  public Long getSize() throws UnknownSizeException {
    Long size = 0L;
    try {
      CommandOutput result =
          SystemUtil.runWithRawOutput(new String[] {StorageProperties.EUCA_ROOT_WRAPPER, "blockdev", "--getsize64", this.getPath()});
      size = Long.parseLong(StringUtils.trimToEmpty(result.output));
      return size;
    } catch (Exception e) {
      throw new UnknownSizeException("Failed to determine size for " + this.getId() + " mounted at " + this.getPath(), e);
    }
  }

  @Override
  public InputStream getInputStream() throws FileNotFoundException {
    return new FileInputStream(new File(this.getPath()));
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    FileOutputStream outStream = null;
    int failedAttempts = 0;
    do {
      try {
        outStream = new FileOutputStream(new File(this.getPath()));
        return outStream;
      } catch (FileNotFoundException e) { // Output stream to block devices may throw permission denied error, retry a few times
        if ((++failedAttempts) < ATTEMPTS) {
          LOG.debug("Failed to open FileOutputStream for " + this.getId() + " mounted at " + this.getPath() + ". Will retry");
        } else {
          LOG.warn("Failed to open FileOutputStream for " + this.getId() + " mounted at " + this.getPath() + " after " + failedAttempts + " attempts");
          throw e;
        }
      }
    } while (failedAttempts < ATTEMPTS);

    throw new IOException("Failed to open FileOutputStream for " + this.getId() + " mounted at " + this.getPath());
  }

  @Override
  public Boolean isDownloadSynchronous() {
    return Boolean.TRUE;
  }

}
