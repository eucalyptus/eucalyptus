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

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: dmitriizagorodnov
 * Date: May 3, 2008
 * Time: 2:57:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface EucalyptusWebBackendAsync {

	void getNewSessionID(String userId, String bCryptedPassword, final AsyncCallback<String> async)
	;

	void addUserRecord(UserInfoWeb user, final AsyncCallback<String> async)
	;

	void addUserRecord(String sessionId, UserInfoWeb user, final AsyncCallback<String> async)
	;

	void recoverPassword(UserInfoWeb user, final AsyncCallback<String> async)
	;

	void getUserRecord(String sessionId, String userId, final AsyncCallback<List<UserInfoWeb>> async)
	;

	void getImageInfo(String sessionId, String userId, final AsyncCallback<List<ImageInfoWeb>> async)
	;

	void performAction(String sessionId, String action, String param, final AsyncCallback<String> async)
	;

	void logoutSession(String sessionId, final AsyncCallback async)
	;

	void getNewCert(String sessionId, final AsyncCallback<String> async)
	;

	void getProperties(final AsyncCallback<HashMap<String, String>> async)
	;

	void changePassword(String sessionId, String oldPassword, String newPassword, final AsyncCallback<String> async)
	;

	void updateUserRecord(String sessionId, UserInfoWeb newRecord, final AsyncCallback<String> async)
	;

	void getClusterList(String sessionId, final AsyncCallback<List<ClusterInfoWeb>> async)
	;

	void setClusterList(String sessionId, List<ClusterInfoWeb> clusterList, final AsyncCallback async)
	;

	void getStorageList(String sessionId, final AsyncCallback<List<StorageInfoWeb>> async)
	;

	void setStorageList(String sessionId, List<StorageInfoWeb> storageList, final AsyncCallback async)
	;

	void getWalrusList(String sessionId, final AsyncCallback<List<WalrusInfoWeb>> async)
	;

	void setWalrusList(String sessionId, List<WalrusInfoWeb> walrusList, final AsyncCallback async)
	;

	void getSystemConfig(String sessionId, final AsyncCallback<SystemConfigWeb> async)
	;

	void setSystemConfig(String sessionId, SystemConfigWeb systemConfig, final AsyncCallback async)
	;

	void getVmTypes(String sessionId, final AsyncCallback<List<VmTypeWeb>> async)
	;

	void setVmTypes(String sessionId, List<VmTypeWeb> vmTypes, final AsyncCallback async)
	;

	void getCloudInfo(String sessionId, boolean setExternalHostport, final AsyncCallback<CloudInfoWeb> async)
	;

	void getDownloads(String sessionId, String downloadsUrl, final AsyncCallback<List<DownloadsWeb>> async)
	;
	
	void getFileContentsByPath(String sessionId, String path, final AsyncCallback<String> async)
	;
}
