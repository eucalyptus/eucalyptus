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

package com.eucalyptus.objectstorage;

import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.component.Components;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.storage.msgs.BucketLogData;
import com.eucalyptus.ws.client.ServiceDispatcher;

/**
 * Handles bucket logging feature by writing log entries to the destination bucket
 *
 */
public class ObjectStorageBucketLogger {
	private Logger LOG = Logger.getLogger( ObjectStorageBucketLogger.class );
	private static ObjectStorageBucketLogger singleton;

	private static int LOG_THRESHOLD = 10;
	private static int LOG_PERIODICITY = 120;

	private LinkedBlockingQueue<BucketLogData> logData;
	private ConcurrentHashMap<String, LogFileEntry> logFileMap;
	ScheduledExecutorService logger;

	public ObjectStorageBucketLogger() {
		logData = new LinkedBlockingQueue<BucketLogData>();
		logFileMap = new ConcurrentHashMap<String, LogFileEntry>();
		logger = Executors.newSingleThreadScheduledExecutor();
		logger.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				
				if(logData.size() > LOG_THRESHOLD) {
					//dispatch
					Dispatcher dispatcher = ServiceDispatcher.lookupSingle(Components.lookup(ObjectStorage.class));
					List<BucketLogData> data = new ArrayList<BucketLogData>();
					
					/* TODO: zhill Fix this to issue object put
					 * For now, just drain the data so we don't bloat memory too much.
					 * Will fix later to issue object put with log data.
					 */
					logData.drainTo(data);
					data.clear();
					data = null;
					/*for(BucketLogData entry : data) {
						String bucket = entry.getTargetBucket();
						String uuid = UUID.randomUUID().toString();
						String key = entry.getTargetPrefix() + String.format("%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS-", Calendar.getInstance()) 
						+ uuid;

						if(!logFileMap.containsKey(bucket)) {
							//TODO: Fix this entire class. Must do s3-api operations, not direct logging.
						}
						try {
							LogFileEntry logFileEntry = logFileMap.get(bucket);
							FileChannel logChannel = logFileEntry.getChannel();
							String logString = entry.toFormattedString();
							logChannel.write(ByteBuffer.wrap(logString.getBytes()), logChannel.size());

							MessageDigest digest = Digest.MD5.get();
							digest.update(logString.getBytes());
							String etag = Hashes.bytesToHex(digest.digest());

							AddObjectType request = new AddObjectType();
							request.regarding( );
							request.setBucket(bucket);
							request.setKey(key);
							request.setObjectName(logFileEntry.getLogFileName());
							request.setEtag(etag);
							String ownerId = entry.getOwnerId();
							try {
								ArrayList<Grant> grants = new ArrayList<Grant>();
								grants.add(new Grant(new Grantee(new CanonicalUser(ownerId, Accounts.lookupAccountById(ownerId).getName())), 
								"FULL_CONTROL"));
								request.getAccessControlList().setGrants(grants);
							} catch (AuthException e1) {
								LOG.error(e1);
							}
							try {
								AsyncRequests.dispatch(osg, putRequest);
								//dispatcher.send(request);
							} catch (EucalyptusCloudException e) {
								LOG.error(e);
							}
						} catch (IOException e) {
							LOG.error(e);
						}
					}*/
					/*for(String bucket : logFileMap.keySet()) {
						try {
							logFileMap.get(bucket).getChannel().close();
						} catch (IOException e) {
							LOG.error(e);						
						}
					}*/
					logFileMap.clear();
				}
			}}, 1, LOG_PERIODICITY, TimeUnit.SECONDS);
	}

	public static ObjectStorageBucketLogger getInstance() {
		if (singleton == null) {
			singleton = new ObjectStorageBucketLogger();
		}		
		return singleton;
	}

	public void addLogEntry(BucketLogData logEntry) {
		try {
			logData.offer(logEntry, 500, TimeUnit.MILLISECONDS);
		} catch(InterruptedException ex) {
      Thread.currentThread( ).interrupt( );
		}
	}

	public BucketLogData makeLogEntry(String requestId) {
		return new BucketLogData(requestId);
	}

	private class LogFileEntry {
		private String logFileName;
		private FileChannel channel;

		public LogFileEntry(String logFileName, FileChannel channel) {
			this.logFileName = logFileName;
			this.channel = channel;
		}

		public FileChannel getChannel() {
			return channel;
		}

		public String getLogFileName() {
			return logFileName;
		}
	}
}
