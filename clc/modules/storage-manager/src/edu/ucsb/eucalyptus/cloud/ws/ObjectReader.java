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

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.util.WalrusDataMessage;
import edu.ucsb.eucalyptus.util.WalrusSemaphore;
import edu.ucsb.eucalyptus.storage.fs.FileIO;
import edu.ucsb.eucalyptus.storage.StorageManager;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class ObjectReader extends Thread {

    private static Logger LOG = Logger.getLogger(ObjectReader.class);

    private String bucketName;
    private String objectName;
    private long objectSize;
    private LinkedBlockingQueue<WalrusDataMessage> getQueue;
    private StorageManager storageManager;
    private long byteRangeStart;
    private long byteRangeEnd;
    private boolean compressed;
    private boolean deleteAfterXfer;
    private WalrusSemaphore semaphore;

    public ObjectReader(String bucketName, String objectName, long objectSize, LinkedBlockingQueue<WalrusDataMessage> getQueue, StorageManager storageManager) {
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.objectSize = objectSize;
        this.getQueue = getQueue;
        this.storageManager = storageManager;
    }


    public ObjectReader(String bucketName, String objectName, long objectSize, LinkedBlockingQueue<WalrusDataMessage> getQueue, long byteRangeStart, long byteRangeEnd, StorageManager storageManager) {
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.objectSize = objectSize;
        this.getQueue = getQueue;
        this.byteRangeStart = byteRangeStart;
        this.byteRangeEnd = byteRangeEnd;
        this.storageManager = storageManager;
    }

    public ObjectReader(String bucketName, String objectName, long objectSize, LinkedBlockingQueue<WalrusDataMessage> getQueue, boolean deleteAfterXfer, WalrusSemaphore semaphore, StorageManager storageManager) {
        this(bucketName, objectName, objectSize, getQueue, storageManager);
        this.deleteAfterXfer = deleteAfterXfer;
        this.semaphore = semaphore;
    }

    public void run() {
        FileIO fileIO = storageManager.prepareForRead(bucketName, objectName);

        long bytesRemaining = objectSize;
        long offset = byteRangeStart;

        if (byteRangeEnd > 0) {
            assert (byteRangeEnd <= objectSize);
            assert (byteRangeEnd >= byteRangeStart);
            bytesRemaining = byteRangeEnd - byteRangeStart;
            if (byteRangeEnd > objectSize || (byteRangeStart < 0))
                bytesRemaining = 0;
        }

        try {
            getQueue.put(WalrusDataMessage.StartOfData(bytesRemaining));

            while (bytesRemaining > 0) {
                int bytesRead = fileIO.read(offset);
                if (bytesRead < 0) {
                    LOG.error("Unable to read object: " + bucketName + "/" + objectName);
                    break;
                }
                if ((bytesRemaining - bytesRead) > 0)
                    getQueue.put(WalrusDataMessage.DataMessage(fileIO.getBuffer(), bytesRead));
                else
                    getQueue.put(WalrusDataMessage.DataMessage(fileIO.getBuffer(), (int) bytesRemaining));

                bytesRemaining -= bytesRead;
                offset += bytesRead;
            }
            fileIO.finish();
            getQueue.put(WalrusDataMessage.EOF());
        } catch (Exception ex) {
            LOG.error(ex, ex);
        }
        if (semaphore != null) {
            semaphore.release();
            synchronized (semaphore) {
                semaphore.notifyAll();
            }
        }
        if (deleteAfterXfer) {
            try {
                storageManager.deleteObject(bucketName, objectName);
            } catch (Exception ex) {
                LOG.error(ex, ex);
            }
        }
    }
}
