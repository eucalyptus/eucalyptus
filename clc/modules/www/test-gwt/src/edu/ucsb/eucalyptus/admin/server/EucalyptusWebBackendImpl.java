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
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.SerializableException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.ucsb.eucalyptus.admin.client.CloudInfoWeb;
import edu.ucsb.eucalyptus.admin.client.ClusterInfoWeb;
import edu.ucsb.eucalyptus.admin.client.DownloadsWeb;
import edu.ucsb.eucalyptus.admin.client.EucalyptusWebBackend;
import edu.ucsb.eucalyptus.admin.client.GroupInfoWeb;
import edu.ucsb.eucalyptus.admin.client.StorageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.SystemConfigWeb;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;
import edu.ucsb.eucalyptus.admin.client.VmTypeWeb;
import edu.ucsb.eucalyptus.admin.client.WalrusInfoWeb;

/**
 * Created by IntelliJ IDEA.
 * User: dmitriizagorodnov
 * Date: May 3, 2008
 * Time: 2:57:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class EucalyptusWebBackendImpl extends RemoteServiceServlet implements EucalyptusWebBackend {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	static UserInfoWeb adminUser = new UserInfoWeb();
	static Map<String, UserInfoWeb> users = new HashMap<String, UserInfoWeb>();
	static Map<String, GroupInfoWeb> groups = new HashMap<String, GroupInfoWeb>();
	static Map<String, Set<String>> groupUsers = new HashMap<String, Set<String>>();
	
	static {
		GroupInfoWeb group;
		group = new GroupInfoWeb();
		group.name = "research";
		group.zones = new ArrayList<String>();
		group.zones.add("cluster1");
		addMockGroup(group);
		group = new GroupInfoWeb();
		group.name = "product";
		group.zones = new ArrayList<String>();
		group.zones.add("cluster1");
		group.zones.add("cluster2");
		addMockGroup(group);
		
		List<String> groupNames = new ArrayList<String>();
		
		adminUser.setUserName("admin");
		adminUser.setRealName("Jack Bauer");
		adminUser.setPassword("12345");
		adminUser.setAdministrator(true); // so we see all tabs
		adminUser.setAffiliation("IT");
		adminUser.setApproved(true);
		adminUser.setConfirmed(true);
		adminUser.setEnabled(true);
		adminUser.setEmail("admin@foobar.com"); // so we skip first-time login screen
		groupNames.clear();
		groupNames.add("research");
		groupNames.add("product");
		addMockUser(adminUser, groupNames);
		
		UserInfoWeb newUser = new UserInfoWeb();
		newUser.setUserName("tomh");
		newUser.setRealName("Tom Hanks");
		newUser.setPassword("12345");
		newUser.setEmail("thanks@foobar.com");
		newUser.setAdministrator(false);
		newUser.setAffiliation("lab");
		newUser.setApproved(true);
		newUser.setConfirmed(true);
		newUser.setEnabled(true);
		groupNames.clear();
		groupNames.add("research");
		addMockUser(newUser, groupNames);
		
		newUser = new UserInfoWeb();
		newUser.setUserName("hford");
		newUser.setRealName("Harrison Ford");
		newUser.setPassword("12345");
		newUser.setEmail("hford@foobar.com");
		newUser.setAdministrator(false);
		newUser.setAffiliation("incubation");
		newUser.setApproved(true);
		newUser.setConfirmed(true);
		newUser.setEnabled(true);
		groupNames.clear();
		groupNames.add("research");
		addMockUser(newUser, groupNames);
		
		newUser = new UserInfoWeb();
		newUser.setUserName("cfisher");
		newUser.setRealName("Carrie Fisher");
		newUser.setPassword("12345");
		newUser.setEmail("cfisher@foobar.com");
		newUser.setAdministrator(false);
		newUser.setAffiliation("sales");
		newUser.setApproved(true);
		newUser.setConfirmed(true);
		newUser.setEnabled(true);
		groupNames.clear();
		groupNames.add("product");
		addMockUser(newUser, groupNames);
		
		newUser = new UserInfoWeb();
		newUser.setUserName("mhamill");
		newUser.setRealName("Mark Hamill");
		newUser.setPassword("12345");
		newUser.setEmail("mhamill@foobar.com");
		newUser.setAdministrator(false);
		newUser.setAffiliation("support");
		newUser.setApproved(true);
		newUser.setConfirmed(true);
		newUser.setEnabled(true);
		groupNames.clear();
		groupNames.add("product");
		addMockUser(newUser, groupNames);
	}
	
	private static void addMockGroup(GroupInfoWeb group) {
		groups.put(group.name, group);
		groupUsers.put(group.name, new HashSet<String>());
	}
	
	private static void updateMockGroup(GroupInfoWeb group) {
		groups.put(group.name, group);
	}
	
	private static void addMockUser(UserInfoWeb user, List<String> groupNames) {
		users.put(user.getUserName(), user);
		for (String groupName : groupNames) {
			groupUsers.get(groupName).add(user.getUserName());
		}
	}
	
	private static void updateMockUser(UserInfoWeb user, List<String> groupNames) {
		users.put(user.getUserName(), user);
		Set<String> groupSet = new HashSet<String>(groupNames);
		for (String groupName : groups.keySet()) {
			if (groupSet.contains(groupName)) {
				groupUsers.get(groupName).add(user.getUserName());
			} else {
				groupUsers.get(groupName).remove(user.getUserName());
			}
		}
	}
	
	private static void deleteMockGroups(List<String> groupNames) {
		for (String groupName : groupNames) {
			groups.remove(groupName);
			groupUsers.remove(groupName);
		}
	}
	
	private static void deleteMockUsers(List<String> userNames) {
		for (String userName : userNames) {
			users.remove(userName);
			for (Set<String> userSet : groupUsers.values()) {
				userSet.remove(userName);
			}
		}
	}
	
	private static List<String> getMockZones() {
		List<String> zones = new ArrayList<String>();
		zones.add("cluster1");
		zones.add("cluster2");
		zones.add("cluster3");
		return zones;
	}
	
	public String addUserRecord ( UserInfoWeb user )
	throws SerializableException
	{
		return addUserRecord(null, user);
	}

	public String addUserRecord(String sessionId, UserInfoWeb user)
	throws SerializableException
	{
		users.put(user.getUserName(), user);
		return "OK";
	}

	public String recoverPassword ( UserInfoWeb web_user )
	throws SerializableException
	{
		return "OK";
	}

	public String getNewSessionID (String userId, String md5Password)
	throws SerializableException
	{
		return "session-id";
	}

	public String performAction (String sessionId, String action, String param)
	throws SerializableException
	{
		return "OK";
	}

	public void logoutSession(String sessionId)
	throws SerializableException
	{

	}

	public List<UserInfoWeb> getUserRecord (String sessionId, String userId)
	throws SerializableException
	{
		if (userId==null) {
			List<UserInfoWeb> l = new ArrayList<UserInfoWeb>();
			l.add(adminUser);
			return l;
		} else {
			return new ArrayList<UserInfoWeb>(users.values());
		}
	}

	public static UserInfoWeb getUserRecord (String sessionId) // a *static* getUserRecord, for ImageStoreService
	throws SerializableException
	{
		return adminUser;
	}

	public List getImageInfo (String sessionId, String userId)
	throws SerializableException
	{
		return new ArrayList();
	}

	/* from here on down, all requests require users to be enabled, approved, and confirmed */

	public String getNewCert(String sessionId)
	throws SerializableException
	{
		return "cert";
	}

	public HashMap getProperties()
	throws SerializableException
	{
		HashMap h = new HashMap();
		h.put("ready", true);
		h.put("cloud-name", "Back-end stub");
		h.put("version", "1.6.2");
		h.put("signup-greeting", "<signup greeting>");
		h.put("certificate-download-text", "<certificate-download-text>");
		h.put("rest-credentials-text", "<rest-credentials-text>");
		h.put("user-account-text", "user-account-text");
		h.put("admin-first-time-config-text", "admin-first-time-config-text");
		h.put("admin-email-change-text", "admin-email-change-text");
		h.put("admin-cloud-ip-setup-text", "admin-cloud-ip-setup-text");
		return h;
	}

	public String changePassword (String sessionId, String oldPassword, String newPassword )
	throws SerializableException
	{
		return "OK";
	}

	public String updateUserRecord (String sessionId, UserInfoWeb newRecord )
	throws SerializableException
	{
		return "OK";
	}

	public List<ClusterInfoWeb> getClusterList(String sessionId) throws SerializableException
	{
		ArrayList<ClusterInfoWeb> a = new ArrayList<ClusterInfoWeb>();
		ClusterInfoWeb c = new ClusterInfoWeb("CLUSTER", "hostname", 43210, 44, 55);
		a.add(c);
		return a;
	}

	public void setClusterList(String sessionId, List<ClusterInfoWeb> clusterList ) throws SerializableException
	{

	}

	public List<StorageInfoWeb> getStorageList(String sessionId) throws SerializableException
	{
		ArrayList<StorageInfoWeb> a = new ArrayList<StorageInfoWeb>();
		StorageInfoWeb s = new StorageInfoWeb("CLUSTER", "hostname", 54321, new ArrayList<String>());
		a.add(s);
		return a;
	}

	public void setStorageList(String sessionId, List<StorageInfoWeb> storageList ) throws SerializableException
	{

	}

	public List<WalrusInfoWeb> getWalrusList(String sessionId) throws SerializableException
	{
		return new ArrayList<WalrusInfoWeb>();
	}

	public void setWalrusList(String sessionId, List<WalrusInfoWeb> walrusList ) throws SerializableException
	{

	}

	public SystemConfigWeb getSystemConfig( final String sessionId ) throws SerializableException
	{
		SystemConfigWeb c = new SystemConfigWeb();
		c.setDoDynamicPublicAddresses(false);
		return c;
	}

	public void setSystemConfig( final String sessionId, final SystemConfigWeb systemConfig ) throws SerializableException
	{

	}

	public List<VmTypeWeb> getVmTypes( final String sessionId ) throws SerializableException
	{
		return new ArrayList<VmTypeWeb>();
	}

	public void setVmTypes( final String sessionId, final List<VmTypeWeb> vmTypes ) throws SerializableException
	{

	}

	public CloudInfoWeb getCloudInfo(final String sessionId, final boolean setExternalHostPort) throws SerializableException
	{
		return new CloudInfoWeb();
	}

	private static List<DownloadsWeb> getDownloadsFromUrl(final String downloadsUrl) {
		return new ArrayList<DownloadsWeb>();
	}

	public List<DownloadsWeb> getDownloads(final String sessionId, final String downloadsUrl) throws SerializableException {
		return new ArrayList<DownloadsWeb>();
	}
	
	public List<GroupInfoWeb> getGroups(final String sessionId, final String name) throws Exception {
		if ("".equals(name) || name == null) {
			return new ArrayList<GroupInfoWeb>(groups.values());
		}
		List<GroupInfoWeb> result = new ArrayList<GroupInfoWeb>();
		result.add(groups.get(name));
		return result;
	}
	
	public List<UserInfoWeb> getUsersByGroups(final String sessionId, final List<String> groupNames) throws Exception {
		List<UserInfoWeb> result = new ArrayList<UserInfoWeb>();
		Set<UserInfoWeb> resultSet = new HashSet<UserInfoWeb>();
		for (String group : groupNames) {
			Set<String> usersForGroup = groupUsers.get(group);
			if (usersForGroup != null) {
				for (String userName : usersForGroup.toArray(new String[0])) {
					resultSet.add(users.get(userName));
				}
			}
		}
		result.addAll(resultSet);
		return result;
	}
	
	public void addGroup(final String sessionId, final GroupInfoWeb group) throws Exception {
		addMockGroup(group);
	}
	
	public void updateGroup(final String sessionId, final GroupInfoWeb group) throws Exception {
		updateMockGroup(group);
	}
	
	public List<String> getGroupsByUser(final String sessionId, final String userName) throws Exception {
		List<String> groupNames = new ArrayList<String>();
		for (String groupName : groupUsers.keySet()) {
			if (groupUsers.get(groupName).contains(userName)) {
				groupNames.add(groupName);
			}
		}
		return groupNames;
	}
	
	public void deleteGroups(final String sessionId, final List<String> groupNames) throws Exception {
		deleteMockGroups(groupNames);
	}
	
	public void addUser(final String sessionId, final UserInfoWeb user, final List<String> groupNames) throws Exception {
		addMockUser(user, groupNames);
	}
	
	public void updateUser(final String sessionId, final UserInfoWeb user, final List<String> groupNames) throws Exception {
		updateMockUser(user, groupNames);
	}
	
	public void deleteUsers(final String sessionId, final List<String> userNames) throws Exception {
		deleteMockUsers(userNames);
	}
	
	public void addUsersToGroups(final String sessionId, final List<String> userNames, final List<String> groupNames) throws Exception {
		for (String groupName : groupNames) {
			groupUsers.get(groupName).addAll(userNames);
		}
	}
	
	public void removeUsersFromGroups(final String sessionId, final List<String> userNames, final List<String> groupNames) throws Exception {
		for (String groupName : groupNames) {
			groupUsers.get(groupName).removeAll(userNames);
		}
	}
	
	public void enableUsers(final String sessionId, final List<String> userNames) throws Exception {
		for (String userName : userNames) {
			users.get(userName).setEnabled(true);
		}
	}
	
	public void disableUsers(final String sessionId, final List<String> userNames) throws Exception {
		for (String userName : userNames) {
			users.get(userName).setEnabled(false);
		}
	}
	
	public void approveUsers(final String sessionId, final List<String> userNames) throws Exception {
		for (String userName : userNames) {
			users.get(userName).setApproved(true);
		}
	}
	
	public List<String> getZones(final String sessionId) throws Exception {
		return getMockZones();
	}
}
