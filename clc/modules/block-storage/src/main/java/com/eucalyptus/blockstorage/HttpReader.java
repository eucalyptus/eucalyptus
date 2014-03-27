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

package com.eucalyptus.blockstorage;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataMessage;
import edu.ucsb.eucalyptus.util.StreamConsumer;



@Deprecated
public class HttpReader extends HttpTransfer {

	private static Logger LOG = Logger.getLogger(HttpReader.class);

	private LinkedBlockingQueue<ObjectStorageDataMessage> getQueue;
	private File file;
	private String tempPath;
	private boolean compressed;

	public HttpReader(String path, LinkedBlockingQueue<ObjectStorageDataMessage> getQueue, File file, String eucaOperation, String eucaHeader) {
		this.getQueue = getQueue;
		this.file = file;
		httpClient = new HttpClient();

		String httpVerb = "GET";
		String addr = StorageProperties.WALRUS_URL + "/" + path;

		method = constructHttpMethod(httpVerb, addr, eucaOperation, eucaHeader,true);
		//signEucaInternal(method);
	}

	public HttpReader(String path, LinkedBlockingQueue<ObjectStorageDataMessage> getQueue, File file, String eucaOperation, String eucaHeader, boolean compressed, String tempPath) {
		this(path, getQueue, file, eucaOperation, eucaHeader);
		this.compressed = compressed;
		this.tempPath = tempPath;
	}

	public String getResponseAsString() {
		try {
			httpClient.executeMethod(method);
			InputStream inputStream;
			if(compressed) {
				inputStream = new GZIPInputStream(method.getResponseBodyAsStream());
			} else {
				inputStream = method.getResponseBodyAsStream();
			}

			String responseString = "";
			byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
			int bytesRead;
			while((bytesRead = inputStream.read(bytes)) > 0) {
				responseString += new String(bytes, 0 , bytesRead);
			}
			method.releaseConnection();
			return responseString;
		} catch(Exception ex) {
			LOG.error(ex, ex);
		}
		return null;
	}

	private void getResponseToFile() {
		byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
		FileOutputStream fileOutputStream = null;
		BufferedOutputStream bufferedOut = null;
		try {
			File outFile = null;
			File outFileUncompressed = null;
			if(compressed) {
				String outFileNameUncompressed = tempPath + File.pathSeparator + file.getName() + Hashes.getRandom(16);
				outFileUncompressed = new File(outFileNameUncompressed);
				outFile = new File(outFileNameUncompressed + ".gz");		
			} else {
				outFile = file;
			}

			httpClient.executeMethod(method);

			// GZIPInputStream has a bug thats corrupting snapshot file system. Mounting the block device failed with unknown file system error
			/*InputStream httpIn = null;
			if(compressed) {
				httpIn = new GZIPInputStream(method.getResponseBodyAsStream());
			}
			else {
				httpIn = method.getResponseBodyAsStream();				
			}*/

			InputStream httpIn =  method.getResponseBodyAsStream();
			int bytesRead;
			fileOutputStream = new FileOutputStream(outFile);
			// fileOutputStream = new FileOutputStream(file);
			bufferedOut = new BufferedOutputStream(fileOutputStream);
			while((bytesRead = httpIn.read(bytes)) > 0) {
				bufferedOut.write(bytes, 0, bytesRead);
			}
			bufferedOut.close();

			if (compressed) {
				try
				{
					Runtime rt = Runtime.getRuntime();
					Process proc = rt.exec(new String[]{ "/bin/gunzip", outFile.getAbsolutePath()});
					StreamConsumer error = new StreamConsumer(proc.getErrorStream());
					StreamConsumer output = new StreamConsumer(proc.getInputStream());
					error.start();
					output.start();
					output.join();
					error.join();
				} catch (Exception t) {
					LOG.error(t);
				}
				if ((outFileUncompressed != null) && (!outFileUncompressed.renameTo(file))) {
					LOG.error("Unable to uncompress: " + outFile.getAbsolutePath());
					return;
				}
			}
		} catch (Exception ex) {
			LOG.error(ex, ex);
		} finally {
			method.releaseConnection();
			if(bufferedOut != null) {
				try {
					bufferedOut.close();
				} catch (IOException e) {
					LOG.error(e);	
				}
			}
			if(fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					LOG.error(e);	
				}
			}
		}
	}

	private void getResponseToQueue() {
		byte[] bytes = new byte[StorageProperties.TRANSFER_CHUNK_SIZE];
		try {
			httpClient.executeMethod(method);
			InputStream httpIn = method.getResponseBodyAsStream();
			int bytesRead;
			getQueue.add(ObjectStorageDataMessage.StartOfData(0));
			while((bytesRead = httpIn.read(bytes)) > 0) {
				getQueue.add(ObjectStorageDataMessage.DataMessage(bytes, bytesRead));
			}
			getQueue.add(ObjectStorageDataMessage.EOF());
		} catch (Exception ex) {
			LOG.error(ex, ex);
		} finally {
			method.releaseConnection();
		}
	}

	public void run() {
		if(getQueue != null) {
			getResponseToQueue();
		} else if(file != null) {
			getResponseToFile();
		}
	}

	public String getResponseHeader(String headerName) {
		try {
			httpClient.executeMethod(method);
			Header value = method.getResponseHeader(headerName);
			method.releaseConnection();
			if(value != null)
			    return value.getValue();
		} catch(Exception ex) {
			LOG.error(ex, ex);
		}
		return null;
	}
}
