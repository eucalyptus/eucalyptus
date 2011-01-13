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
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.storage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import edu.ucsb.eucalyptus.cloud.entities.SANInfo;


public class ShellSessionManager implements SessionManager {
	private BufferedWriter writer;
	private BufferedReader reader;
	private Channel channel;
	private final ExecutorService pool;

	private static Logger LOG = Logger.getLogger(ShellSessionManager.class);
	private final int NUM_THREADS = 1;

	public ShellSessionManager() {
		pool = Executors.newFixedThreadPool(NUM_THREADS);
	}

	public void checkConnection() throws EucalyptusCloudException {
		if(channel != null) {
			if(!channel.isConnected())
				connect();
		} else {
			connect();
		}
	}

	public void connect() throws EucalyptusCloudException {
		try {
			JSch jsch = new JSch();
			Session session;
			SANInfo sanInfo = SANInfo.getStorageInfo();
			session = jsch.getSession(sanInfo.getSanUser(), sanInfo.getSanHost());
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(sanInfo.getSanPassword());
			session.connect();
			channel = session.openChannel("shell");
			PipedOutputStream outStream = new PipedOutputStream();
			channel.setInputStream(new PipedInputStream(outStream));
			PipedInputStream inStream = new PipedInputStream();
			channel.setOutputStream(new PipedOutputStream(inStream));
			channel.connect();
			writer = new BufferedWriter(new OutputStreamWriter(outStream, "utf-8"));
			reader = new BufferedReader(new InputStreamReader(inStream, "utf-8"));
		} catch (JSchException e) {
			throw new EucalyptusCloudException(e);
		} catch (UnsupportedEncodingException e) {
			throw new EucalyptusCloudException(e);
		} catch (IOException e) {
			throw new EucalyptusCloudException(e);
		}
	}

	public void refresh() throws EucalyptusCloudException {
		try {
			channel.disconnect();
			channel.getSession().disconnect();
			connect();
		} catch (JSchException e) {
			throw new EucalyptusCloudException(e);
		}
	}

	public void addTask(final AbstractSANTask task) throws InterruptedException {
		pool.execute(new Runnable() {		
			@Override
			public void run() {
				try {
					writer.write("" + task.getCommand() + task.getEOFCommand());
					writer.flush();
					String returnValue = "";
					for (String line = null; (line = reader.readLine()) != null;) {
						line = line + "\r";
						if(line.contains("" + task.getEOFCommand()))
							break;
						returnValue += line;
					}
					synchronized (task) {
						task.setValue(returnValue);
						task.notifyAll();
					}
				} catch (IOException e) {
					LOG.error(e);
					try {
						refresh();
						addTask(task);
					} catch(Exception ex) {
						LOG.error(ex);
					}
				}
			}
		});
	}

	public void update() throws EucalyptusCloudException {
		if(channel != null) {
			refresh();
		} else {
			connect();
		}
	}
	
	public void stop() throws EucalyptusCloudException {
		if(channel != null) {
			channel.disconnect();
		}
		pool.shutdownNow();
	}
}

