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

package com.eucalyptus.walrus;

import java.io.IOException;
import java.util.List;

import com.eucalyptus.storage.common.fs.FileIO;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.entities.PartInfo;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.WalrusDataGetResponseType;

public interface StorageManager {

  public void checkPreconditions() throws EucalyptusCloudException;

  public boolean bucketExists(String bucket);

  public boolean objectExists(String bucket, String object);

  public void createBucket(String bucket) throws IOException;

  public long getSize(String bucket, String object);

  public void deleteBucket(String bucket) throws IOException;

  public void createObject(String bucket, String object) throws IOException;

  public void putObject(String bucket, String object, byte[] base64Data, boolean append) throws IOException;

  public FileIO prepareForRead(String bucket, String object) throws Exception;

  public FileIO prepareForWrite(String bucket, String object) throws Exception;

  public int readObject(String bucket, String object, byte[] bytes, long offset) throws IOException;

  public int readObject(String objectPath, byte[] bytes, long offset) throws IOException;

  public void deleteObject(String bucket, String object) throws IOException;

  public void deleteAbsoluteObject(String object) throws IOException;

  public void copyObject(String sourceBucket, String sourceObject, String destinationBucket, String destinationObject) throws IOException;

  public void copyMultipartObject(List<PartInfo> parts, String destinationBucket, String destinationObject) throws Exception;

  public void renameObject(String bucket, String oldName, String newName) throws IOException;

  public String getObjectPath(String bucket, String object);

  public void getObject(String bucketName, String objectName, WalrusDataGetResponseType reply, Long size, Boolean isCompressed)
      throws WalrusException;

  public void getObject(String bucketName, String objectName, WalrusDataGetResponseType reply, Long byteRangeStart, Long byteRangeEnd,
      Boolean isCompressed) throws WalrusException;

  public void getMultipartObject(WalrusDataGetResponseType reply, List<PartInfo> parts, Boolean isCompressed) throws WalrusException;

  public void getMultipartObject(WalrusDataGetResponseType reply, List<PartInfo> parts, Boolean isCompressed, Long byteRangeStart, Long byteRangeEnd)
      throws WalrusException;

  public void disable() throws EucalyptusCloudException;

  public void enable() throws EucalyptusCloudException;

  public void stop() throws EucalyptusCloudException;

  public void check() throws EucalyptusCloudException;

  public void start() throws EucalyptusCloudException;
}
