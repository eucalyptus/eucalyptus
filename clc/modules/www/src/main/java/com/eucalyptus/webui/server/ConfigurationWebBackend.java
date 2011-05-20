package com.eucalyptus.webui.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.address.AddressingConfiguration;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Dispatcher;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.config.StorageControllerConfiguration;
import com.eucalyptus.config.WalrusConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.EventFailedException;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.event.SystemConfigurationEvent;
import com.eucalyptus.images.ImageConfiguration;
import com.eucalyptus.util.DNSProperties;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.WalrusProperties;
import com.eucalyptus.webui.client.service.EucalyptusServiceException;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.TableDisplay;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.eucalyptus.ws.client.ServiceDispatcher;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationResponseType;
import edu.ucsb.eucalyptus.msgs.GetWalrusConfigurationType;
import edu.ucsb.eucalyptus.msgs.UpdateStorageConfigurationType;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;


/**
 * Translate system configurations into dumb UI display data, and vice versa. 
 * 
 * @author Ye Wen (wenye@eucalyptus.com)
 */
public class ConfigurationWebBackend {
  
  public static final String ID = "Id";
  public static final String NAME = "Name";
  public static final String TYPE = "Type";
  public static final String HOST = "Host";
  
  public static final String CLOUD_NAME = "cloud";
  public static final String WALRUS_NAME = "walrus";
  
  public static final String CLOUD_TYPE = "cloud controller";
  public static final String CLUSTER_TYPE = "cluster controller";
  public static final String STORAGE_TYPE = "storage controller";
  public static final String WALRUS_TYPE = "walrus";
  
  public static final String DEFAULT_KERNEL = "Default kernel";
  public static final String DEFAULT_RAMDISK = "Default ramdisk";
  public static final String DNS_DOMAIN = "DNS domain";
  public static final String DNS_NAMESERVER = "DNS nameserver";
  public static final String DNS_IP = "DNS IP";
  public static final String MAX_USER_PUBLIC_ADDRESSES = "Max public addresses per user";
  public static final String ENABLE_DYNAMIC_PUBLIC_ADDRESSES = "Enable dynamic public addresses";
  
  public static final String PORT = "Port";
  public static final String MAX_VLAN = "Max VLAN tag";
  public static final String MIN_VLAN = "Min VLAN tag";
  
  public static final String COMPONENT_PROPERTY_TYPE_KEY_VALUE = "KEYVALUE";
  public static final String COMPONENT_PROPERTY_TYPE_KEY_VALUE_HIDDEN = "KEYVALUEHIDDEN";
  public static final String COMPONENT_PROPERTY_TYPE_BOOLEAN = "BOOLEAN";
  
  public static final int TYPE_FIELD_INDEX = 2;
  
  // Common fields
  public static final ArrayList<SearchResultFieldDesc> COMMON_CONFIG_FIELD_DESCS = Lists.newArrayList( );
  static {
    COMMON_CONFIG_FIELD_DESCS.add( new SearchResultFieldDesc( ID, ID, false, "0px", TableDisplay.NONE, Type.TEXT, false, true ) );
    COMMON_CONFIG_FIELD_DESCS.add( new SearchResultFieldDesc( NAME, NAME, false, "30%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_CONFIG_FIELD_DESCS.add( new SearchResultFieldDesc( TYPE, TYPE, false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_CONFIG_FIELD_DESCS.add( new SearchResultFieldDesc( "hostName", HOST, false, "30%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
    COMMON_CONFIG_FIELD_DESCS.add( new SearchResultFieldDesc( "port", PORT, false, "20%", TableDisplay.MANDATORY, Type.TEXT, false, false ) );
  }
  // Cloud config extra fields
  public static final ArrayList<SearchResultFieldDesc> CLOUD_CONFIG_EXTRA_FIELD_DESCS = Lists.newArrayList( );
  static {
    CLOUD_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "defaultKernel", DEFAULT_KERNEL, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
    CLOUD_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "defaultRamdisk", DEFAULT_RAMDISK, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
    CLOUD_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "dnsDomain", DNS_DOMAIN, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
    CLOUD_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "nameserver", DNS_NAMESERVER, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
    CLOUD_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "nameserverAddress", DNS_IP, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
    CLOUD_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "doDynamicPublicAddresses", ENABLE_DYNAMIC_PUBLIC_ADDRESSES, false, "0px", TableDisplay.NONE, Type.BOOLEAN, true, false ) );
    CLOUD_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "maxUserPublicAddresses", MAX_USER_PUBLIC_ADDRESSES, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
  }
  // Cluster config extra fields
  public static final ArrayList<SearchResultFieldDesc> CLUSTER_CONFIG_EXTRA_FIELD_DESCS = Lists.newArrayList( );
  static {
    CLUSTER_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "minVlan", MIN_VLAN, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
    CLUSTER_CONFIG_EXTRA_FIELD_DESCS.add( new SearchResultFieldDesc( "maxVlan", MAX_VLAN, false, "0px", TableDisplay.NONE, Type.TEXT, true, false ) );
  }

  public static final String SC_DEFAULT_NAME = "sc-default";
  public static final String SC_DEFAULT_HOST = "sc-default";
  public static final Integer SC_DEFAULT_PORT = 8773;
  
  private static final Logger LOG = Logger.getLogger( ConfigurationWebBackend.class );
  
  private static String makeConfigId( String name, String type ) {
    return type + "." + name;
  }
  
  private static void serializeSystemConfiguration( SystemConfiguration sysConf, SearchResultRow result ) {
    // First fill in the common fields
    result.addField( makeConfigId( CLOUD_NAME, CLOUD_TYPE ) ); // id
    result.addField( CLOUD_NAME );                            // name  
    result.addField( CLOUD_TYPE );                            // type
    result.addField( Internets.localHostAddress( ) );               // host
    result.addField( "" );                                    // port
    // Then fill in the specific fields
    result.addField( ImageConfiguration.getInstance( ).getDefaultKernelId( ) );           // default kernel
    result.addField( ImageConfiguration.getInstance( ).getDefaultRamdiskId( ) );          // default ramdisk
    result.addField( sysConf.getDnsDomain( ) );               // dns domain
    result.addField( sysConf.getNameserver( ) );              // dns nameserver
    result.addField( sysConf.getNameserverAddress( ) );       // dns IP
    result.addField( AddressingConfiguration.getInstance( ).getDoDynamicPublicAddresses( ).toString( ) );// enable dynamic public addresses
    result.addField( AddressingConfiguration.getInstance( ).getMaxUserPublicAddresses( ).toString( ) ); // max public addresses per user    
  }
  
  /**
   * @return the cloud configuration row with its own specific fields for UI display.
   */
  public static SearchResultRow getCloudConfiguration( ) {
    SystemConfiguration sysConf = SystemConfiguration.getSystemConfiguration( );
    SearchResultRow result = new SearchResultRow( );
    // Set the extra field descs
    result.setExtraFieldDescs( CLOUD_CONFIG_EXTRA_FIELD_DESCS );
    // Fill the fields
    serializeSystemConfiguration( sysConf, result );    
    return result;
  }
  
  private static void deserializeSystemConfiguration( SystemConfiguration sysConf, SearchResultRow input ) {
    int i = COMMON_CONFIG_FIELD_DESCS.size( );
    ImageConfiguration.getInstance( ).setDefaultKernelId( input.getField( i++ ) );
    ImageConfiguration.getInstance( ).setDefaultRamdiskId( input.getField( i++ ) );
    sysConf.setDnsDomain( input.getField( i++ ) );
    sysConf.setNameserver( input.getField( i++ ) );
    sysConf.setNameserverAddress( input.getField( i++ ) );
    AddressingConfiguration.getInstance( ).setDoDynamicPublicAddresses( Boolean.parseBoolean( input.getField( i++ ) ) );
    try {
      Integer val = Integer.parseInt( input.getField( i++ ) );
      AddressingConfiguration.getInstance( ).setMaxUserPublicAddresses( val );
    } catch ( Exception e ) { }
  }
  
  /**
   * Set the cloud configuration using UI input.
   * 
   * @param input
   */
  public static void setCloudConfiguration( SearchResultRow input ) throws EucalyptusServiceException {
    EntityWrapper<SystemConfiguration> db = EntityWrapper.get( SystemConfiguration.class );
    SystemConfiguration sysConf = null;
    try {
      sysConf = db.getUnique( new SystemConfiguration( ) );
      deserializeSystemConfiguration( sysConf, input );
      db.commit( );
      DNSProperties.update( );      
    } catch ( EucalyptusCloudException e ) {
      try {
        LOG.debug( e, e );
        sysConf = new SystemConfiguration( );
        deserializeSystemConfiguration( sysConf, input );
        db.persist( sysConf );
        db.commit( );
        DNSProperties.update( );
      } catch ( Exception e1 ) {
        LOG.error( "Failed to set system configuration", e1 );
        throw new EucalyptusServiceException( "Failed to set system configuration", e1 );
      }
    }
    try {
      ListenerRegistry.getInstance( ).fireEvent( new SystemConfigurationEvent( sysConf ) );
    } catch ( EventFailedException e ) {
      LOG.debug( e, e );
    }    
  }
  
  private static void serializeClusterConfiguration( ClusterConfiguration clusterConf, SearchResultRow result ) {
    // Common
    result.addField( makeConfigId( clusterConf.getName( ), CLUSTER_TYPE ) );
    result.addField( clusterConf.getName( ) );
    result.addField( CLUSTER_TYPE );
    result.addField( clusterConf.getHostName( ) );
    result.addField( clusterConf.getPort( ) == null ? null : clusterConf.getPort( ).toString( ) );
    // Specific
    result.addField( clusterConf.getMinVlan( ) == null ? null : clusterConf.getMinVlan( ).toString( ) );
    result.addField( clusterConf.getMaxVlan( ) == null ? null : clusterConf.getMaxVlan( ).toString( ) );
  }
  
  /**
   * @return the list of cluster configurations for UI display.
   */
  public static List<SearchResultRow> getClusterConfigurations( ) {
    List<SearchResultRow> results = Lists.newArrayList( );
    try {
      for ( ClusterConfiguration c : ServiceConfigurations.getConfigurations( ClusterConfiguration.class ) ) {
        SearchResultRow row = new SearchResultRow( );
        row.setExtraFieldDescs( CLUSTER_CONFIG_EXTRA_FIELD_DESCS );
        serializeClusterConfiguration( c, row );
        results.add( row );
      }
    } catch ( Throwable e ) {
      LOG.debug( "Got an error while trying to retrieving storage controller configuration list", e );
    }    
    return results;
  }
  
  private static void deserializeClusterConfiguration( ClusterConfiguration clusterConf, SearchResultRow input ) {
    int i = COMMON_CONFIG_FIELD_DESCS.size( );
    try {
      Integer val = Integer.parseInt( input.getField( i++ ) );
      clusterConf.setMaxVlan( val );
    } catch ( Exception e ) { }
    try {
      Integer val = Integer.parseInt( input.getField( i++ ) );
      clusterConf.setMinVlan( val );
    } catch ( Exception e ) { }
  }
  
  /**
   * Set the cluster configuration using the UI input.
   * 
   * @param input
   */
  public static void setClusterConfiguration( SearchResultRow input ) throws EucalyptusServiceException {
    try {
      ClusterConfiguration clusterConf = ServiceConfigurations.getConfiguration( ClusterConfiguration.class, input.getField( 1 ) );
      deserializeClusterConfiguration( clusterConf, input );
      EntityWrapper.get( clusterConf ).mergeAndCommit( clusterConf );
    } catch ( Exception e ) {
      LOG.error( "Failed to set cluster configuration" );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to set cluster configuration", e );
    }
  }
  
  private static Type propertyTypeToFieldType( String propertyType ) {
    if ( COMPONENT_PROPERTY_TYPE_KEY_VALUE.equals( propertyType ) ) {
      return Type.TEXT;
    } else if ( COMPONENT_PROPERTY_TYPE_KEY_VALUE_HIDDEN.equals( propertyType ) ) {
      return Type.HIDDEN;
    } else if ( COMPONENT_PROPERTY_TYPE_BOOLEAN.equals( propertyType ) ) {
      return Type.BOOLEAN;
    }
    return Type.TEXT;
  }
  
  private static String fieldTypeToPropertyType( Type fieldType ) {
    if ( Type.TEXT == fieldType ) {
      return COMPONENT_PROPERTY_TYPE_KEY_VALUE;
    } else if ( Type.HIDDEN == fieldType ) {
      return COMPONENT_PROPERTY_TYPE_KEY_VALUE_HIDDEN;
    } else if ( Type.BOOLEAN == fieldType ) {
      return COMPONENT_PROPERTY_TYPE_BOOLEAN;
    }
    return COMPONENT_PROPERTY_TYPE_KEY_VALUE;
  }
  
  private static void serializeStorageConfiguration( String type, String name, String host, Integer port, List<ComponentProperty> properties, SearchResultRow result ) {
    // Common fields
    result.addField( makeConfigId( name, type ) );
    result.addField( name );
    result.addField( type );
    result.addField( host );
    result.addField( port == null ? null : port.toString( ) );
    // Dynamic fields
    serializeComponentProperties( properties, result );
  }
  
  private static void serializeComponentProperties( List<ComponentProperty> properties, SearchResultRow result ) {
    // Make sure we see consistent order of properties.
    Collections.<ComponentProperty>sort( properties, new Comparator<ComponentProperty>( ) {
      @Override
      public int compare( ComponentProperty r1, ComponentProperty r2 ) {
        if ( r1 == r2 ) {
          return 0;
        }
        int diff = -1;
        if ( r1 != null ) {
          diff = ( r2 != null ) ? r1.getDisplayName( ).compareTo( r2.getDisplayName( ) ) : 1;
        }
        return diff;
      }
    } );
    for ( ComponentProperty prop : properties ) {
      result.addExtraFieldDesc( new SearchResultFieldDesc( prop.getQualifiedName( ), prop.getDisplayName( ), false, "0px", TableDisplay.NONE, propertyTypeToFieldType( prop.getType( ) ), true, false ) );
      result.addField( prop.getValue( ) );
    }
  }

  private static void deserializeComponentProperties( List<ComponentProperty> properties, SearchResultRow input, int startIndex ) {
    int i = startIndex;
    for ( SearchResultFieldDesc desc : input.getExtraFieldDescs( ) ) {
      properties.add( new ComponentProperty( fieldTypeToPropertyType( desc.getType( ) ), desc.getTitle( ), input.getField( i++ ), desc.getName( ) ) );
    }
  }
  
  private static SearchResultRow createStorageConfiguration( String type, String name, String host, Integer port, List<ComponentProperty> properties ) {
    SearchResultRow result = new SearchResultRow( );
    serializeStorageConfiguration( type, name, host, port, properties, result );
    return result;
  }
  
  /**
   * @return the storage configurations for UI display.
   */
  public static List<SearchResultRow> getStorageConfiguration( ) {
    List<SearchResultRow> results = Lists.newArrayList( );
    for ( ClusterConfiguration cc : ServiceConfigurations.getConfigurations( ClusterConfiguration.class ) ) {
      try {
        if ( Internets.testLocal( cc.getHostName( ) ) && !Components.lookup( "storage" ).isEnabledLocally( ) ) {
          results.add( createStorageConfiguration( STORAGE_TYPE, SC_DEFAULT_NAME, SC_DEFAULT_HOST, SC_DEFAULT_PORT, new ArrayList<ComponentProperty>( ) ) );
          continue;
        }
      } catch ( Exception e ) {
        LOG.debug( "Got an error while trying to retrieving storage controller configuration list", e );
      }
      StorageControllerConfiguration c;
      try {
        c = ServiceConfigurations.getConfiguration( StorageControllerConfiguration.class, cc.getName( ) );
        List<ComponentProperty> properties = Lists.newArrayList( );
        try {
          GetStorageConfigurationResponseType getStorageConfigResponse = sendForStorageInfo( cc, c );
          if ( c.getName( ).equals( getStorageConfigResponse.getName( ) ) ) {
            properties.addAll( getStorageConfigResponse.getStorageParams( ) );
          } else {
            LOG.debug( "Unexpected storage controller name: " + getStorageConfigResponse.getName( ), new Exception( ) );
            LOG.debug( "Expected configuration for SC related to CC: " + LogUtil.dumpObject( c ) );
            LOG.debug( "Received configuration for SC related to CC: " + LogUtil.dumpObject( getStorageConfigResponse ) );
          }
        } catch ( Throwable e ) {
          LOG.debug( "Got an error while trying to communicate with remote storage controller", e );
        }
        results.add( createStorageConfiguration( STORAGE_TYPE, c.getName( ), c.getHostName( ), c.getPort( ), properties ) );
      } catch ( Exception e1 ) {
        results.add( createStorageConfiguration( STORAGE_TYPE, SC_DEFAULT_NAME, SC_DEFAULT_HOST, SC_DEFAULT_PORT, new ArrayList<ComponentProperty>( ) ) );
      }
    }
    return results;
  }

  /**
   * Set properties for a storage controller from UI input.
   * 
   * @param input
   */
  public static void setStorageConfiguration( SearchResultRow input ) throws EucalyptusServiceException  {
    int i = 0;
    i++;//id
    String name = input.getField( i++ );
    i++;//type
    String host = input.getField( i++ );
    Integer port = null;
    try {
      port = Integer.parseInt( input.getField( i++ ) );
    } catch ( Exception e ) {
      LOG.error( "Failed to parse port for storage configuration from UI input" );
      return;
    }
    ArrayList<ComponentProperty> properties = Lists.newArrayList( );
    deserializeComponentProperties( properties, input, i );
    
    StorageControllerConfiguration scConfig = new StorageControllerConfiguration( null/**ASAP: FIXME: GRZE **/, name, host, port);
    final UpdateStorageConfigurationType updateStorageConfiguration = new UpdateStorageConfigurationType( );
    updateStorageConfiguration.setName( scConfig.getName( ) );
    updateStorageConfiguration.setStorageParams( properties );
    Dispatcher scDispatch = ServiceDispatcher.lookup( scConfig );
    try {
      scDispatch.send( updateStorageConfiguration );
    } catch ( Exception e ) {
      LOG.error( "Error sending update configuration message to storage controller: " + updateStorageConfiguration );
      LOG.error( "The storage controller's configuration may be out of sync!" );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to update storage configuration", e ); 
    }
  }

  private static GetStorageConfigurationResponseType sendForStorageInfo( ClusterConfiguration cc, StorageControllerConfiguration c ) throws EucalyptusCloudException {
    GetStorageConfigurationType getStorageConfiguration = new GetStorageConfigurationType( c.getName( ) );
    Dispatcher scDispatch = ServiceDispatcher.lookup( c );
    GetStorageConfigurationResponseType getStorageConfigResponse = scDispatch.send( getStorageConfiguration );
    return getStorageConfigResponse;
  }

  /**
   * @return the list of Walrus configurations for UI display.
   */
  public static List<SearchResultRow> getWalrusConfiguration( ) {
    List<SearchResultRow> results = new ArrayList<SearchResultRow>( );
    try {
      for ( WalrusConfiguration c : ServiceConfigurations.getConfigurations( WalrusConfiguration.class ) ) {
        GetWalrusConfigurationType getWalrusConfiguration = new GetWalrusConfigurationType( WalrusProperties.NAME );
        Dispatcher scDispatch = ServiceDispatcher.lookupSingle( Components.lookup( WALRUS_NAME ) );
        GetWalrusConfigurationResponseType getWalrusConfigResponse = scDispatch.send( getWalrusConfiguration );
        results.add( createStorageConfiguration( WALRUS_TYPE, c.getName( ), c.getHostName( ), c.getPort( ), getWalrusConfigResponse.getProperties( ) ) );
      }
    } catch ( Exception ex ) {
      LOG.error( "Failed to retrieve walrus configuration", ex );
      LOG.debug( ex , ex );
    }
    return results;    
  }
  
  /**
   * Set Walrus configuration using UI input.
   * 
   * @param input
   */
  public static void setWalrusConfiguration( SearchResultRow input ) throws EucalyptusServiceException  {
    ArrayList<ComponentProperty> properties = Lists.newArrayList( );
    deserializeComponentProperties( properties, input, COMMON_CONFIG_FIELD_DESCS.size( ) );
    UpdateWalrusConfigurationType updateWalrusConfiguration = new UpdateWalrusConfigurationType( );
    updateWalrusConfiguration.setName( WalrusProperties.NAME );
    updateWalrusConfiguration.setProperties( properties );
    Dispatcher scDispatch = ServiceDispatcher.lookupSingle( Components.lookup( WALRUS_NAME ) );
    try {
      scDispatch.send( updateWalrusConfiguration );
    } catch ( Exception e ) {
      LOG.error( "Failed to set Walrus configuration", e );
      LOG.debug( e, e );
      throw new EucalyptusServiceException( "Failed to set Walrus configuration", e );
    }
  }
  
}
