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
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: dmitriizagorodnov
 * Date: May 3, 2008
 * Time: 2:57:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface EucalyptusWebBackend extends RemoteService {

	public String getNewSessionID( String userId, String bCryptedPassword )
	throws SerializableException;

	public String addUserRecord( UserInfoWeb user)
	throws SerializableException;

	public String addUserRecord( String sessionId, UserInfoWeb user )
	throws SerializableException;

	public String recoverPassword( UserInfoWeb user )
	throws SerializableException;

	public List<UserInfoWeb> getUserRecord( String sessionId, String userId )
	throws SerializableException;

	public List<ImageInfoWeb> getImageInfo( String sessionId, String userId )
	throws SerializableException;

	public String performAction( String sessionId, String action, String param )
	throws SerializableException;

	public void logoutSession( String sessionId )
	throws SerializableException;

	public String getNewCert( String sessionId )
	throws SerializableException;

	public HashMap<String,String> getProperties()
	throws SerializableException;

	public String changePassword( String sessionId, String oldPassword, String newPassword )
	throws SerializableException;

	public String updateUserRecord( String sessionId, UserInfoWeb newRecord )
	throws SerializableException;

	public List<ClusterInfoWeb> getClusterList( String sessionId )
	throws SerializableException;

	public void setClusterList( String sessionId, List<ClusterInfoWeb> clusterList )
	throws SerializableException;

	public List<StorageInfoWeb> getStorageList( String sessionId )
	throws SerializableException;

	public void setStorageList( String sessionId, List<StorageInfoWeb> storageList )
	throws SerializableException;

	public List<WalrusInfoWeb> getWalrusList( String sessionId )
	throws SerializableException;

	public void setWalrusList( String sessionId, List<WalrusInfoWeb> storageList )
	throws SerializableException;

	public SystemConfigWeb getSystemConfig( String sessionId ) throws SerializableException;

	public void setSystemConfig( String sessionId, SystemConfigWeb systemConfig ) throws SerializableException;

	public List<VmTypeWeb> getVmTypes( String sessionId ) throws SerializableException;
	public void setVmTypes( String sessionId, List<VmTypeWeb> vmTypes )throws SerializableException;

	public CloudInfoWeb getCloudInfo (String sessionId, boolean setExternalHostport) throws SerializableException;

	public List<DownloadsWeb> getDownloads ( String sessionId, String downloadsUrl ) throws SerializableException;

	public String getFileContentsByPath ( String sessionId, String path ) throws SerializableException;

	/**
	 * Utility/Convenience class.
	 * Use EucalyptusWebBackend.App.getInstance() to access static instance of EucalyptusWebBackendAsync
	 */
	public static class App {

		private static edu.ucsb.eucalyptus.admin.client.EucalyptusWebBackendAsync ourInstance = null;

		public static synchronized edu.ucsb.eucalyptus.admin.client.EucalyptusWebBackendAsync getInstance()
		{
			if ( ourInstance == null )
			{
				ourInstance = ( edu.ucsb.eucalyptus.admin.client.EucalyptusWebBackendAsync ) GWT.create( EucalyptusWebBackend.class );
				( ( ServiceDefTarget ) ourInstance ).setServiceEntryPoint( GWT.getModuleBaseURL() + "EucalyptusWebBackend" );
			}
			return ourInstance;
		}
	}
}
