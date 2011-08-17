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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import com.eucalyptus.address.AddressingConfiguration;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.HttpServerBootstrapper;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.SystemConfigurationEvent;
import com.eucalyptus.images.ImageConfiguration;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.images.NoSuchImageException;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.network.NetworkGroup;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Composites;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.Tx;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.client.CloudInfoWeb;
import edu.ucsb.eucalyptus.admin.client.GroupInfoWeb;
import edu.ucsb.eucalyptus.admin.client.ImageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.SystemConfigWeb;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

public class EucalyptusManagement {

	private static Logger LOG = Logger.getLogger( EucalyptusManagement.class );
//grze: see Groups.{ALL,DEFAULT}
//	private static final String GROUP_ALL = "all";
//	private static final String GROUP_DEFAULT = "default";
	
	public static String getError( String message )
	{
		return "<html><title>HTTP/1.0 403 Forbidden</title><body><div align=\"center\"><p><h1>403: Forbidden</h1></p><p><img src=\"themes/active/logo.png\" /></p><p><h3 style=\"font-color: red;\">" + message + "</h3></p></div></body></html>";
	}

	/* TODO: for now 'pattern' is ignored and all users are returned */
	public static List <UserInfoWeb> getWebUsers (String pattern) throws SerializableException
	{
	  /*
	  final List<UserInfoWeb> webUsersList = Lists.newArrayList();
	  for( User u : Users.listAllUsers( ) ) {
      try {
        UserInfo userInfo = (( WrappedUser ) u).getUserInfo( );
        webUsersList.add( Composites.composeNew( UserInfoWeb.class, userInfo, u ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
	  }
		return webUsersList;
		*/
	  return null;
	}

	/* TODO: for now 'pattern' is ignored and all images are returned */
	public static List <ImageInfoWeb> getWebImages (String pattern) throws SerializableException {
		List<ImageInfoWeb> ret = Lists.newArrayList( );
	  for( ImageInfo i : Images.listAllImages( ) ) {
        ret.add( Composites.update( i, new ImageInfoWeb( ) ) );
	  }
    return ret;
	}

	public static UserInfoWeb getWebUser( String userName, String accountName ) throws SerializableException {
	  try {
	    Account account = Accounts.lookupAccountByName( accountName );
	    User user = account.lookupUserByName( userName );
	    return Webifier.toWeb( user );
	  } catch ( Exception e ) {
	    throw new SerializableException( "Can not find user " + userName + " in account " + accountName );
	  }
	}

  public static UserInfoWeb getWebUserByEmail( String emailAddress ) throws SerializableException {
    /*
    UserInfo s = new UserInfo( );
    s.setEmail( emailAddress );
    return EucalyptusManagement.getWebUserByExample( s );
    */
    return null;
  }

  public static UserInfoWeb getWebUserByCode( String confCode ) throws SerializableException {
    /*
    UserInfo s = new UserInfo( );
    s.setConfirmationCode( confCode );
    return EucalyptusManagement.getWebUserByExample( s );
    */
    return null;
  }

  /*
  private static UserInfoWeb getWebUserByExample( UserInfo ex ) throws SerializableException {
    try {
      UserInfo userInfo = UserInfoStore.getUserInfo( ex );
      User user = Users.lookupUser( userInfo.getUserName( ) );
      UserInfoWeb webUser = Composites.composeNew( UserInfoWeb.class, userInfo, user );
      return webUser;
    } catch ( NoSuchUserException e ) {
      throw EucalyptusManagement.makeFault( "User does not exist" );
    }
  }
  */

	public static synchronized void addWebUser( UserInfoWeb webUser ) throws SerializableException
	{
	  /*
	  User user = null;
	  try {
      user = Users.lookupUser( webUser.getUserName( ) );
      throw EucalyptusManagement.makeFault("User already exists" );
    } catch ( NoSuchUserException e ) {
      try {
        user = Users.addUser( webUser.getUserName( ), webUser.isAdministrator( ), webUser.isEnabled( ) );
        try {
          UserInfo userInfo = Composites.updateNew( webUser, UserInfo.class );
          try {
            NetworkGroupUtil.createUserNetworkRulesGroup( userInfo.getUserName( ), NetworkRulesGroup.NETWORK_DEFAULT_NAME, "default group" );
          } catch ( EucalyptusCloudException e1 ) {
            LOG.debug( e1, e1 );
          }
          UserInfoStore.addUserInfo( userInfo );
        } catch ( Exception e1 ) {
          LOG.error( e1, e1 );
          throw EucalyptusManagement.makeFault("Error adding user: " + e1.getMessage( ) );
        }
      } catch ( UserExistsException e1 ) {
        LOG.error( e1, e1 );
        throw EucalyptusManagement.makeFault("User already exists" );
      } catch ( UnsupportedOperationException e1 ) {
        LOG.error( e1, e1 );
        throw EucalyptusManagement.makeFault("Error adding user: " + e1.getMessage( ) );
      }
    }	
    */  
	}

	private static SerializableException makeFault(String message)
	{
		SerializableException e = new SerializableException( message );
		LOG.error(e);
		return e;
	}

	public static void deleteWebUser( UserInfoWeb webUser ) throws SerializableException
	{
		String userName = webUser.getUserName();
		deleteUser( userName );
	}

	public static void deleteUser( String userName ) throws SerializableException
	{
	  /*
	  try {
      Users.deleteUser( userName );
      UserInfoStore.deleteUserInfo( userName );
    } catch ( NoSuchUserException e1 ) {
      LOG.debug( e1, e1 );
      throw EucalyptusManagement.makeFault( "Unable to delete user" );
    } catch ( UnsupportedOperationException e1 ) {
      LOG.debug( e1, e1 );
      throw EucalyptusManagement.makeFault("Error while deleting user: " + e1.getMessage( ) );      
    }
    */
	}

	public static void commitWebUser( final UserInfoWeb webUser ) throws SerializableException
	{
	  try {
	    Webifier.fromWeb( webUser );
	  } catch (Exception e) {
	    LOG.error( e, e );
	    throw new SerializableException("Failed to update user " + webUser.getUserName( ));
	  }
	}

	public static String getAdminEmail() throws SerializableException
	{
	  /*
		String addr = null;
		try {
      UserInfo adminUser = UserInfoStore.getUserInfo( new UserInfo("admin") );
      addr = adminUser.getEmail( );
    } catch ( NoSuchUserException e ) {
      throw EucalyptusManagement.makeFault("Administrator account not found" );
    }
    if (addr==null || addr.equals("")) {
      throw EucalyptusManagement.makeFault( "Email address is not set" );
    }
		return addr;
		*/
	  return null;
	}

	public static void deleteImage(String imageId)
	throws SerializableException
	{
	  try {
      Images.deregisterImage( imageId );
    } catch ( NoSuchImageException ex ) {
      LOG.error( ex , ex );
      throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
    }
	}
	public static void disableImage(String imageId)
	throws SerializableException
	{
    try {
      Images.deregisterImage( imageId );
    } catch ( NoSuchImageException ex ) {
      LOG.error( ex , ex );
      throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
    }
	}
	public static void enableImage(String imageId)
	throws SerializableException
	{
    try {
      Images.enableImage( imageId );
    } catch ( NoSuchImageException ex ) {
      LOG.error( ex , ex );
      throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
    }
	}
	
  @TypeMapper
  public enum SystemConfigToWebMapper implements Function<SystemConfiguration, SystemConfigWeb> {
    INSTANCE;
    
    @Override
    public SystemConfigWeb apply( final SystemConfiguration input ) {
      return new SystemConfigWeb( ) {
        {
          setDefaultKernelId( ImageConfiguration.getInstance().getDefaultRamdiskId( ) );//TODO:GRZE: facade for images config to remove direct reference to impl. class for config
          setDefaultRamdiskId( ImageConfiguration.getInstance().getDefaultRamdiskId( ) );
          setMaxUserPublicAddresses( AddressingConfiguration.getInstance( ).getMaxUserPublicAddresses( ) );//TODO:GRZE: facade for address config to remove direct reference to impl. class for config
          setDoDynamicPublicAddresses( AddressingConfiguration.getInstance( ).getDoDynamicPublicAddresses( ) );
          setSystemReservedPublicAddresses( AddressingConfiguration.getInstance( ).getSystemReservedPublicAddresses( ) );
          setDnsDomain( input.getDnsDomain( ) );
          setNameserver( input.getNameserver( ) );
          setNameserverAddress( input.getNameserverAddress( ) );
          setCloudHost( Internets.localHostInetAddress( ).getCanonicalHostName( ) );
        }
      };
    }
    
  }
	
	public static SystemConfigWeb getSystemConfig() throws SerializableException
	{
		SystemConfiguration sysConf = SystemConfiguration.getSystemConfiguration();
		LOG.debug( "Sending cloud host: " + Internets.localHostInetAddress( ) );
		return TypeMappers.lookup( SystemConfiguration.class, SystemConfigWeb.class ).apply( sysConf );
	}

	public static void setSystemConfig( final SystemConfigWeb systemConfig )
	{
    try {
      Transactions.one( AddressingConfiguration.getInstance( ), new Callback<AddressingConfiguration>( ) {
        
        @Override
        public void fire( AddressingConfiguration t ) {
          t.setMaxUserPublicAddresses( systemConfig.getMaxUserPublicAddresses() );
          t.setDoDynamicPublicAddresses( systemConfig.isDoDynamicPublicAddresses() );
          t.setSystemReservedPublicAddresses( systemConfig.getSystemReservedPublicAddresses() );
        }
      } );
    } catch ( ExecutionException ex ) {
      LOG.error( ex , ex );
    }
    try {
      Transactions.one( ImageConfiguration.getInstance( ), new Callback<ImageConfiguration>( ) {
        
        @Override
        public void fire( ImageConfiguration t ) {
          t.setDefaultKernelId( systemConfig.getDefaultKernelId() );
          t.setDefaultRamdiskId( systemConfig.getDefaultRamdiskId() );
        }
      } );
    } catch ( ExecutionException ex ) {
      LOG.error( ex , ex );
    }
	  
		EntityWrapper<SystemConfiguration> db = EntityWrapper.get( SystemConfiguration.class );
		SystemConfiguration sysConf = null;
		try
		{
			sysConf = db.getUnique( new SystemConfiguration() );
			sysConf.setDnsDomain(systemConfig.getDnsDomain());
			sysConf.setNameserver(systemConfig.getNameserver());
			sysConf.setNameserverAddress(systemConfig.getNameserverAddress());
			db.commit();
			DNSProperties.update();
		}
		catch ( EucalyptusCloudException e )
		{
			sysConf = new SystemConfiguration(
					systemConfig.getDnsDomain(),
					systemConfig.getNameserver(),
					systemConfig.getNameserverAddress());
			db.add(sysConf);
			db.commit();
			DNSProperties.update();
		}
    try {
      ListenerRegistry.getInstance( ).fireEvent( new SystemConfigurationEvent( sysConf ) );
    } catch ( EventFailedException e ) {
      LOG.debug( e, e );
    }
	}

	private static String getExternalIpAddress ()
	{
		String ipAddr = null;
		HttpClient httpClient = new HttpClient();
		//support for http proxy
		if(HttpServerBootstrapper.httpProxyHost != null && (HttpServerBootstrapper.httpProxyHost.length() > 0)) {
			String proxyHost = HttpServerBootstrapper.httpProxyHost;
			if(HttpServerBootstrapper.httpProxyPort != null &&  (HttpServerBootstrapper.httpProxyPort.length() > 0)) {
				int proxyPort = Integer.parseInt(HttpServerBootstrapper.httpProxyPort);
				httpClient.getHostConfiguration().setProxy(proxyHost, proxyPort);
			} else {
				httpClient.getHostConfiguration().setProxyHost(new ProxyHost(proxyHost));
			}
		}
		// Use Rightscale's "whoami" service
		GetMethod method = new GetMethod("https://my.rightscale.com/whoami?api_version=1.0&cloud=0");
		Integer timeoutMs = new Integer(3 * 1000); // TODO: is this working?
		method.getParams().setSoTimeout(timeoutMs);

		try {
			httpClient.executeMethod(method);
			String str = "";
			InputStream in = method.getResponseBodyAsStream();
			byte[] readBytes = new byte[1024];
			int bytesRead = -1;
			while((bytesRead = in.read(readBytes)) > 0) {
				str += new String(readBytes, 0, bytesRead);
			}
			Matcher matcher = Pattern.compile(".*your ip is (.*)").matcher(str);
			if (matcher.find()) {
				ipAddr = matcher.group(1);
			}

		} catch (MalformedURLException e) {
			LOG.warn ("Malformed URL exception: " + e.getMessage());
			e.printStackTrace();

		} catch (IOException e) {
			LOG.warn ("I/O exception: " + e.getMessage());
			e.printStackTrace();

		} finally {
			method.releaseConnection();
		}

		return ipAddr;
	}

	public static CloudInfoWeb getCloudInfo (boolean setExternalHostPort) throws SerializableException
	{
		String cloudRegisterId = null;
	    cloudRegisterId = SystemConfiguration.getSystemConfiguration().getRegistrationId();
		CloudInfoWeb cloudInfo = new CloudInfoWeb();
		cloudInfo.setInternalHostPort (Internets.localHostInetAddress( ).getHostAddress( ) + ":8443");
		if (setExternalHostPort) {
			String ipAddr = getExternalIpAddress();
			if (ipAddr!=null) {
				cloudInfo.setExternalHostPort ( ipAddr + ":8443");
			}
		}
		cloudInfo.setServicePath ("/register"); // TODO: what is the actual cloud registration service?
		cloudInfo.setCloudId ( cloudRegisterId ); // TODO: what is the actual cloud registration ID?
		return cloudInfo;
	}

	private static List<String> getGroupZones(Group group) {
	  /*
		List<String> zones = new ArrayList<String>();
		for (Authorization auth : group.getAuthorizations()) {
			if (auth instanceof AvailabilityZonePermission) {
				zones.add(auth.getValue());
			}
		}
		return zones;
		*/
	  return null;
	}

	public static List<GroupInfoWeb> getAllGroups() {
	  /*
		List<GroupInfoWeb> result = new ArrayList<GroupInfoWeb>();
		List<Group> groups = Groups.listAllGroups();
		if (groups != null) {
			for (Group group : Groups.listAllGroups()) {
				GroupInfoWeb gi = new GroupInfoWeb();
				gi.name = group.getName();
				gi.zones = getGroupZones(group);
				result.add(gi);
			}
		}
		return result;
		*/
	  return null;
	}
	
	public static GroupInfoWeb getGroup(String name) {
	  /*
		try {
			Group group = Groups.lookupGroup(name);
			GroupInfoWeb gi = new GroupInfoWeb();
			gi.name = group.getName();
			// TODO: fill in the zone name here based on permission
			gi.zones = getGroupZones(group);
			return gi;
		} catch (NoSuchGroupException nge) {
		}
		return null;
		*/
	  return null;
	}
	
	public static List<UserInfoWeb> getGroupMembers(String groupName) {
	  /*
		final List<UserInfoWeb> uis = new ArrayList<UserInfoWeb>();
		Group group = null;
		try {
			group = Groups.lookupGroup(groupName);
		} catch (NoSuchGroupException nge) {
			LOG.debug(nge, nge);
			return uis;
		}
		Enumeration<? extends Principal> users = group.members();
		while (users.hasMoreElements()) {
			User u = (User) users.nextElement();
			try {
				UserInfo userInfo = UserInfoStore.getUserInfo(new UserInfo(u.getName()));
				uis.add(Composites.composeNew(UserInfoWeb.class, userInfo, u));
			} catch ( Exception e ) {
				LOG.debug( e, e );
			}
		}
		return uis;
		*/
	  return null;
	}
	
	public static List<String> getUserGroups(String userName) throws Exception {
	  /*
		final List<String> groupNames = new ArrayList<String>();
		List<Group> groups = Groups.lookupUserGroups(Users.lookupUser(userName));
		for (Group group : groups) {
			groupNames.add(group.getName());
		}
		return groupNames;
		*/
	  return null;
	}
	
	public static void addGroup(GroupInfoWeb gi) throws Exception {
	  /*
	  Groups.checkNotRestricted( gi.getName( ) );
		Group group = Groups.addGroup(gi.name);
		for (String zone : gi.zones) {
			group.addAuthorization(new AvailabilityZonePermission(zone));
		}
		*/
	}
	
	public static void updateGroup(GroupInfoWeb gi) throws Exception {
	  /*
	  Groups.checkNotRestricted( gi.getName( ) );
		try {
			Group group = Groups.lookupGroup(gi.name);
			Set<String> oldZoneSet = new HashSet<String>(getGroupZones(group));
			Set<String> newZoneSet = new HashSet<String>(gi.zones);
			Set<String> toRemove = Sets.difference(oldZoneSet, newZoneSet);
			Set<String> toAdd = Sets.difference(newZoneSet, oldZoneSet);
			for (String zone : toRemove) {
				group.removeAuthorization(new AvailabilityZonePermission(zone));
			}
			for (String zone : toAdd) {
				group.addAuthorization(new AvailabilityZonePermission(zone));
			}
		} catch (NoSuchGroupException nsge) {
			throw new Exception("Can not find the group");
		}
		*/
	}
	
	public static void deleteGroup(String groupName) throws Exception {
	  /*
    Groups.checkNotRestricted( groupName );
		try {
			Groups.deleteGroup(groupName);
		} catch (NoSuchGroupException nsge) {
			throw new Exception("No such group");
		}
		*/
	}
	
	public static void addUserToGroup(String userName, String groupName) throws Exception {
	  /*
    Groups.checkNotRestricted( groupName );
    if (Groups.NAME_ALL.equalsIgnoreCase(groupName)) {
      throw new Exception("Group 'all' cannot be added to");
    }
		User user = null;
		try {
			user = Users.lookupUser(userName);
		} catch (NoSuchUserException nsue) {
			throw new Exception("Can't find user " + userName);
		}
		Group group = null;
		try {
			group = Groups.lookupGroup(groupName);
		} catch (NoSuchGroupException nsge) {
			throw new Exception("Can't find group " + groupName);
		}
		if (!group.addMember(user)) {
			throw new Exception("Failed to add user " + userName + " to " + groupName);
		}
		*/
	}
	
	public static void removeUserFromGroup(String userName, String groupName) throws Exception {
	  /*
		if (Groups.NAME_ALL.equalsIgnoreCase(groupName)) {
			throw new Exception("Group 'all' cannot be removed from");
		}
		User user = null;
		try {
			user = Users.lookupUser(userName);
		} catch (NoSuchUserException nsue) {
			throw new Exception("Can't find user " + userName);
		}
		Group group = null;
		try {
			group = Groups.lookupGroup(groupName);
		} catch (NoSuchGroupException nsge) {
			throw new Exception("Can't find group " + groupName);
		}
		if (!group.removeMember(user)) {
			throw new Exception("Failed to remove user " + userName + " from " + groupName);
		}
		*/
	}
	
	public static void updateUserGroups(String userName, List<String> updateGroups) throws Exception {
	  /*
		User user = null;
		try {
			user = Users.lookupUser(userName);
		} catch (NoSuchUserException nsue) {
			throw new Exception("Can't find user " + userName);
		}
		Set<String> updateGroupSet = new HashSet<String>();
		updateGroupSet.addAll(updateGroups);
		for (Group group : Groups.listAllGroups()) {
		  if( Groups.NAME_ALL.equalsIgnoreCase( group.getName( ) ) ) {
		    continue;
		  }
			if (updateGroupSet.contains(group.getName())) {
				group.addMember(user);
			} else {
				group.removeMember(user);
			}
		}
		*/
	}
}
