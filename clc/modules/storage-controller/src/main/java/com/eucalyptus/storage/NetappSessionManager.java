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
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.storage;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import netapp.manage.NaAPIFailedException;
import netapp.manage.NaAuthenticationException;
import netapp.manage.NaElement;
import netapp.manage.NaProtocolException;
import netapp.manage.NaServer;

import org.apache.log4j.Logger;

import com.eucalyptus.util.EucalyptusCloudException;

import edu.ucsb.eucalyptus.cloud.entities.SANInfo;

public class NetappSessionManager implements SessionManager {
	private static Logger LOG = Logger.getLogger(NetappSessionManager.class);
	private final ExecutorService pool;
	private NaServer connection;

	public NetappSessionManager() {
		pool = Executors.newFixedThreadPool(1);
	}

	public void checkConnection() throws EucalyptusCloudException {
	}

	public void connect() throws EucalyptusCloudException {
		try {
			SANInfo sanInfo = SANInfo.getStorageInfo();
			connection = new NaServer(sanInfo.getSanHost(), NetappProvider.API_MAJOR_VERSION, NetappProvider.API_MINOR_VERSION);
			connection.setStyle(NaServer.STYLE_LOGIN_PASSWORD);
			connection.setKeepAliveEnabled(true);
			connection.setAdminUser(sanInfo.getSanUser(), sanInfo.getSanPassword());
			//test
			NaElement request = new NaElement("system-get-version");
			NaElement reply;
			try {
				reply = connection.invokeElem(request);
				LOG.info("Version: " + reply.getChildContent("version"));
			} catch (NaAuthenticationException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			} catch (NaAPIFailedException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			} catch (NaProtocolException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			} catch (IOException e) {
				LOG.error(e);
				throw new EucalyptusCloudException(e);
			}			
		} catch (UnknownHostException ex) {
			throw new EucalyptusCloudException(ex);
		}
	}

	public void refresh() throws EucalyptusCloudException {
		connection.close();
		connect();
	}

	public void update() throws EucalyptusCloudException {
		if(connection != null) {
			refresh();
		} else {
			connect();
		}
	}

	@Override
	public void addTask(final AbstractSANTask task) throws InterruptedException {
		final NetappSANTask sanTask = (NetappSANTask) task;
		pool.execute(new Runnable() {		
			@Override
			public void run() {
				NaElement request = sanTask.getCommand();
				try {
					NaElement reply = connection.invokeElem(request);
					synchronized (task) {
						sanTask.setValue(reply);
						sanTask.notifyAll();
					}
				} catch (NaAuthenticationException e) {
					LOG.error(e);
					sanTask.setErrorMessage(e.getMessage());
					synchronized (task) {
						sanTask.notifyAll();
					}
				} catch (NaAPIFailedException e) {
					LOG.error(e);
					sanTask.setErrorMessage(e.getMessage());
					synchronized (task) {
						sanTask.notifyAll();
					}
				} catch (NaProtocolException e) {
					LOG.error(e);
					sanTask.setErrorMessage(e.getMessage());
					synchronized (task) {
						sanTask.notifyAll();
					}
				} catch (IOException e) {
					try {
						refresh();
						addTask(task);
					} catch(Exception ex) {
						LOG.error(ex);
						sanTask.setErrorMessage(e.getMessage());
						synchronized (task) {
							sanTask.notifyAll();
						}
					}
				}			
			}
		});
	}

	public void stop() throws EucalyptusCloudException {
		if(connection != null) {
			connection.close();
		}
		pool.shutdownNow();
	}
}

