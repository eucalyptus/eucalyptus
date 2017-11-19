/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws;

import java.lang.Object;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BillOfMaterials;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.entities.AbstractPersistent;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "cloud_image_configuration" )
@ConfigurableClass( root = "bootstrap.webservices",
                    description = "Parameters controlling the web services endpoint." )
public class StackConfiguration extends AbstractPersistent {
  
  @ConfigurableField( description = "Channel connect timeout (ms).",
                      changeListener = WebServices.CheckNonNegativeIntegerPropertyChangeListener.class,
                      initial = "500" )
  public static Integer       CHANNEL_CONNECT_TIMEOUT           = 500;
  @ConfigurableField( changeListener = TimeChangeListener.class,
                      description = "Time interval duration (in seconds) during which duplicate signatures will be accepted to accommodate collisions.",
                      initial = "0" )
  public static Integer       REPLAY_SKEW_WINDOW_SEC            = 0;
  @ConfigurableField( description = "A max clock skew value (in seconds) between client and server accepted when validating timestamps in Query/REST protocol.",
                      changeListener = TimeChangeListener.class,
                      initial = "20" )
  public static Integer       CLOCK_SKEW_SEC                    = 20;
  @ConfigurableField( description = "Server socket reuse address.",
                      changeListener = WebServices.CheckBooleanPropertyChangeListener.class,
                      initial = "true" )
  public static       Boolean SERVER_CHANNEL_REUSE_ADDRESS      = true;
  @ConfigurableField( description = "Server socket TCP_NODELAY.",
                      changeListener = WebServices.CheckBooleanPropertyChangeListener.class,
                      initial = "true" )
  public static       Boolean SERVER_CHANNEL_NODELAY            = true;
  @ConfigurableField( description = "Socket reuse address.",
                      changeListener = WebServices.CheckBooleanPropertyChangeListener.class,
                      initial = "true" )
  public static       Boolean CHANNEL_REUSE_ADDRESS             = true;
  @ConfigurableField( description = "Socket keep alive.",
                      changeListener = WebServices.CheckBooleanPropertyChangeListener.class,
                      initial = "true" )
  public static       Boolean CHANNEL_KEEP_ALIVE                = true;
  @ConfigurableField( description = "Server socket TCP_NODELAY.",
                      changeListener = WebServices.CheckBooleanPropertyChangeListener.class,
                      initial = "true" )
  public static       Boolean CHANNEL_NODELAY                   = true;
  @ConfigurableField( description = "Server worker thread pool max.",
                      changeListener = WebServices.CheckNonNegativeIntegerPropertyChangeListener.class,
                      initial = "32" )
  public static Integer       SERVER_POOL_MAX_THREADS           = 128;
  @ConfigurableField( description = "Server max worker memory per connection.",
                      changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class,
                      initial = "0" )
  public static Long          SERVER_POOL_MAX_MEM_PER_CONN      = 0L;
  @ConfigurableField( description = "Server max worker memory total.",
                      changeListener = WebServices.CheckNonNegativeIntegerPropertyChangeListener.class,
                      initial = "0" )
  public static Long          SERVER_POOL_TOTAL_MEM             = 0L;
  
  @ConfigurableField( description = "Service socket select timeout (ms).",
                      changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class,
                      initial = "500" )
  public static Long          SERVER_POOL_TIMEOUT_MILLIS        = 500L;
  
  @ConfigurableField( description = "Server selector thread pool max.",
                      changeListener = WebServices.CheckNonNegativeIntegerPropertyChangeListener.class,
                      initial = "128" )
  public static Integer       SERVER_BOSS_POOL_MAX_THREADS      = 128;
  
  @ConfigurableField( description = "Server max selector memory per connection.",
                      changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class,
                      initial = "0" )
  public static Long          SERVER_BOSS_POOL_MAX_MEM_PER_CONN = 0L;
  
  @ConfigurableField( description = "Server worker thread pool max.",
                      changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class,
                      initial = "0" )
  public static Long          SERVER_BOSS_POOL_TOTAL_MEM        = 0L;
  
  @ConfigurableField( description = "Service socket select timeout (ms).",
                      changeListener = WebServices.CheckNonNegativeLongPropertyChangeListener.class,
                      initial = "500" )
  public static Long          SERVER_BOSS_POOL_TIMEOUT_MILLIS   = 500L;
  
  @ConfigurableField( description = "Port to bind (note: port 8773 is always bound regardless).",
                      changeListener = WebServices.CheckNonNegativeIntegerPropertyChangeListener.class,
                      initial = "8773" )
  public static Integer       PORT                              = 8773;
  public static final Integer INTERNAL_PORT                     = 8773;

  @ConfigurableField( description = "CIDRs matching addresses to bind on (note: default interface is always bound regardless).",
      initial = "0.0.0.0",
      changeListener = WebServices.CheckCidrListPropertyChangeListener.class )
  public static volatile String LISTENER_ADDRESS_MATCH          = "0.0.0.0";

  @ConfigurableField( description = "Record and report service times.", initial = "false" )
  public static Boolean       STATISTICS                        = Boolean.FALSE;
  
  @ConfigurableField( description = "Execute service specific pipeline handlers from a separate thread pool (with respect to I/O).",
                      initial = "false" )
  public static Boolean       ASYNC_PIPELINE                    = Boolean.FALSE;
  
  @ConfigurableField( description = "Execute service operations from a separate thread pool (with respect to I/O).",
                      initial = "false" )
  public static Boolean       ASYNC_OPERATIONS                  = Boolean.FALSE;
  
  @ConfigurableField( description = "Execute internal service operations from a separate thread pool (with respect to I/O).",
                      initial = "false" )
  public static Boolean       ASYNC_INTERNAL_OPERATIONS         = Boolean.TRUE;
  
  @ConfigurableField( description = "Execute internal service operations out of band from the normal service bus.",
                      initial = "true" )
  public static Boolean       OOB_INTERNAL_OPERATIONS           = Boolean.TRUE;
  
  @ConfigurableField( description = "Client idle timeout (secs).", initial = "60" )
  public static Integer       CLIENT_INTERNAL_TIMEOUT_SECS      = 60;

  @ConfigurableField( description = "Client connection timeout (ms).", initial = "3000" )
  public static Integer       CLIENT_INTERNAL_CONNECT_TIMEOUT_MILLIS = 3000;

  @ConfigurableField( description = "Client HMAC signature version 4 enabled", initial = "true" )
  public static Boolean       CLIENT_INTERNAL_HMAC_SIGNATURE_ENABLED = true;

  @ConfigurableField( description = "Cluster connect timeout (ms).", initial = "3000" )
  public static Integer       CLUSTER_CONNECT_TIMEOUT_MILLIS    = 3000;

  @ConfigurableField( description = "Server socket idle time-out.", initial = "60" )
  public static Integer       PIPELINE_IDLE_TIMEOUT_SECONDS     = 60;
  
  @ConfigurableField( description = "Server http chunk max.", initial = "1048576000" )
  public static Integer       CLIENT_HTTP_CHUNK_BUFFER_MAX      = 1048576000;

  @ConfigurableField( description = "Client http pool acquire timeout.", initial = "60000" )
  public static Long          CLIENT_HTTP_POOL_ACQUIRE_TIMEOUT  = 60_000L;

  @ConfigurableField( description = "Client worker thread pool max.", initial = "32" )
  public static Integer       CLIENT_POOL_MAX_THREADS           = 32;
  
  @ConfigurableField( description = "Client message patterns to match for logging" )
  public static String        CLIENT_MESSAGE_LOG_WHITELIST = "";

  @ConfigurableField( description = "Maximum HTTP chunk size (bytes).",
                      initial = "102400")
  public static Integer       HTTP_MAX_CHUNK_BYTES              = 10 * 10 * 1024;

  @ConfigurableField( description = "Maximum Query Pipeline http chunk size (bytes).",
                      initial = "307200")
  public static Integer       PIPELINE_MAX_QUERY_REQUEST_SIZE    = 30 * 10 * 1024;
  
  @ConfigurableField( description = "Maximum HTTP initial line size (bytes).", initial = "4096" )
  public static Integer       HTTP_MAX_INITIAL_LINE_BYTES       = 4 * 1024;
  
  @ConfigurableField( description = "Maximum HTTP headers size (bytes).", initial = "8192" )
  public static Integer       HTTP_MAX_HEADER_BYTES             = 8 * 1024;

  @ConfigurableField( description = "Maximum HTTP requests per persistent connection.", initial = "100" )
  public static Integer       HTTP_MAX_REQUESTS_PER_CONNECTION  = 100;

  @ConfigurableField( description = "HTTP server header for responses (use 'default' for standard)", initial = "default" )
  public static String        HTTP_SERVER_HEADER                = "default";

  @ConfigurableField( description = "Use DNS delegation for eucarc.", initial = "false" )
  @Deprecated  //GRZE: this field will be superceded by new DNS and eucarc support in 3.4: DO NOT USE IT!
  public static Boolean       USE_DNS_DELEGATION                = Boolean.FALSE;
  @ConfigurableField( description = "Use DNS names for instances.", initial = "false" )
  @Deprecated  //GRZE: this field will be superceded by new DNS support in 3.4: DO NOT USE IT!
  public static Boolean       USE_INSTANCE_DNS                = Boolean.FALSE;

  @ConfigurableField( description = "Default scheme prefix in eucarc.",
                      changeListener = TemporarySchemeUpdater.class,
                      initial = "false" )
  @Deprecated  //GRZE: this field will be superceded by new eucarc support in 3.4: DO NOT USE IT!
  public static Boolean       DEFAULT_HTTPS_ENABLED             = Boolean.FALSE;
  
  @ConfigurableField( description = "Default scheme for EC2_URL in eucarc.",
                      changeListener = UriChangeListener.class,
                      initial = "http" )
  @Deprecated  //GRZE: this field will be superceded by new eucarc support in 3.4: DO NOT USE IT!
  public static String        DEFAULT_EC2_URI_SCHEME            = "http";                                      //GRZE: there references to specific services are not in the right scope here. 
                                                                                                                
  @ConfigurableField( description = "Default scheme for S3_URL in eucarc.",
                      changeListener = UriChangeListener.class,
                      initial = "http" )
  @Deprecated  //GRZE: this field will be superceded by new eucarc support in 3.4: DO NOT USE IT!
  public static String        DEFAULT_S3_URI_SCHEME             = "http";                                      //GRZE: there references to specific services are not in the right scope here.
                                                                                                                
  @ConfigurableField( description = "Default scheme for AWS_SNS_URL in eucarc.",
                      changeListener = UriChangeListener.class,
                      initial = "http" )
  @Deprecated  //GRZE: this field will be superceded by new eucarc support in 3.4: DO NOT USE IT!
  public static String        DEFAULT_AWS_SNS_URI_SCHEME        = "http";                                      //GRZE: there references to specific services are not in the right scope here.
                                                                                                                
  @ConfigurableField( description = "Default scheme for EUARE_URL in eucarc.",
                      changeListener = UriChangeListener.class,
                      initial = "http" )
  @Deprecated  //GRZE: this field will be superceded by new eucarc support in 3.4: DO NOT USE IT!
  public static String        DEFAULT_EUARE_URI_SCHEME          = "http";                                      //GRZE: there references to specific services are not in the right scope here.

  @ConfigurableField( description = "Default EUSTORE_URL in eucarc.",
                      changeListener = UriChangeListener.class,
                      initial = "http://emis.eucalyptus.com/" )
  @Deprecated  //GRZE: this field will be superceded by new eucarc support in 3.4: DO NOT USE IT!
  public static String        DEFAULT_EUSTORE_URL               = "http://emis.eucalyptus.com/";               //GRZE: there references to specific services are not in the right scope here.

  @ConfigurableField( description = "Request unknown parameter handling (default|ignore|error).", initial = "default" )
  public static String        UNKNOWN_PARAMETER_HANDLING        = "default";

  @ConfigurableField( description = "Enable request logging.", initial = "true",
      changeListener = WebServices.CheckBooleanPropertyChangeListener.class )
  public static volatile Boolean LOG_REQUESTS                   = true;

  @ConfigurableField( description = "List of services with disabled SOAP APIs.", initial = "",
      changeListener = WebServices.ComponentListPropertyChangeListener.class )
  public static volatile String DISABLED_SOAP_API_COMPONENTS    = "";

  private static final String DEFAULT_SERVER_HEADER = "Eucalyptus/" + BillOfMaterials.getVersion( );

  private static Logger       LOG                               = Logger.getLogger( StackConfiguration.class );

  public static Optional<String> getServerHeader( ) {
    String headerValue = HTTP_SERVER_HEADER.trim( );
    if ( "default".equals( headerValue ) ) {
      headerValue = DEFAULT_SERVER_HEADER;
    } else if ( headerValue.isEmpty( ) ) {
      headerValue = null;
    }
    return Optional.ofNullable( headerValue );
  }

  public enum BasicTransport implements TransportDefinition {
    HTTP {
      @Override
      public String getScheme( ) {
        return "http";
      }
      
      @Override
      public String getSecureScheme( ) {
        return "https";
      }
    },
    JMX {
      @Override
      public String getSecureScheme( ) {
        return getScheme( );
      }
      
      @Override
      public String getScheme( ) {
        return "service:jmx:rmi:///jndi/rmi://";
      }
    },
    JDBC {
      
      @Override
      public String getSecureScheme( ) {
        return getScheme( );
      }
      
      @Override
      public String getScheme( ) {
        return Databases.getJdbcScheme( );
      }
      
    };
    @Override
    public abstract String getScheme( );
    
    @Override
    public abstract String getSecureScheme( );
  }
  
  @Deprecated  //GRZE: this field will be superceded by new DNS support in 3.4: DO NOT USE IT!
  public static String lookupDnsDomain( ) {
    return SystemConfiguration.getSystemConfiguration( ).getDnsDomain( );//TODO:GRZE: sigh. 
  }
  
  public static class TimeChangeListener implements PropertyChangeListener {
    /**
     * @see PropertyChangeListener#fireChange(ConfigurableProperty,
     *      Object)
     * 
     *      Validates that the new value is >= 0
     */
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      
      int time = -1;
      try {
        if ( newValue instanceof String ) {
          time = Integer.parseInt( ( String ) newValue );
        }
      } catch ( NumberFormatException e ) {
        LOG.debug( "Failed to parse int from " + newValue );
      }
      if ( time < 0 )
        throw new ConfigurablePropertyException( "An integer >= 0 is expected for " + t.getFieldName( ) );
      
    }
  }
  
  public static class TemporarySchemeUpdater implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      String scheme = Boolean.TRUE.equals( Boolean.parseBoolean("" + newValue ))
        ? "https"
        : "http";
      DEFAULT_AWS_SNS_URI_SCHEME = DEFAULT_EC2_URI_SCHEME = DEFAULT_EUARE_URI_SCHEME = DEFAULT_S3_URI_SCHEME = scheme;
    }
  }
  
  public static class UriChangeListener implements PropertyChangeListener {
    /**
     * @see PropertyChangeListener#fireChange(ConfigurableProperty,
     *      Object)
     * 
     *      Validates that the new value is >= 0
     */
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      
      String prefix = null;
      
      if ( newValue instanceof String ) {
        prefix = ( String ) newValue;
        if ( "http".equals( prefix ) || "https".equals( prefix ) )
          return;
      }
      try {
          URL url = new URL( (String) newValue );
      } catch ( MalformedURLException e ) {
          throw new ConfigurablePropertyException( "Invalid URL or URL prefix: " + t.getFieldName( ) + e);
      }
      
    }
  }
  
}
