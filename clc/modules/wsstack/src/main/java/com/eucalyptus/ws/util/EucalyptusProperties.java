package com.eucalyptus.ws.util;

import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.collect.Lists;

public class EucalyptusProperties {

  private static Logger LOG = Logger.getLogger( EucalyptusProperties.class );

  public static List<String> getDisabledOperations( ) {
    if ( System.getProperty( "euca.ebs.disable" ) != null ) {
      EucalyptusProperties.disableBlockStorage = true;
      return Lists.newArrayList( "CreateVolume", "DeleteVolume", "DescribeVolumes", "AttachVolume", "DetachVolume", "CreateSnapshot", "DeleteSnapshot", "DescribeSnapshots" );
    }
    return Lists.newArrayList( );
  }

  public static boolean disableNetworking    = false;
  public static boolean disableBlockStorage  = false;

  public static String  NAME                 = "eucalyptus";
  public static String  WWW_NAME             = "jetty";
  public static String  NETWORK_DEFAULT_NAME = "default";
  public static String  DEBUG_FSTRING        = "[%12s] %s";
  public static String  CLUSTERSINK_REF      = "vm://ClusterSink";

  public enum NETWORK_PROTOCOLS {

    tcp, udp, icmp
  }

  public static String NAME_SHORT           = "euca2";
  public static String FSTRING              = "::[ %-20s: %-50.50s ]::\n";
  public static String IMAGE_MACHINE        = "machine";
  public static String IMAGE_KERNEL         = "kernel";
  public static String IMAGE_RAMDISK        = "ramdisk";
  public static String IMAGE_MACHINE_PREFIX = "emi";
  public static String IMAGE_KERNEL_PREFIX  = "eki";
  public static String IMAGE_RAMDISK_PREFIX = "eri";

  public static String getDName( final String name ) {
    return String.format( "CN=www.eucalyptus.com, OU=Eucalyptus, O=%s, L=Santa Barbara, ST=CA, C=US", name );
  }

  // TODO: bootstrap system configuration
  // public static SystemConfiguration getSystemConfiguration() throws
  // EucalyptusCloudException {
  // EntityWrapper<SystemConfiguration> confDb = new
  // EntityWrapper<SystemConfiguration>();
  // SystemConfiguration conf = null;
  // try {
  // conf = confDb.getUnique( new SystemConfiguration() );
  // }
  // catch ( EucalyptusCloudException e ) {
  // confDb.rollback();
  // throw new EucalyptusCloudException( "Failed to load system configuration",
  // e );
  // }
  // if( conf.getRegistrationId() == null ) {
  // conf.setRegistrationId( UUID.randomUUID().toString() );
  // }
  // if( conf.getSystemReservedPublicAddresses() == null ) {
  // conf.setSystemReservedPublicAddresses( 10 );
  // }
  // if( conf.getMaxUserPublicAddresses() == null ) {
  // conf.setMaxUserPublicAddresses( 5 );
  // }
  // if( conf.isDoDynamicPublicAddresses() == null ) {
  // conf.setDoDynamicPublicAddresses( true );
  // }
  // confDb.commit();
  // String walrusUrl = null;
  // try {
  // walrusUrl = ( new URL( conf.getStorageUrl() + "/" ) ).toString();
  // }
  // catch ( MalformedURLException e ) {
  // throw new EucalyptusCloudException(
  // "System is misconfigured: cannot parse Walrus URL.", e );
  // }
  //
  // return conf;
  // }

  public enum TokenState {

    preallocate, returned, accepted, submitted, allocated, redeemed;
  }
}
