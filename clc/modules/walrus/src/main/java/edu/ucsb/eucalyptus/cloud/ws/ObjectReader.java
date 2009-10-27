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
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.ws;

import edu.ucsb.eucalyptus.storage.StorageManager;
import edu.ucsb.eucalyptus.storage.fs.FileIO;
import edu.ucsb.eucalyptus.util.WalrusDataMessage;
import edu.ucsb.eucalyptus.util.EucaSemaphore;
import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
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
	private EucaSemaphore semaphore;

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

	public ObjectReader(String bucketName, String objectName, long objectSize, LinkedBlockingQueue<WalrusDataMessage> getQueue, boolean deleteAfterXfer, EucaSemaphore semaphore, StorageManager storageManager) {
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
					if ((bytesRemaining - bytesRead) > 0) {
						ByteBuffer buffer = fileIO.getBuffer();
						if(buffer != null)
							getQueue.put(WalrusDataMessage.DataMessage(buffer, bytesRead));
					} else {
						ByteBuffer buffer = fileIO.getBuffer();
						if(buffer != null)
							getQueue.put(WalrusDataMessage.DataMessage(buffer, (int) bytesRemaining));
					}
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
