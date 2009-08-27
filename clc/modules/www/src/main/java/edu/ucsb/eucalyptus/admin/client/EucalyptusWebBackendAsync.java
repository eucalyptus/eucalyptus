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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
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
}
