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

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.ws.client.ServiceDispatcher;

import edu.ucsb.eucalyptus.cloud.BucketLogData;
import edu.ucsb.eucalyptus.msgs.PutObjectInlineResponseType;
import edu.ucsb.eucalyptus.msgs.PutObjectInlineType;
import edu.ucsb.eucalyptus.msgs.WalrusUsageStatsRecord;

public class WalrusBucketLogger {
	private Logger LOG = Logger.getLogger( WalrusBucketLogger.class );
	private static WalrusBucketLogger singleton;
	private static final int LOG_THRESHOLD = 0;

	private LinkedBlockingQueue<BucketLogData> logData;
	ScheduledExecutorService logger;

	public WalrusBucketLogger() {
		logData = new LinkedBlockingQueue<BucketLogData>();
		logger = Executors.newScheduledThreadPool(1);
		logger.schedule(new Runnable() {
			@Override
			public void run() {
				if(logData.size() > LOG_THRESHOLD) {
					//dispatch
					ServiceDispatcher dispatcher = ServiceDispatcher.lookupSingle(Component.walrus);
					List<BucketLogData> data = new ArrayList<BucketLogData>();
					logData.drainTo(data);
					for(BucketLogData entry : data) {
						try {
							PutObjectInlineType putRequest = new PutObjectInlineType();
							putRequest.setBucket(entry.getTargetBucket());
							putRequest.setKey(entry.getTargetPrefix() + "blah");
							PutObjectInlineResponseType putResponse = (PutObjectInlineResponseType) dispatcher.send(putRequest);
						} catch(EucalyptusCloudException ex) {

						}
					}
				}
			}}, 5, TimeUnit.SECONDS);
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
}
