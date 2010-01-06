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

package edu.ucsb.eucalyptus.storage;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.ClusterCredentials;
import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.StorageProperties;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import edu.ucsb.eucalyptus.cloud.entities.EquallogicUserInfo;
import edu.ucsb.eucalyptus.cloud.entities.EquallogicVolumeInfo;
import edu.ucsb.eucalyptus.ic.StorageController;

public class EquallogicManager implements LogicalStorageManager {
	private static final Pattern VOLUME_CREATE_PATTERN = Pattern.compile(".*iSCSI target name is (.*)\r");
	private static final Pattern VOLUME_DELETE_PATTERN = Pattern.compile("Volume deletion succeeded.");
	private static final Pattern USER_CREATE_PATTERN = Pattern.compile(".*Password is (.*)\r");
		
	private PSConnectionManager connectionManager;
	private static EquallogicManager singleton;
	private static Logger LOG = Logger.getLogger(EquallogicManager.class);

	public static LogicalStorageManager getInstance( ) {
		synchronized ( EquallogicManager.class ) {
			if ( singleton == null ) {
				singleton = new EquallogicManager( );
			}
		}
		return singleton;
	}

	public EquallogicManager() {
		connectionManager = new PSConnectionManager();
	}

	@Override
	public void addSnapshot(String snapshotId) throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public void checkPreconditions() throws EucalyptusCloudException {
		//connectionManager.checkConnection();
	}

	@Override
	public void cleanSnapshot(String volumeId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cleanVolume(String volumeId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void configure() {
		try {
			connectionManager.checkConnection();
		} catch (EucalyptusCloudException e) {
			LOG.error(e);
		}
	}

	@Override
	public List<String> createSnapshot(String volumeId, String snapshotId)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void createVolume(String volumeId, int size)
	throws EucalyptusCloudException {
		String iqn = connectionManager.createVolume(volumeId, size);
		if(iqn != null) {
			EquallogicVolumeInfo volumeInfo = new EquallogicVolumeInfo(volumeId, iqn, size);			
			EntityWrapper<EquallogicVolumeInfo> db = StorageController.getEntityWrapper();
			db.add(volumeInfo);
			db.commit();
		}
	}

	@Override
	public int createVolume(String volumeId, String snapshotId)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteSnapshot(String snapshotId)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteVolume(String volumeId) throws EucalyptusCloudException {
		if(connectionManager.deleteVolume(volumeId)) {
			EntityWrapper<EquallogicVolumeInfo> db = StorageController.getEntityWrapper();
			EquallogicVolumeInfo volumeInfo = db.getUnique(new EquallogicVolumeInfo(volumeId));
			if(volumeInfo != null)
				db.delete(volumeInfo);
			db.commit();
		}
	}

	@Override
	public void dupVolume(String volumeId, String dupedVolumeId)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getSnapshotSize(String snapshotId)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<String> getSnapshotValues(String snapshotId)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getStatus(List<String> volumeSet)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVolumeProperty(String volumeId)
	throws EucalyptusCloudException {
		return connectionManager.getVolumeProperty(volumeId);
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadSnapshots(List<String> snapshotSet,
			List<String> snapshotFileNames) throws EucalyptusCloudException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> prepareForTransfer(String snapshotId)
	throws EucalyptusCloudException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reload() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStorageInterface(String storageInterface) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startupChecks() {
		// TODO Auto-generated method stub

	}

	public class PSConnectionManager {
		private String host;
		private String username;
		private String password;


		public void checkConnection() throws EucalyptusCloudException {
			//for now 
			host = "192.168.7.188";
			username = "grpadmin";
			password = "zoomzoom";
			
			String show = execCommand("show");
			if(show.length() <= 0) 
				throw new EucalyptusCloudException("Connection failed.");
			addUser("eucalyptus");
		}

		public String getVolumeProperty(String volumeId) {
			EntityWrapper<EquallogicVolumeInfo> db = StorageController.getEntityWrapper();
			try {
				EquallogicVolumeInfo volumeInfo = db.getUnique(new EquallogicVolumeInfo(volumeId));
				EntityWrapper<EquallogicUserInfo> dbUser = db.recast(EquallogicUserInfo.class);
				EquallogicUserInfo userInfo = dbUser.getUnique(new EquallogicUserInfo("eucalyptus"));
				String property = host + "," + volumeInfo.getIqn() + "," + userInfo.getEncryptedPassword();
				db.commit();
				return property;
			} catch(EucalyptusCloudException ex) {
				LOG.error(ex);
				db.rollback();
				return null;
			}
		}

		public String execCommand(String command) throws EucalyptusCloudException {
			try {
				JSch jsch = new JSch();
				Session session;
				session = jsch.getSession(username, host);
				session.setConfig("StrictHostKeyChecking", "no");
				session.setPassword(password);
				session.connect();
				Channel channel = session.openChannel("shell");
				channel.setInputStream(new ByteArrayInputStream(command.getBytes()));
				ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
				channel.setOutputStream(bytesOut);
				channel.connect();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOG.error(e);
				}
				channel.disconnect();
				session.disconnect();
				return bytesOut.toString();
			} catch (JSchException e1) {
				LOG.error(e1, e1);
				throw new EucalyptusCloudException(e1);
			}
		}

		private String matchPattern(String input,
				Pattern pattern) {
			Matcher m = pattern.matcher(input);
			if(m.find()) 
				return m.group(1);
			else
				return null;			
		}

		public String createVolume(String volumeName, int size) {
			try {
				String returnValue = execCommand("stty hardwrap off\u001Avolume create " + volumeName + " " + (size * StorageProperties.KB) + "\u001A");
				return matchPattern(returnValue, VOLUME_CREATE_PATTERN);
			} catch (EucalyptusCloudException e) {
				LOG.error(e);
				return null;
			}
		}
		
		public boolean deleteVolume(String volumeName) {
			try {
				String returnValue = execCommand("stty hardwrap off\u001Avolume select " + volumeName + " offline\u001Avolume delete " + volumeName + "\u001A");
				if(matchPattern(returnValue, VOLUME_DELETE_PATTERN) != null)
					return true;
				else
					return false;
			} catch(EucalyptusCloudException e) {
				LOG.error(e);
				return false;
			}
		}
		
		public void addUser(String userName) throws EucalyptusCloudException {
			EntityWrapper<EquallogicUserInfo> db = StorageController.getEntityWrapper();
			try {
				EquallogicUserInfo userInfo = db.getUnique(new EquallogicUserInfo(userName));
				db.commit();
			} catch(EucalyptusCloudException ex) {
				db.rollback();
				try {
					String returnValue = execCommand("stty hardwrap off\u001Achapuser create " + userName + "\u001A");
					String password = matchPattern(returnValue, USER_CREATE_PATTERN);
					db = StorageController.getEntityWrapper();
					EquallogicUserInfo userInfo = new EquallogicUserInfo(userName, encryptTargetPassword(password));
					db.add(userInfo);
					db.commit();
				} catch (EucalyptusCloudException e) {
					LOG.error(e);
					throw e;
				}
			}
		}
		
		private String encryptTargetPassword(String password) throws EucalyptusCloudException {
			EntityWrapper<ClusterCredentials> credDb = Credentials.getEntityWrapper( );
			try {
				ClusterCredentials credentials = credDb.getUnique( new ClusterCredentials( StorageProperties.NAME ) );
				PublicKey ncPublicKey = X509Cert.toCertificate(credentials.getNodeCertificate()).getPublicKey();
				credDb.commit();
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey);
				return new String(Base64.encode(cipher.doFinal(password.getBytes())));	      
			} catch ( Exception e ) {
				LOG.error( "Unable to encrypt storage target password" );
				credDb.rollback( );
				throw new EucalyptusCloudException(e.getMessage(), e);
			}
		}
	}
}

