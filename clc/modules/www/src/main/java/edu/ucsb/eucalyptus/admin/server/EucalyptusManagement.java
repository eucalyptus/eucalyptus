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

package edu.ucsb.eucalyptus.admin.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.NoSuchUserException;
import com.eucalyptus.auth.UserExistsException;
import com.eucalyptus.auth.GroupEntity;
import com.eucalyptus.auth.UserInfo;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Crypto;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Counters;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.NetworkRulesGroup;
import com.eucalyptus.event.EventVetoedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.SystemConfigurationEvent;
import com.eucalyptus.images.Image;
import com.eucalyptus.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.network.NetworkGroupUtil;
import com.eucalyptus.util.Composites;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.client.CloudInfoWeb;
import edu.ucsb.eucalyptus.admin.client.ImageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.SystemConfigWeb;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

public class EucalyptusManagement {

	private static Logger LOG = Logger.getLogger( EucalyptusManagement.class );

	public static String getError( String message )
	{
		return "<html><title>HTTP/1.0 403 Forbidden</title><body><div align=\"center\"><p><h1>403: Forbidden</h1></p><p><img src=\"themes/active/logo.png\" /></p><p><h3 style=\"font-color: red;\">" + message + "</h3></p></div></body></html>";
	}

	/* TODO: for now 'pattern' is ignored and all users are returned */
	public static List <UserInfoWeb> getWebUsers (String pattern) throws SerializableException
	{
    final EntityWrapper<UserInfo> dbWrapper = EntityWrapper.get( UserInfo.class );
	  final List<UserInfoWeb> webUsersList = Lists.newArrayList();
	  for( User u : Users.listAllUsers( ) ) {
      try {
        UserInfo userInfo = dbWrapper.getUnique( new UserInfo( u.getName( ) ) );
        webUsersList.add( Composites.composeNew( UserInfoWeb.class, userInfo, u ) );
      } catch ( Exception e ) {
        LOG.debug( e, e );
      }
	  }
		dbWrapper.commit();
		return webUsersList;
	}

	/* TODO: for now 'pattern' is ignored and all images are returned */
	public static List <ImageInfoWeb> getWebImages (String pattern) throws SerializableException {
		List<ImageInfoWeb> ret = Lists.newArrayList( );
	  for( Image i : Images.listAllImages( ) ) {
        ret.add( Composites.update( i, new ImageInfoWeb( ) ) );
	  }
    return ret;
	}

	public static UserInfoWeb getWebUser( String userName ) throws SerializableException {
	  return EucalyptusManagement.getWebUserByExample( new UserInfo( userName ) );
	}

  public static UserInfoWeb getWebUserByEmail( String emailAddress ) throws SerializableException {
    UserInfo s = new UserInfo( );
    s.setEmail( emailAddress );
    return EucalyptusManagement.getWebUserByExample( s );
  }

  public static UserInfoWeb getWebUserByCode( String confCode ) throws SerializableException {
    UserInfo s = new UserInfo( );
    s.setConfirmationCode( confCode );
    return EucalyptusManagement.getWebUserByExample( s );
  }
  
  private static UserInfoWeb getWebUserByExample( UserInfo ex ) throws SerializableException {
    EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>( );
    try {
      UserInfo userInfo = dbWrapper.getUnique( ex );
      User user = Users.lookupUser( userInfo.getUserName( ) );
      UserInfoWeb webUser = Composites.composeNew( UserInfoWeb.class, userInfo, user );
      dbWrapper.commit( );
      return webUser;
    } catch ( EucalyptusCloudException e ) {
      dbWrapper.rollback( );
      throw EucalyptusManagement.makeFault( "Error looking up user information: " + e.getMessage( ) );
    } catch ( NoSuchUserException e ) {
      dbWrapper.rollback( );
      throw EucalyptusManagement.makeFault( "User does not exist" );
    }
  }

	public static synchronized void addWebUser( UserInfoWeb webUser ) throws SerializableException
	{
	  User user = null;
	  try {
      user = Users.lookupUser( webUser.getUserName( ) );
      throw EucalyptusManagement.makeFault("User already exists" );
    } catch ( NoSuchUserException e ) {
      try {
        user = Users.addUser( webUser.getUserName( ), webUser.isAdministrator( ), webUser.isEnabled( ) );
        EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>("eucalyptus_general");
        try {
          UserInfo userInfo = Composites.updateNew( webUser, UserInfo.class );
          userInfo.setConfirmationCode( Crypto.generateSessionToken( webUser.getUserName() ) );
          try {
            NetworkGroupUtil.createUserNetworkRulesGroup( userInfo.getUserName( ), NetworkRulesGroup.NETWORK_DEFAULT_NAME, "default group" );
          } catch ( EucalyptusCloudException e1 ) {
            LOG.debug( e1, e1 );
          }
          dbWrapper.add( userInfo );
          dbWrapper.commit();
        } catch ( Exception e1 ) {
          dbWrapper.rollback();
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
	  try {
      Users.deleteUser( userName );
      EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>( );
      try {
        UserInfo userInfo = dbWrapper.getUnique( new UserInfo(userName) );
        dbWrapper.delete( userInfo );
        dbWrapper.commit( );
      } catch ( EucalyptusCloudException e1 ) {
        dbWrapper.rollback( );
        LOG.error( e1, e1 );
        throw EucalyptusManagement.makeFault("Error while deleting user: " + e1.getMessage( ) );      
      }
    } catch ( NoSuchUserException e1 ) {
      LOG.debug( e1, e1 );
      throw EucalyptusManagement.makeFault( "Unable to delete user" );
    } catch ( UnsupportedOperationException e1 ) {
      LOG.debug( e1, e1 );
      throw EucalyptusManagement.makeFault("Error while deleting user: " + e1.getMessage( ) );      
    }
	}

	public static void commitWebUser( UserInfoWeb webUser ) throws SerializableException
	{
	  String userName = webUser.getUserName( );
    try {
      User user = Users.lookupUser( userName );
      EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>( );
      try {
        UserInfo userInfo = dbWrapper.getUnique( new UserInfo(userName) );
        Composites.project( webUser, userInfo, user );
        dbWrapper.commit( );
      } catch ( EucalyptusCloudException e1 ) {
        dbWrapper.rollback( );
        LOG.error( e1, e1 );
        throw EucalyptusManagement.makeFault("Error while updating user: " + e1.getMessage( ) );      
      }
    } catch ( NoSuchUserException e1 ) {
      LOG.error( e1, e1 );
      throw EucalyptusManagement.makeFault( "Unable to update user" );
    } catch ( UnsupportedOperationException e1 ) {
      LOG.error( e1, e1 );
      throw EucalyptusManagement.makeFault("Error while updating user: " + e1.getMessage( ) );      
    }
	}

	public static String getAdminEmail() throws SerializableException
	{
		EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>(  );
		String addr = null;
		try {
      UserInfo adminUser = db.getUnique( new UserInfo("admin") );
      addr = adminUser.getEmail( );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      throw EucalyptusManagement.makeFault("Administrator account not found" );
    }
    if (addr==null || addr.equals("")) {
      throw EucalyptusManagement.makeFault( "Email address is not set" );
    }
		return addr;
	}

	public static void deleteImage(String imageId)
	throws SerializableException
	{
		ImageInfo searchImg = new ImageInfo( );
		searchImg.setImageId( imageId );
		EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
		List<ImageInfo> imgList= db.query( searchImg );

		if ( imgList.size() > 0 && !imgList.isEmpty() )
		{
			Image foundimgSearch = imgList.get( 0 );
			foundimgSearch.setImageState( "deregistered" );
			db.commit();
		}
		else
		{
			db.rollback();
			throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
		}
	}
	public static void disableImage(String imageId)
	throws SerializableException
	{
	  try {
      new Images._byId( imageId ) {{ new _mutator() {
          @Override public void set( ImageInfo e ) {
            e.setImageState( "deregistered" );
          }}.set( );
      }};
    } catch ( EucalyptusCloudException e ) {
      throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
    }
	}
	public static void enableImage(String imageId)
	throws SerializableException
	{
    try {
      new Images._byId( imageId ) {{ new _mutator() {
          @Override public void set( ImageInfo e ) {
            e.setImageState( "available" );
          }}.set( );
      }};
    } catch ( EucalyptusCloudException e ) {
      throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
    }
	}

	public static SystemConfigWeb getSystemConfig() throws SerializableException
	{
		SystemConfiguration sysConf = SystemConfiguration.getSystemConfiguration();
		return new SystemConfigWeb( 
				sysConf.getDefaultKernel(),
				sysConf.getDefaultRamdisk(),
				sysConf.getMaxUserPublicAddresses(),
				sysConf.isDoDynamicPublicAddresses(),
				sysConf.getSystemReservedPublicAddresses(),
				sysConf.getZeroFillVolumes(),
				sysConf.getDnsDomain(),
				sysConf.getNameserver(),
				sysConf.getNameserverAddress(),
				sysConf.getCloudHost( ));
	}

	public static void setSystemConfig( final SystemConfigWeb systemConfig )
	{
		EntityWrapper<SystemConfiguration> db = new EntityWrapper<SystemConfiguration>();
		SystemConfiguration sysConf = null;
		try
		{
			sysConf = db.getUnique( new SystemConfiguration() );
			sysConf.setCloudHost( systemConfig.getCloudHost() );
			sysConf.setDefaultKernel( systemConfig.getDefaultKernelId() );
			sysConf.setDefaultRamdisk( systemConfig.getDefaultRamdiskId() );

			sysConf.setDnsDomain(systemConfig.getDnsDomain());
			sysConf.setNameserver(systemConfig.getNameserver());
			sysConf.setNameserverAddress(systemConfig.getNameserverAddress());
			sysConf.setMaxUserPublicAddresses( systemConfig.getMaxUserPublicAddresses() );
			sysConf.setDoDynamicPublicAddresses( systemConfig.isDoDynamicPublicAddresses() );
			sysConf.setSystemReservedPublicAddresses( systemConfig.getSystemReservedPublicAddresses() );
			sysConf.setZeroFillVolumes(systemConfig.getZeroFillVolumes());
			db.commit();
			DNSProperties.update();
		}
		catch ( EucalyptusCloudException e )
		{
			sysConf = new SystemConfiguration(
					systemConfig.getDefaultKernelId(),
					systemConfig.getDefaultRamdiskId(),
					systemConfig.getMaxUserPublicAddresses(),
					systemConfig.isDoDynamicPublicAddresses(),
					systemConfig.getSystemReservedPublicAddresses(),
					systemConfig.getZeroFillVolumes(),
					systemConfig.getDnsDomain(),
					systemConfig.getNameserver(),
					systemConfig.getNameserverAddress(),
					systemConfig.getCloudHost( ));
			db.add(sysConf);
			db.commit();
			DNSProperties.update();
		}
    try {
      ListenerRegistry.getInstance( ).fireEvent( new SystemConfigurationEvent( sysConf ) );
    } catch ( EventVetoedException e ) {
      LOG.debug( e, e );
    }
	}

	private static String getExternalIpAddress ()
	{
		String ipAddr = null;
		HttpClient httpClient = new HttpClient();
		//support for http proxy
		if(System.getProperty("http.proxyHost") != null) {
			String proxyHost = System.getProperty("http.proxyHost");
			if(System.getProperty("http.proxyPort") != null) {
				int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
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
		cloudInfo.setInternalHostPort (SystemConfiguration.getInternalIpAddress() + ":8443");
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

}
