/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileIO;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;
import edu.ucsb.eucalyptus.util.WalrusSemaphore;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

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
        try {
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
            	LOG.warn("putting data");
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
        } catch(Exception ex) {
            try {
                getQueue.put(WalrusDataMessage.EOF());
            } catch(InterruptedException e) {
                LOG.error(e);
            }
        }
    }
}
