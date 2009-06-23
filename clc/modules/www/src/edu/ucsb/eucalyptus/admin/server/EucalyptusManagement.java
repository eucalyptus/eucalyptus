/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.server;

import com.eucalyptus.util.DNSProperties;
import com.google.gwt.user.client.rpc.SerializableException;
import edu.ucsb.eucalyptus.admin.client.CloudInfoWeb;
import edu.ucsb.eucalyptus.admin.client.ImageInfoWeb;
import edu.ucsb.eucalyptus.admin.client.SystemConfigWeb;
import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;
import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.CertificateInfo;
import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.ImageInfo;
import edu.ucsb.eucalyptus.cloud.entities.NetworkRulesGroup;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import edu.ucsb.eucalyptus.util.StorageProperties;
import edu.ucsb.eucalyptus.util.UserManagement;
import edu.ucsb.eucalyptus.util.WalrusProperties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EucalyptusManagement {

    private static Logger LOG = Logger.getLogger( EucalyptusManagement.class );

    public static UserInfoWeb fromServer( UserInfo source )
    {
        UserInfoWeb target = new UserInfoWeb();
        update( target, source );
        return target;
    }

    public static UserInfo fromClient( UserInfoWeb source )
    {
        UserInfo target = new UserInfo();
        update( target, source );
        return target;
    }

    public static void update( UserInfo target, UserInfo user )
    {
        target.setUserName( user.getUserName() );
        target.setRealName( user.getRealName() );
        target.setEmail( user.getEmail() );
        target.setBCryptedPassword( user.getBCryptedPassword() );
        target.setTelephoneNumber( user.getTelephoneNumber() );
        target.setAffiliation( user.getAffiliation() );
        target.setProjectDescription( user.getProjectDescription() );
        target.setProjectPIName( user.getProjectPIName() );
        target.setConfirmationCode( user.getConfirmationCode() );
        target.setCertificateCode( user.getCertificateCode() );
        target.setIsApproved( user.isApproved() );
        target.setIsConfirmed( user.isConfirmed() );
        target.setIsEnabled( user.isEnabled() );
        target.setIsAdministrator( user.isAdministrator() );
        target.setPasswordExpires( user.getPasswordExpires() );
        target.setQueryId( user.getQueryId() );
        target.setSecretKey( user.getSecretKey() );
        target.setTemporaryPassword( user.getTemporaryPassword() );
    }

    public static void update( UserInfoWeb target, UserInfo user )
    {
        target.setUserName( user.getUserName() );
        target.setRealName( user.getRealName() );
        target.setEmail( user.getEmail() );
        target.setBCryptedPassword( user.getBCryptedPassword() );
        target.setTelephoneNumber( user.getTelephoneNumber() );
        target.setAffiliation( user.getAffiliation() );
        target.setProjectDescription( user.getProjectDescription() );
        target.setProjectPIName( user.getProjectPIName() );
        target.setConfirmationCode( user.getConfirmationCode() );
        target.setCertificateCode( user.getCertificateCode() );
        target.setIsApproved( user.isApproved() );
        target.setIsConfirmed( user.isConfirmed() );
        target.setIsEnabled( user.isEnabled() );
        target.setIsAdministrator( user.isAdministrator() );
        target.setPasswordExpires( user.getPasswordExpires() );
        target.setQueryId( user.getQueryId() );
        target.setSecretKey( user.getSecretKey() );
        target.setTemporaryPassword( user.getTemporaryPassword() );
    }

    public static void update( UserInfo target, UserInfoWeb user )
    {
        target.setUserName( user.getUserName() );
        target.setRealName( user.getRealName() );
        target.setEmail( user.getEmail() );
        target.setBCryptedPassword( user.getBCryptedPassword() );
        target.setTelephoneNumber( user.getTelephoneNumber() );
        target.setAffiliation( user.getAffiliation() );
        target.setProjectDescription( user.getProjectDescription() );
        target.setProjectPIName( user.getProjectPIName() );
        target.setConfirmationCode( user.getConfirmationCode() );
        target.setCertificateCode( user.getCertificateCode() );
        target.setIsApproved( user.isApproved() );
        target.setIsConfirmed( user.isConfirmed() );
        target.setIsEnabled( user.isEnabled() );
        target.setIsAdministrator( user.isAdministrator() );
        target.setPasswordExpires( user.getPasswordExpires() );
        target.setQueryId( user.getQueryId() );
        target.setSecretKey( user.getSecretKey() );
        target.setTemporaryPassword( user.getTemporaryPassword() );
    }

    public static ImageInfoWeb imageConvertToWeb ( ImageInfo source)
    {
        ImageInfoWeb target = new ImageInfoWeb();

        target.setId(source.getId());
        target.setImageId(source.getImageId());
        target.setImageLocation(source.getImageLocation());
        target.setImageState(source.getImageState());
        target.setImageOwnerId(source.getImageOwnerId());
        target.setArchitecture(source.getArchitecture());
        target.setImageType(source.getImageType());
        target.setKernelId(source.getKernelId());
        target.setRamdiskId(source.getRamdiskId());
        target.setPublic(source.getPublic());

        return target;
    }

    public static String getError( String message )
    {
        return "<html><title>HTTP/1.0 403 Forbidden</title><body><div align=\"center\"><p><h1>403: Forbidden</h1></p><p><img src=\"img/error-1.jpg\" /></p><p><h3 style=\"font-color: red;\">" + message + "</h3></p></div></body></html>";
    }

    public static String[] getUserCertificateAliases( String userName ) throws SerializableException
    {
        EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = dbWrapper.query( new UserInfo( userName ) );
        if ( userList.size() != 1 )
        {
            dbWrapper.rollback();
            throw EucalyptusManagement.makeFault("User does not exist" );
        }
        UserInfo user = ( UserInfo ) userList.get( 0 );
        String[] certInfo = new String[user.getCertificates().size()];
        int i = 0;
        for ( CertificateInfo c : user.getCertificates() )
            certInfo[ i++ ] = c.getCertAlias();
        dbWrapper.commit();
        return certInfo;
    }

    /* TODO: for now 'pattern' is ignored and all users are returned */
    public static List <UserInfoWeb> getWebUsers (String pattern) throws SerializableException
    {
        UserInfo searchUser = new UserInfo(); /* empty => return all */
        EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = dbWrapper.query( searchUser );

        List<UserInfoWeb> webUsersList = new ArrayList<UserInfoWeb>();
        for ( UserInfo u : userList)
            webUsersList.add(fromServer(u));
        dbWrapper.commit();
        return webUsersList;
    }

    /* TODO: for now 'pattern' is ignored and all images are returned */
    public static List <ImageInfoWeb> getWebImages (String pattern) throws SerializableException
    {
        ImageInfo searchImage = new ImageInfo(); /* empty => return all */
        EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
        List<ImageInfo> results= db.query( searchImage );
        List<ImageInfoWeb> imagesList = new ArrayList<ImageInfoWeb>();
        for ( ImageInfo i : results )
            imagesList.add(imageConvertToWeb(i));
        db.commit();
        return imagesList;
    }

    public static UserInfoWeb getWebUser( String userName ) throws SerializableException
    {
        EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = dbWrapper.query( new UserInfo( userName ) );
        if ( userList.size() != 1 )
        {
            dbWrapper.rollback();
            throw EucalyptusManagement.makeFault("User does not exist" );
        }
        dbWrapper.commit();
        return EucalyptusManagement.fromServer( userList.get( 0 ) );
    }

    public static UserInfoWeb getWebUserByEmail( String emailAddress ) throws SerializableException
    {
        UserInfo searchUser = new UserInfo( );
        searchUser.setEmail ( emailAddress );
        EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = dbWrapper.query( searchUser );
        if ( userList.size() != 1 )
        {
            dbWrapper.rollback();
            throw EucalyptusManagement.makeFault("User does not exist" );
        }
        dbWrapper.commit();
        return EucalyptusManagement.fromServer( userList.get( 0 ) );
    }

    public static UserInfoWeb getWebUserByCode( String code ) throws SerializableException
    {
        UserInfo searchUser = new UserInfo( );
        searchUser.setConfirmationCode ( code );
        EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = dbWrapper.query( searchUser );
        if ( userList.size() != 1 )
        {
            dbWrapper.rollback();
            throw EucalyptusManagement.makeFault("Invalid confirmation code" );
        }
        dbWrapper.commit();
        return EucalyptusManagement.fromServer( userList.get( 0 ) );
    }

    public static synchronized void addWebUser( UserInfoWeb webUser ) throws SerializableException
    {
        EntityWrapper<UserInfo> dbWrapper = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = dbWrapper.query( new UserInfo( webUser.getUserName() ) );
        if ( userList.size() != 0 )
        {
            dbWrapper.rollback();
            throw EucalyptusManagement.makeFault("User already exists" );
        }

        //String hash = BCrypt.hashpw( webUser.getBCryptedPassword(), BCrypt.gensalt() );
        //webUser.setBCryptedPassword( hash );
        //webUser.setIsAdministrator( false );
        //webUser.setIsApproved( false );
        //webUser.setIsEnabled( false );

        // TODO: add web user properly, with all keys and certs generated, too
        webUser.setConfirmationCode( UserManagement.generateConfirmationCode( webUser.getUserName() ) );
        webUser.setCertificateCode( UserManagement.generateCertificateCode( webUser.getUserName() ) );

        webUser.setSecretKey( UserManagement.generateSecretKey( webUser.getUserName() ) );
        webUser.setQueryId( UserManagement.generateQueryId( webUser.getUserName() ));

        UserInfo newUser = EucalyptusManagement.fromClient( webUser );
        newUser.setReservationId( 0l );
        newUser.getNetworkRulesGroup().add( NetworkRulesGroup.getDefaultGroup() );

        dbWrapper.add( newUser );
        dbWrapper.commit();
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
        EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = db.query( new UserInfo( userName )  );
        if ( userList.size() != 1 )
        {
            db.rollback();
            throw EucalyptusManagement.makeFault("User already exists" );
        }
        db.delete( userList.get(0) );
        db.commit();
    }

    public static void commitWebUser( UserInfoWeb webUser ) throws SerializableException
    {
        UserInfo user = fromClient( webUser );
        commitUser( user );
    }

    public static void commitUser( UserInfo user ) throws SerializableException
    {
        UserInfo searchUser = new UserInfo( user.getUserName() );
        EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = db.query( searchUser );
        UserInfo target = userList.get( 0 );
        if ( userList.size() != 1 )
        {
            db.rollback();
            throw EucalyptusManagement.makeFault( "User does not exist" );
        }
        update( target, user );
        db.commit();
    }

    public static String getAdminEmail() throws SerializableException
    {
        UserInfo searchUser = new UserInfo();
        searchUser.setIsAdministrator( true );
        EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
        List<UserInfo> userList = db.query( searchUser );
        if ( userList.size() < 1 || userList.isEmpty() )
        {
            db.rollback();
            throw EucalyptusManagement.makeFault("Administrator account not found" );
        }

        UserInfo first = userList.get( 0 );
        String addr = first.getEmail();
        if (addr==null || addr.equals("")) {
            db.rollback();
            throw EucalyptusManagement.makeFault( "Email address is not set" );
        }
        db.commit();
        return addr;

        //return Configuration.getConfiguration().getAdminEmail();
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
            ImageInfo foundimgSearch = imgList.get( 0 );
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
        ImageInfo searchImg = new ImageInfo( );
        searchImg.setImageId( imageId );
        EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
        List<ImageInfo> imgList= db.query( searchImg );

        if ( imgList.size() > 0 && !imgList.isEmpty() )
        {
            ImageInfo foundimgSearch = imgList.get( 0 );
            foundimgSearch.setImageState( "deregistered" );
            db.commit();
        }
        else
        {
            db.rollback();
            throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
        }
    }
    public static void enableImage(String imageId)
            throws SerializableException
    {
        ImageInfo searchImg = new ImageInfo( );
        searchImg.setImageId( imageId );
        EntityWrapper<ImageInfo> db = new EntityWrapper<ImageInfo>();
        List<ImageInfo> imgList= db.query( searchImg );

        if ( imgList.size() > 0 && !imgList.isEmpty() )
        {
            ImageInfo foundimgSearch = imgList.get( 0 );
            foundimgSearch.setImageState( "available" );
            db.commit();
        }
        else
        {
            db.rollback();
            throw EucalyptusManagement.makeFault ("Specified image was not found, sorry.");
        }
    }

    public static String getInternalIpAddress ()
    {
        String ipAddr = null;
        String localAddr = "127.0.0.1";

        List<NetworkInterface> ifaces = null;
        try {
            ifaces = Collections.list( NetworkInterface.getNetworkInterfaces() );
        }
        catch ( SocketException e1 ) {}

        for ( NetworkInterface iface : ifaces )
            try {
                if ( !iface.isLoopback() && !iface.isVirtual() && iface.isUp() ) {
                    for ( InetAddress iaddr : Collections.list( iface.getInetAddresses() ) ) {
                        if ( !iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address ) ) {
                            ipAddr = iaddr.getHostAddress();
                        } else if ( iaddr.isSiteLocalAddress() && !( iaddr instanceof Inet6Address ) ) {
                            localAddr = iaddr.getHostAddress();
                        }
                    }
                }
            }
            catch ( SocketException e1 ) {}

        return ipAddr == null ? localAddr : ipAddr;
    }

    public static SystemConfigWeb getSystemConfig() throws SerializableException
    {
        EntityWrapper<SystemConfiguration> db = new EntityWrapper<SystemConfiguration>();
        SystemConfiguration sysConf;
        try
        {
            sysConf = db.getUnique( new SystemConfiguration() );
            validateSystemConfiguration(sysConf);
        } catch(EucalyptusCloudException e) {
            sysConf = validateSystemConfiguration(null);
        }
        finally {
            db.commit();
        }
        return new SystemConfigWeb( sysConf.getStorageUrl(),
                                    sysConf.getStorageDir(),
                                    sysConf.getStorageMaxBucketsPerUser(),
                                    sysConf.getStorageMaxBucketSizeInMB(),
                                    sysConf.getStorageMaxCacheSizeInMB(),
                                    sysConf.getStorageMaxTotalSnapshotSizeInGb(),
                                    sysConf.getStorageMaxTotalVolumeSizeInGb(),
                                    sysConf.getStorageMaxVolumeSizeInGB(),
                                    sysConf.getStorageVolumesDir(),
                                    sysConf.getDefaultKernel(),
                                    sysConf.getDefaultRamdisk(),
                                    sysConf.getMaxUserPublicAddresses(),
                                    sysConf.isDoDynamicPublicAddresses(),
                                    sysConf.getSystemReservedPublicAddresses(),
                                    sysConf.getDnsDomain(),
                                    sysConf.getNameserver(),
                                    sysConf.getNameserverAddress());
    }

    private static SystemConfiguration validateSystemConfiguration(SystemConfiguration sysConf) {
        if(sysConf == null) {
            sysConf = new SystemConfiguration();
        }
        if(sysConf.getStorageUrl() == null) {
            String ipAddr = getInternalIpAddress ();
            String wUrl = String.format( "http://%s:8773/services/" + WalrusProperties.SERVICE_NAME, ipAddr );
            sysConf.setStorageUrl(wUrl);
        }
        if(sysConf.getStorageDir() == null) {
            sysConf.setStorageDir(WalrusProperties.bucketRootDirectory.replaceAll( "//","/" ));
        }
        if(sysConf.getStorageMaxBucketsPerUser() == null) {
            sysConf.setStorageMaxBucketsPerUser(WalrusProperties.MAX_BUCKETS_PER_USER);
        }
        if(sysConf.getStorageMaxBucketSizeInMB() == null) {
            sysConf.setStorageMaxBucketSizeInMB((int)(WalrusProperties.MAX_BUCKET_SIZE / WalrusProperties.M));
        }
        if(sysConf.getStorageMaxCacheSizeInMB() == null) {
            sysConf.setStorageMaxCacheSizeInMB((int)(WalrusProperties.IMAGE_CACHE_SIZE / WalrusProperties.M));
        }
        if(sysConf.getStorageMaxTotalSnapshotSizeInGb() == null) {
            sysConf.setStorageMaxTotalSnapshotSizeInGb(StorageProperties.MAX_TOTAL_SNAPSHOT_SIZE);
        }
        if(sysConf.getStorageMaxTotalVolumeSizeInGb() == null) {
            sysConf.setStorageMaxTotalVolumeSizeInGb(StorageProperties.MAX_TOTAL_VOLUME_SIZE);
        }
        if(sysConf.getStorageMaxVolumeSizeInGB() == null) {
            sysConf.setStorageMaxVolumeSizeInGB(StorageProperties.MAX_VOLUME_SIZE);
        }
        if(sysConf.getStorageVolumesDir() == null) {
            sysConf.setStorageVolumesDir(StorageProperties.storageRootDirectory);
        }
        if(sysConf.getDefaultKernel() == null) {
            ImageInfo q = new ImageInfo();
            EntityWrapper<ImageInfo> db2 = new EntityWrapper<ImageInfo>();

            q.setImageType( EucalyptusProperties.IMAGE_KERNEL );
            List<ImageInfo> res = db2.query(q);
            if( res.size() > 0 )
                sysConf.setDefaultKernel(res.get(0).getImageId());
        }
        if(sysConf.getDefaultRamdisk() == null) {
            ImageInfo q = new ImageInfo();
            EntityWrapper<ImageInfo> db2 = new EntityWrapper<ImageInfo>();

            q.setImageType( EucalyptusProperties.IMAGE_RAMDISK );
            List<ImageInfo> res = db2.query(q);
            if( res.size() > 0 )
                sysConf.setDefaultRamdisk(res.get(0).getImageId());
        }
        if(sysConf.getDnsDomain() == null) {
            sysConf.setDnsDomain(DNSProperties.DOMAIN);
        }
        if(sysConf.getNameserver() == null) {
            sysConf.setNameserver(DNSProperties.NS_HOST);
        }
        if(sysConf.getNameserverAddress() == null) {
            sysConf.setNameserverAddress(DNSProperties.NS_IP);
        }
        if( sysConf.getMaxUserPublicAddresses() == null ) {
          sysConf.setMaxUserPublicAddresses( 5 );
        }
        if( sysConf.isDoDynamicPublicAddresses() == null ) {
          sysConf.setDoDynamicPublicAddresses( true );
        }
        if( sysConf.getSystemReservedPublicAddresses() == null ) {
          sysConf.setSystemReservedPublicAddresses( 10 );
        }
        return sysConf;
    }

    public static void setSystemConfig( final SystemConfigWeb systemConfig )
    {
        EntityWrapper<SystemConfiguration> db = new EntityWrapper<SystemConfiguration>();
        try
        {
            SystemConfiguration sysConf = db.getUnique( new SystemConfiguration() );
            //:: TODO: verify the URL :://
            sysConf.setStorageUrl( systemConfig.getStorageUrl() );
            //:: TODO: check the path exists && is writeable, create directory if needed :://
            sysConf.setStorageDir( systemConfig.getStoragePath() );
            //:: TODO: verify the EKI :://
            sysConf.setDefaultKernel( systemConfig.getDefaultKernelId() );
            //:: TODO: verify the ERI :://
            sysConf.setDefaultRamdisk( systemConfig.getDefaultRamdiskId() );

            sysConf.setStorageMaxBucketsPerUser( systemConfig.getStorageMaxBucketsPerUser() );
            sysConf.setStorageMaxBucketSizeInMB( systemConfig.getStorageMaxBucketSizeInMB() );
            sysConf.setStorageMaxCacheSizeInMB ( systemConfig.getStorageMaxCacheSizeInMB() );
            sysConf.setStorageMaxTotalVolumeSizeInGb ( systemConfig.getStorageVolumesTotalInGB() );
            sysConf.setStorageMaxTotalSnapshotSizeInGb( systemConfig.getStorageSnapshotsTotalInGB() );
            sysConf.setStorageMaxVolumeSizeInGB (systemConfig.getStorageMaxVolumeSizeInGB());
            sysConf.setStorageVolumesDir (systemConfig.getStorageVolumesPath());
            sysConf.setDnsDomain(systemConfig.getDnsDomain());
            sysConf.setNameserver(systemConfig.getNameserver());
            sysConf.setNameserverAddress(systemConfig.getNameserverAddress());
            sysConf.setMaxUserPublicAddresses( systemConfig.getMaxUserPublicAddresses() );
            sysConf.setDoDynamicPublicAddresses( systemConfig.isDoDynamicPublicAddresses() );
            sysConf.setSystemReservedPublicAddresses( systemConfig.getSystemReservedPublicAddresses() );
            db.commit();
            WalrusProperties.update();
            StorageProperties.update();
        }
        catch ( EucalyptusCloudException e )
        {
            db.add( new SystemConfiguration(systemConfig.getStorageUrl(),
                    systemConfig.getDefaultKernelId(),
                    systemConfig.getDefaultRamdiskId(),
                    systemConfig.getStoragePath(),
                    systemConfig.getStorageMaxBucketsPerUser() ,
                    systemConfig.getStorageMaxBucketSizeInMB(),
                    systemConfig.getStorageMaxCacheSizeInMB(),
                    systemConfig.getStorageVolumesTotalInGB(),
                    systemConfig.getStorageSnapshotsTotalInGB(),
                    systemConfig.getStorageMaxVolumeSizeInGB(),
                    systemConfig.getStorageVolumesPath(),
                    systemConfig.getMaxUserPublicAddresses(),
                    systemConfig.isDoDynamicPublicAddresses(),
                    systemConfig.getSystemReservedPublicAddresses(),
                    systemConfig.getDnsDomain(),
                    systemConfig.getNameserver(),
                    systemConfig.getNameserverAddress()));
            db.commit();
            WalrusProperties.update();
            StorageProperties.update();
            DNSProperties.update();
        }
    }

    private static String getExternalIpAddress ()
    {
        String ipAddr = null;
        HttpClient httpClient = new HttpClient();
        // Use Rightscale's "whoami" service
        GetMethod method = new GetMethod("https://my.rightscale.com/whoami?api_version=1.0&cloud=0");
        Integer timeoutMs = new Integer(3 * 1000); // TODO: is this working?
        method.getParams().setSoTimeout(timeoutMs);

        try {
            httpClient.executeMethod(method);
            String str = method.getResponseBodyAsString();
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
        try {
            cloudRegisterId = EucalyptusProperties.getSystemConfiguration().getRegistrationId();
        } catch ( EucalyptusCloudException e ) {
            cloudRegisterId = "this should never be unset!";
        }
        CloudInfoWeb cloudInfo = new CloudInfoWeb();
        cloudInfo.setInternalHostPort (getInternalIpAddress() + ":8443");
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
