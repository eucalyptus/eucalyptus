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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.walrus.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.List;

import com.eucalyptus.walrus.entities.PartInfo;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.stream.ChunkedInput;

import com.eucalyptus.context.Contexts;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.StorageManager;
import com.eucalyptus.walrus.WalrusBucketLogger;
import com.eucalyptus.walrus.entities.WalrusInfo;
import com.eucalyptus.walrus.msgs.WalrusDataGetRequestType;
import com.eucalyptus.walrus.util.WalrusProperties;

import edu.ucsb.eucalyptus.util.StreamConsumer;
import edu.ucsb.eucalyptus.util.SystemUtil;

import com.eucalyptus.storage.common.ChunkedDataFile;
import com.eucalyptus.storage.common.CompressedChunkedFile;
import com.eucalyptus.storage.common.fs.FileIO;
import com.eucalyptus.storage.common.fs.FileReader;
import com.eucalyptus.storage.common.fs.FileWriter;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.system.BaseDirectory;

public class FileSystemStorageManager implements StorageManager {

    public static final String FILE_SEPARATOR = "/";
    public static final String lvmRootDirectory = "/dev";
    private static boolean initialized = false;
    public static final String EUCA_ROOT_WRAPPER = BaseDirectory.LIBEXEC.toString() + "/euca_rootwrap";
    public static final int MAX_LOOP_DEVICES = 256;
    private static Logger LOG = Logger.getLogger(FileSystemStorageManager.class);

    public FileSystemStorageManager() {
    }

    public void checkPreconditions() throws EucalyptusCloudException {
        try {
            if(!new File(EUCA_ROOT_WRAPPER).exists()) {
                throw new EucalyptusCloudException("root wrapper (euca_rootwrap) does not exist");
            }
            String returnValue = getLvmVersion();
            if(returnValue.length() == 0) {
                throw new EucalyptusCloudException("Is lvm installed?");
            } else {
                LOG.info(returnValue);
            }
        } catch(EucalyptusCloudException ex) {
            String error = "Unable to run command: " + ex.getMessage();
            LOG.error(error);
            throw new EucalyptusCloudException(error);
        }
    }

    public boolean bucketExists(String bucket) {
        return new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket).exists();
    }

    public boolean objectExists(String bucket, String object) {
        return new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object).exists();
    }

    public void createBucket(String bucket) throws IOException {
        File bukkit = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket);
        if(!bukkit.exists()) {
            if(!bukkit.mkdirs()) {
                throw new IOException("Unable to create bucket: " + bucket);
            }
        }
    }

    public long getSize(String bucket, String object) {
        File objectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
        if(objectFile.exists())
            return objectFile.length();
        return -1;
    }

    public void deleteBucket(String bucket) throws IOException {
        File bukkit = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket);
        if(!bukkit.delete()) {
            throw new IOException("Unable to delete bucket: " + bucket);
        }
    }

    public void createObject(String bucket, String object) throws IOException {
        File objectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
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
        File objectFile = new File (path);
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
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
            throw ex;
        } finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                LOG.error( ex );
            }
        }
        return bytesRead;
    }

    public void deleteObject(String bucket, String object) throws IOException {
        File objectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
        if (objectFile.exists()) {
            if(!objectFile.delete()) {
                throw new IOException("Unable to delete: " + objectFile.getAbsolutePath());
            }
        }
    }

    public void deleteAbsoluteObject(String object) throws IOException {
        File objectFile = new File (object);
        if (objectFile.exists()) {
            if(!objectFile.delete()) {
                throw new IOException("Unable to delete: " + object);
            }
        }
    }

    public void putObject(String bucket, String object, byte[] base64Data, boolean append) throws IOException {
        File objectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object);
        if (!objectFile.exists()) {
            objectFile.createNewFile();
        }
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(objectFile, append));
        try {
            outputStream.write(base64Data);
        } catch (IOException ex) {
            LOG.error( ex );
            Logs.extreme( ).error( ex, ex );
            throw ex;
        } finally {
            try {
                outputStream.close();
            } catch (IOException ex) {
                LOG.error( ex );
            }
        }
    }

    public void renameObject(String bucket, String oldName, String newName) throws IOException {
        File oldObjectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + oldName);
        File newObjectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + newName);
        if(oldObjectFile.exists()) {
            if (!oldObjectFile.renameTo(newObjectFile)) {
                throw new IOException("Unable to rename " + oldObjectFile.getAbsolutePath() + " to " + newObjectFile.getAbsolutePath());
            }
        }
    }

    public void copyObject(String sourceBucket, String sourceObject, String destinationBucket, String destinationObject) throws IOException {
        File oldObjectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + sourceBucket + FILE_SEPARATOR + sourceObject);
        File newObjectFile = new File (WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + destinationBucket + FILE_SEPARATOR + destinationObject);
        if(!oldObjectFile.equals(newObjectFile)) {
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
                LOG.error( ex );
                Logs.extreme( ).error( ex, ex );
                throw ex;
            }  finally {
                try {
                    if(fileIn != null)
                        fileIn.close();
                } catch(IOException e) {
                    LOG.error( e );
                }
                try {
                    if( fileInputStream != null)
                        fileInputStream.close();
                } catch(IOException e) {
                    LOG.error( e );
                }
                try {
                    if(fileOut != null)
                        fileOut.close();
                } catch(IOException e) {
                    LOG.error( e );
                }
                try {
                    if(fileOutputStream != null)
                        fileOutputStream.close();
                } catch(IOException e) {
                    LOG.error( e );
                }
            }
        }
    }

    public String getObjectPath(String bucket, String object) {
        return WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object;
    }

    public long getObjectSize(String bucket, String object) {
        String absoluteObjectPath = WalrusInfo.getWalrusInfo().getStorageDir() + FILE_SEPARATOR + bucket + FILE_SEPARATOR + object;

        File objectFile = new File(absoluteObjectPath);
        if(objectFile.exists())
            return objectFile.length();
        return -1;
    }

    public void sendObject(final WalrusDataGetRequestType request, DefaultHttpResponse httpResponse, String bucketName, String objectName, long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed, String versionId, final BucketLogData logData) {
        try {
            Channel channel = request.getChannel();
            RandomAccessFile raf = new RandomAccessFile(new File(getObjectPath(bucketName, objectName)), "r");
            httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream" );
            if(etag != null)
                httpResponse.addHeader(HttpHeaders.Names.ETAG, etag);
            httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, lastModified);
            if(contentDisposition != null)
                httpResponse.addHeader("Content-Disposition", contentDisposition);
            final ChunkedInput file;
            isCompressed = isCompressed == null ? false : isCompressed;
            if(isCompressed) {
                file = new CompressedChunkedFile(raf, size);
            } else {
                file = new ChunkedDataFile(raf, 0, size, 8192);
                httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(size));
            }
            if(logData != null) {
                logData.setTurnAroundTime(System.currentTimeMillis() - logData.getTurnAroundTime());
                logData.setBytesSent(size);
            }
            if(versionId != null) {
                httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, versionId);
            }
            channel.write(httpResponse);
            channel.write(file).addListener(new ChannelFutureListener( ) {
                @Override public void operationComplete( ChannelFuture future ) throws Exception {
                    Contexts.clear(request.getCorrelationId());
                    file.close();
                    if(logData != null) {
                        logData.setTotalTime(System.currentTimeMillis() - logData.getTotalTime());
                        WalrusBucketLogger.getInstance().addLogEntry(logData);
                    }
                }
            });
        } catch(Exception ex) {
            LOG.error(ex, ex);
        }
    }

    public void sendObject(final WalrusDataGetRequestType request, DefaultHttpResponse httpResponse, String bucketName, String objectName, long start, long end, long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed, String versionId, final BucketLogData logData) {
        try {
            Channel channel = request.getChannel();
            RandomAccessFile raf = new RandomAccessFile(new File(getObjectPath(bucketName, objectName)), "r");
            httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream" );
            if(etag != null)
                httpResponse.addHeader(HttpHeaders.Names.ETAG, etag);
            httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, lastModified);
            if(contentDisposition != null)
                httpResponse.addHeader("Content-Disposition", contentDisposition);
            final ChunkedInput file;
            isCompressed = isCompressed == null ? false : isCompressed;
            if(isCompressed) {
                file = new CompressedChunkedFile(raf, start, end, (int)Math.min((end - start), 8192));
            } else {
                file = new ChunkedDataFile(raf, start, (int)(end - start), (int)Math.min((end - start), 8192));
                httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf((end - start)));
            }
            httpResponse.addHeader("Content-Range", start + "-" + (end -1) + "/" + size);
            if(logData != null) {
                logData.setTurnAroundTime(System.currentTimeMillis() - logData.getTurnAroundTime());
                logData.setBytesSent(size);
            }
            if(versionId != null) {
                httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, versionId);
            }
            channel.write(httpResponse);
            channel.write(file).addListener(new ChannelFutureListener( ) {
                @Override public void operationComplete( ChannelFuture future ) throws Exception {
                    Contexts.clear(request.getCorrelationId());
                    file.close();
                    if(logData != null) {
                        logData.setTotalTime(System.currentTimeMillis() - logData.getTotalTime());
                        WalrusBucketLogger.getInstance().addLogEntry(logData);
                    }
                }
            });
        } catch(Exception ex) {
            LOG.error(ex, ex);
        }
    }

    public void sendHeaders(final WalrusDataGetRequestType request, DefaultHttpResponse httpResponse, Long size, String etag,
                            String lastModified, String contentType, String contentDisposition, String versionId, final BucketLogData logData) {
        Channel channel = request.getChannel();
        httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(size));
        httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream" );
        if(etag != null)
            httpResponse.addHeader(HttpHeaders.Names.ETAG, etag);
        httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, lastModified);
        if(contentDisposition != null)
            httpResponse.addHeader("Content-Disposition", contentDisposition);
        if(versionId != null) {
            httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, versionId);
        }
        if(logData != null) {
            logData.setTurnAroundTime(System.currentTimeMillis() - logData.getTurnAroundTime());
        }
        channel.write(httpResponse).addListener(new ChannelFutureListener( ) {
            @Override public void operationComplete( ChannelFuture future ) throws Exception {
                Contexts.clear(request.getCorrelationId());
                if(logData != null) {
                    logData.setTotalTime(System.currentTimeMillis() - logData.getTotalTime());
                    WalrusBucketLogger.getInstance().addLogEntry(logData);
                }
            }
        });
    }

    private String removeLoopback(String loDevName) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "losetup", "-d", loDevName});
    }

    private int losetup(String absoluteFileName, String loDevName) {
        try
        {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(new String[]{EUCA_ROOT_WRAPPER, "losetup", loDevName, absoluteFileName});
            StreamConsumer error = new StreamConsumer(proc.getErrorStream());
            StreamConsumer output = new StreamConsumer(proc.getInputStream());
            error.start();
            output.start();
            int errorCode = proc.waitFor();
            output.join();
            LOG.info("losetup " + loDevName + " " + absoluteFileName);
            LOG.info(output.getReturnValue());
            LOG.info(error.getReturnValue());
            return errorCode;
        } catch (Exception t) {
            LOG.error(t);
        }
        return -1;
    }

    private String findFreeLoopback() throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "losetup", "-f"}).replaceAll("\n", "");
    }

    private String removeLogicalVolume(String lvName) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvremove", "-f", lvName});
    }

    private String reduceVolumeGroup(String vgName, String pvName) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "vgreduce", vgName, pvName});
    }

    private String removePhysicalVolume(String loDevName) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "pvremove", loDevName});
    }

    private String createVolumeFromLv(String lvName, String volumeKey) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "dd", "if=" + lvName, "of=" + volumeKey, "bs=1M"});
    }

    private String enableLogicalVolume(String lvName) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvchange", "-ay", lvName});
    }

    private String disableLogicalVolume(String lvName) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvchange", "-an", lvName});
    }

    private String removeVolumeGroup(String vgName) throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "vgremove", vgName});
    }

    private String getLvmVersion() throws EucalyptusCloudException {
        return SystemUtil.run(new String[]{EUCA_ROOT_WRAPPER, "lvm", "version"});
    }



    public String createLoopback(String fileName) throws EucalyptusCloudException {
        int number_of_retries = 0;
        int status = -1;
        String loDevName;
        do {
            loDevName = findFreeLoopback();
            if(loDevName.length() > 0) {
                status = losetup(fileName, loDevName);
            }
            if(number_of_retries++ >= MAX_LOOP_DEVICES)
                break;
        } while(status != 0);

        if(status != 0) {
            throw new EucalyptusCloudException("Could not create loopback device for " + fileName +
                    ". Please check the max loop value and permissions");
        }
        return loDevName;
    }

    @Override
    public void enable() throws EucalyptusCloudException {
        //Nothing to do yet.
    }

    @Override
    public void disable() throws EucalyptusCloudException {
        //Nothing to do yet.
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
    public void sendObject(final WalrusDataGetRequestType request, DefaultHttpResponse httpResponse, List<PartInfo> parts, Long size, String etag, String lastModified, String contentType, String contentDisposition, Boolean isCompressed, String versionId) {
        try {
            Channel channel = request.getChannel();
            httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream" );
            if(etag != null)
                httpResponse.addHeader(HttpHeaders.Names.ETAG, etag);
            httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, lastModified);
            if(contentDisposition != null)
                httpResponse.addHeader("Content-Disposition", contentDisposition);
            if(versionId != null) {
                httpResponse.addHeader(WalrusProperties.X_AMZ_VERSION_ID, versionId);
            }
            httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(size));
            channel.write(httpResponse);

            for (PartInfo part : parts) {
                isCompressed = isCompressed == null ? false : isCompressed;
                final ChunkedInput file;
                RandomAccessFile raf = new RandomAccessFile(new File(getObjectPath(part.getBucketName(), part.getObjectName())), "r");
                if(isCompressed) {
                    file = new CompressedChunkedFile(raf, part.getSize());
                } else {
                    file = new ChunkedDataFile(raf, 0, part.getSize(), 8192);
                }
                channel.write(file).addListener(new ChannelFutureListener( ) {
                    @Override public void operationComplete( ChannelFuture future ) throws Exception {
                        file.close();
                    }
                });
            }
        } catch(Exception ex) {
            LOG.error(ex, ex);
        }
    }

}
