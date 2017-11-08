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

package com.eucalyptus.walrus.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.stream.ChunkedInput;

import com.eucalyptus.records.Logs;
import com.eucalyptus.storage.common.ChunkedDataFile;
import com.eucalyptus.storage.common.CompressedChunkedFile;
import com.eucalyptus.storage.common.fs.FileIO;
import com.eucalyptus.storage.common.fs.FileReader;
import com.eucalyptus.storage.common.fs.FileWriter;
import com.eucalyptus.system.BaseDirectory;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.StorageManager;
import com.eucalyptus.walrus.entities.PartInfo;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.WalrusDataGetResponseType;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;

public class FileSystemStorageManager implements StorageManager {

  public static final String FILE_SEPARATOR = "/";
  private static Logger LOG = Logger.getLogger(FileSystemStorageManager.class);

  public FileSystemStorageManager() {}

  public void checkPreconditions() throws EucalyptusCloudException {
  }

  public boolean bucketExists(String bucket) {
    return new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket).exists();
  }

  public boolean objectExists(String bucket, String object) {
    return new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object).exists();
  }

  public void createBucket(String bucket) throws IOException {
    File bukkit = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket);
    if (!bukkit.exists()) {
      if (!bukkit.mkdirs()) {
        throw new IOException("Unable to create bucket: " + bucket);
      }
    }
  }

  public long getSize(String bucket, String object) {
    File objectFile = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
    if (objectFile.exists())
      return objectFile.length();
    return -1;
  }

  public void deleteBucket(String bucket) throws IOException {
    File bukkit = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket);
    if (bukkit.exists() && !bukkit.delete()) {
      throw new IOException("Unable to delete bucket: " + bucket);
    }
  }

  public void createObject(String bucket, String object) throws IOException {
    File objectFile = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
    if (!objectFile.exists()) {
      if (!objectFile.createNewFile()) {
        throw new IOException("Unable to create: " + objectFile.getAbsolutePath());
      }
    }
  }

  public FileIO prepareForRead(String bucket, String object) throws Exception {
    return new FileReader(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
  }

  public FileIO prepareForWrite(String bucket, String object) throws Exception {
    return new FileWriter(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
  }

  public int readObject(String bucket, String object, byte[] bytes, long offset) throws IOException {
    return readObject(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object, bytes, offset);
  }

  public int readObject(String path, byte[] bytes, long offset) throws IOException {
    File objectFile = new File(path);
    if (!objectFile.exists()) {
      throw new IOException("Unable to read: " + path);
    }
    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(objectFile));
    int bytesRead = 0;
    try {
      if (offset > 0) {
        inputStream.skip(offset);
      }
      bytesRead = inputStream.read(bytes);
    } catch (IOException ex) {
      LOG.error(ex);
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      try {
        inputStream.close();
      } catch (IOException ex) {
        LOG.error(ex);
      }
    }
    return bytesRead;
  }

  public void deleteObject(String bucket, String object) throws IOException {
    File objectFile = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
    if (objectFile.exists()) {
      if (!objectFile.delete()) {
        throw new IOException("Unable to delete: " + objectFile.getAbsolutePath());
      }
    }
  }

  public void deleteAbsoluteObject(String object) throws IOException {
    File objectFile = new File(object);
    if (objectFile.exists()) {
      if (!objectFile.delete()) {
        throw new IOException("Unable to delete: " + object);
      }
    }
  }

  public void putObject(String bucket, String object, byte[] base64Data, boolean append) throws IOException {
    File objectFile = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
    if (!objectFile.exists()) {
      objectFile.createNewFile();
    }
    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(objectFile, append));
    try {
      outputStream.write(base64Data);
    } catch (IOException ex) {
      LOG.error(ex);
      Logs.extreme().error(ex, ex);
      throw ex;
    } finally {
      try {
        outputStream.close();
      } catch (IOException ex) {
        LOG.error(ex);
      }
    }
  }

  public void renameObject(String bucket, String oldName, String newName) throws IOException {
    File oldObjectFile = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + oldName);
    File newObjectFile = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + newName);
    if (oldObjectFile.exists()) {
      if (!oldObjectFile.renameTo(newObjectFile)) {
        throw new IOException("Unable to rename " + oldObjectFile.getAbsolutePath() + " to " + newObjectFile.getAbsolutePath());
      }
    }
  }

  public void copyObject(String sourceBucket, String sourceObject, String destinationBucket, String destinationObject) throws IOException {
    File oldObjectFile = new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + sourceBucket + FILE_SEPARATOR + sourceObject);
    File newObjectFile =
        new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + destinationBucket + FILE_SEPARATOR + destinationObject);
    if (!oldObjectFile.equals(newObjectFile)) {
      FileInputStream fileInputStream = null;
      FileChannel fileIn = null;
      FileOutputStream fileOutputStream = null;
      FileChannel fileOut = null;
      try {
        fileInputStream = new FileInputStream(oldObjectFile);
        fileIn = fileInputStream.getChannel();
        fileOutputStream = new FileOutputStream(newObjectFile);
        fileOut = fileOutputStream.getChannel();
        fileIn.transferTo(0, fileIn.size(), fileOut);
      } catch (IOException ex) {
        LOG.error(ex);
        Logs.extreme().error(ex, ex);
        throw ex;
      } finally {
        try {
          if (fileIn != null)
            fileIn.close();
        } catch (IOException e) {
          LOG.error(e);
        }
        try {
          if (fileInputStream != null)
            fileInputStream.close();
        } catch (IOException e) {
          LOG.error(e);
        }
        try {
          if (fileOut != null)
            fileOut.close();
        } catch (IOException e) {
          LOG.error(e);
        }
        try {
          if (fileOutputStream != null)
            fileOutputStream.close();
        } catch (IOException e) {
          LOG.error(e);
        }
      }
    }
  }

  @Override
  public void copyMultipartObject(List<PartInfo> parts, String destinationBucket, String destinationObject) throws Exception {
    Iterator<PartInfo> partIterator = null;

    if (parts != null && (partIterator = parts.iterator()) != null && partIterator.hasNext()) {
      try {
        File newObjectFile =
            new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + destinationBucket + FILE_SEPARATOR + destinationObject);
        FileInputStream fileInputStream = null;
        FileChannel fileIn = null;
        FileOutputStream fileOutputStream = null;
        FileChannel fileOut = null;
        try {
          fileOutputStream = new FileOutputStream(newObjectFile);
          fileOut = fileOutputStream.getChannel();
          PartInfo part = null;
          File partFile = null;

          do {
            part = partIterator.next();
            partFile =
                new File(WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + part.getBucketName() + FILE_SEPARATOR + part.getObjectName());
            fileInputStream = new FileInputStream(partFile);
            fileIn = fileInputStream.getChannel();
            fileIn.transferTo(0, fileIn.size(), fileOut);

            // Get ready for next iteration
            try {
              fileIn.close();
              fileIn = null;
            } catch (IOException e) {
              LOG.warn("Failed to close channel", e);
            }
            try {
              fileInputStream.close();
              fileInputStream = null;
            } catch (IOException e) {
              LOG.warn("Failed to close stream", e);
            }
            partFile = null;
            part = null;
          } while (partIterator.hasNext());
        } finally {
          if (fileIn != null) {
            try {
              fileIn.close();
            } catch (IOException e) {
              LOG.warn("Failed to close channel", e);
            }
          }
          if (fileInputStream != null) {
            try {
              fileInputStream.close();
            } catch (IOException e) {
              LOG.warn("Failed to close stream", e);
            }
          }
          if (fileOut != null) {
            try {
              fileOut.close();
            } catch (IOException e) {
              LOG.warn("Failed to close channel", e);
            }
          }
          if (fileOutputStream != null) {
            try {
              fileOutputStream.close();
            } catch (IOException e) {
              LOG.warn("Failed to close stream", e);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to copy multipart source object to " + destinationObject, e);
        throw e;
      }
    } else {
      LOG.warn("No parts to copy content from and create " + destinationObject);
    }
  }

  public String getObjectPath(String bucket, String object) {
    return WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object;
  }

  public long getObjectSize(String bucket, String object) {
    String absoluteObjectPath = WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object;

    File objectFile = new File(absoluteObjectPath);
    if (objectFile.exists())
      return objectFile.length();
    return -1;
  }

  @Override
  public void enable() throws EucalyptusCloudException {
    // Nothing to do yet.
  }

  @Override
  public void disable() throws EucalyptusCloudException {
    // Nothing to do yet.
  }

  @Override
  public void stop() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void check() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void start() throws EucalyptusCloudException {
    // TODO Auto-generated method stub

  }

  @Override
  public void getObject(String bucketName, String objectName, final WalrusDataGetResponseType response, Long size, Boolean isCompressed)
      throws WalrusException {
    try {
      RandomAccessFile raf = new RandomAccessFile(new File(getObjectPath(bucketName, objectName)), "r");
      final ChunkedInput file;
      isCompressed = isCompressed == null ? false : isCompressed;
      if (isCompressed) {
        file = new CompressedChunkedFile(raf, size);
      } else {
        file = new ChunkedDataFile(raf, 0, size, 8192);
      }
      List<ChunkedInput> dataStreams = new ArrayList<ChunkedInput>();
      dataStreams.add(file);
      response.setDataInputStream(dataStreams);
    } catch (IOException ex) {
      throw new WalrusException(ex.getMessage());
    }
  }

  @Override
  public void getObject(String bucketName, String objectName, final WalrusDataGetResponseType response, Long byteRangeStart, Long byteRangeEnd,
      Boolean isCompressed) throws WalrusException {
    try {
      RandomAccessFile raf = new RandomAccessFile(new File(getObjectPath(bucketName, objectName)), "r");
      final ChunkedInput file;
      isCompressed = isCompressed == null ? false : isCompressed;
      if (isCompressed) {
        file = new CompressedChunkedFile(raf, byteRangeStart, byteRangeEnd, (int) Math.min((byteRangeEnd - byteRangeStart), 8192));
      } else {
        file = new ChunkedDataFile(raf, byteRangeStart, (int) (byteRangeEnd - byteRangeStart), (int) Math.min((byteRangeEnd - byteRangeStart), 8192));
      }
      List<ChunkedInput> dataStreams = new ArrayList<>();
      dataStreams.add(file);
      response.setDataInputStream(dataStreams);
    } catch (IOException ex) {
      throw new WalrusException(ex.getMessage());
    }
  }

  @Override
  public void getMultipartObject(WalrusDataGetResponseType reply, List<PartInfo> parts, Boolean isCompressed) throws WalrusException {
    try {
      List<ChunkedInput> dataStreams = new ArrayList<>();
      for (PartInfo part : parts) {
        isCompressed = isCompressed == null ? false : isCompressed;
        final ChunkedInput file;
        RandomAccessFile raf = new RandomAccessFile(new File(getObjectPath(part.getBucketName(), part.getObjectName())), "r");
        if (isCompressed) {
          file = new CompressedChunkedFile(raf, part.getSize());
        } else {
          file = new ChunkedDataFile(raf, 0, part.getSize(), 8192);
        }
        dataStreams.add(file);
      }
      reply.setDataInputStream(dataStreams);
    } catch (IOException ex) {
      throw new WalrusException(ex.getMessage());
    }
  }

  @Override
  public void getMultipartObject(WalrusDataGetResponseType reply, List<PartInfo> parts, Boolean isCompressed, Long byteRangeStart, Long byteRangeEnd)
      throws WalrusException {
    try {
      List<ChunkedInput> dataStreams = new ArrayList<>();
      isCompressed = isCompressed == null ? false : isCompressed;

      Long requestedSize = byteRangeEnd - byteRangeStart + 1; // Assuming byteRangeEnd is inclusive
      Iterator<PartInfo> partIterator = parts.iterator();
      PartInfo part = null;

      // Compute the part to begin reading from and the starting offset in that part
      Long rangeElapsed = -1L;
      Long startMarker = 0L;
      while (partIterator.hasNext()) {
        part = partIterator.next();
        rangeElapsed += part.getSize();
        if (byteRangeStart <= rangeElapsed) {
          startMarker = part.getSize() - (rangeElapsed - byteRangeStart + 1);
          break;
        }
      }

      // Keep adding bytes from parts till the requested length is met
      Long bytesRead = 0L;
      Long tempLength = 0L;
      do {
        if (part == null && partIterator.hasNext()) {
          part = partIterator.next();
        }

        final ChunkedInput file;
        RandomAccessFile raf = new RandomAccessFile(new File(getObjectPath(part.getBucketName(), part.getObjectName())), "r");

        if (requestedSize > ((part.getSize() - startMarker) + bytesRead)) { // if the part is smaller than what is required, add the part from the
                                                                            // startMarker to end
          tempLength = part.getSize() - startMarker;
        } else { // if the part is larger than what is required, add the part from the startMarker to how much is necessary
          tempLength = requestedSize - bytesRead;
        }

        if (isCompressed) {
          file = new CompressedChunkedFile(raf, startMarker, tempLength, (int) Math.min(tempLength, 8192));
        } else {
          file = new ChunkedDataFile(raf, startMarker, tempLength, (int) Math.min(tempLength, 8192));
        }

        dataStreams.add(file);
        bytesRead = bytesRead + tempLength;
        startMarker = 0L;
        tempLength = 0L;
        part = null;
      } while (bytesRead < requestedSize && partIterator.hasNext());

      reply.setDataInputStream(dataStreams);
    } catch (IOException ex) {
      throw new WalrusException(ex.getMessage());
    }
  }

}
