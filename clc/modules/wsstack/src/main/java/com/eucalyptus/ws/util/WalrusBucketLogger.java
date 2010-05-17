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
package com.eucalyptus.ws.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Digest;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.scripting.groovy.GroovyUtil;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.client.ServiceDispatcher;

import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.cloud.entities.WalrusInfo;
import edu.ucsb.eucalyptus.msgs.AddObjectResponseType;
import edu.ucsb.eucalyptus.msgs.AddObjectType;
import edu.ucsb.eucalyptus.msgs.CanonicalUserType;
import edu.ucsb.eucalyptus.msgs.Grant;
import edu.ucsb.eucalyptus.msgs.Grantee;

public class WalrusBucketLogger {
	private Logger LOG = Logger.getLogger( WalrusBucketLogger.class );
	private static WalrusBucketLogger singleton;

	private static int LOG_THRESHOLD = 10;
	private static int LOG_PERIODICITY = 120;

	private LinkedBlockingQueue<BucketLogData> logData;
	private ConcurrentHashMap<String, LogFileEntry> logFileMap;
	ScheduledExecutorService logger;

	static { GroovyUtil.loadConfig("walruslogger.groovy"); }

	public WalrusBucketLogger() {
		logData = new LinkedBlockingQueue<BucketLogData>();
		logFileMap = new ConcurrentHashMap<String, LogFileEntry>();
		logger = Executors.newSingleThreadScheduledExecutor();
		logger.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if(logData.size() > LOG_THRESHOLD) {
					//dispatch
					Dispatcher dispatcher = ServiceDispatcher.lookupSingle(Component.walrus);
					List<BucketLogData> data = new ArrayList<BucketLogData>();
					logData.drainTo(data);
					for(BucketLogData entry : data) {
						String bucket = entry.getTargetBucket();
						String uuid = UUID.randomUUID().toString();
						String key = entry.getTargetPrefix() + String.format("%1$tY-%1$tm-%1$td-%1$tH-%1$tM-%1$tS-", Calendar.getInstance()) 
						+ uuid;

						if(!logFileMap.containsKey(bucket)) {
							//check if bucket exists, if not create it.
							try {
								String logFileName = "logentry-" + uuid;
								FileChannel channel = new FileOutputStream(new File(WalrusInfo.getWalrusInfo().getStorageDir() + 
										"/" + bucket + "/" + logFileName)).getChannel();
								logFileMap.put(bucket, new LogFileEntry(logFileName, channel));
							} catch (FileNotFoundException e) {
								LOG.error(e);
							}
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
							request.setUserId("admin");
							request.setEffectiveUserId("eucalyptus");
							request.setBucket(bucket);
							request.setKey(key);
							request.setObjectName(logFileEntry.getLogFileName());
							request.setEtag(etag);
							String ownerId = entry.getOwnerId();
							try {
								User userInfo = Users.lookupUser(ownerId);
								ArrayList<Grant> grants = new ArrayList<Grant>();
								grants.add(new Grant(new Grantee(new CanonicalUserType(userInfo.getQueryId(), ownerId)), 
								"FULL_CONTROL"));
								request.getAccessControlList().setGrants(grants);
							} catch (NoSuchUserException e1) {
								LOG.error(e1);
							}
							try {
								dispatcher.send(request);
							} catch (EucalyptusCloudException e) {
								LOG.error(e);
							}
						} catch (IOException e) {
							LOG.error(e);
						}
					}
					for(String bucket : logFileMap.keySet()) {
						try {
							logFileMap.get(bucket).getChannel().close();
						} catch (IOException e) {
							LOG.error(e);						
						}
					}
					logFileMap.clear();
				}
			}}, 1, LOG_PERIODICITY, TimeUnit.SECONDS);
	}

	public static WalrusBucketLogger getInstance() {
		if (singleton == null) {
			singleton = new WalrusBucketLogger();
		}		
		return singleton;
	}

	public void addLogEntry(BucketLogData logEntry) {
		try {
			logData.offer(logEntry, 500, TimeUnit.MILLISECONDS);
		} catch(InterruptedException ex) {

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
