/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare.identity.region;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ValidationUtils;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.records.Logs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 *
 */
@ConfigurableClass( root = "region", description = "Configuration for cloud regions." )
public class RegionConfigurations {

  private static final Logger logger = Logger.getLogger( RegionConfigurations.class );

  private static final boolean validateConfiguration =
      Boolean.valueOf( System.getProperty( "com.eucalyptus.auth.euare.identity.region.validateRegionConfiguration", "true" ) );

  private static final String REGION_DEFAULT_SSL_PROTOCOLS = "TLSv1.2";

  private static final String REGION_DEFAULT_SSL_CIPHERS = "RSA:DSS:ECDSA:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4:!ECDHE";

  @ConfigurableField(
      description = "Region configuration document.",
      changeListener = RegionConfigurations.RegionConfigurationPropertyChangeListener.class )
  public static String REGION_CONFIGURATION = "";

  @ConfigurableField(
      description = "Region name.",
      changeListener = RegionConfigurations.RegionNamePropertyChangeListener.class )
  public static String REGION_NAME = "";

  @ConfigurableField(
      description = "Enable SSL (HTTPS) for regions.",
      initial = "true",
      changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean REGION_ENABLE_SSL = true;

  @ConfigurableField(
      description = "Use default CAs for region SSL connections.",
      initial = "true",
      changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean REGION_SSL_DEFAULT_CAS = true;

  @ConfigurableField(
      description = "Verify hostnames for region SSL connections.",
      initial = "true",
      changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean REGION_SSL_VERIFY_HOSTNAMES = true;

  @ConfigurableField(
      description = "Protocols to use for region SSL",
      initial = RegionConfigurations.REGION_DEFAULT_SSL_PROTOCOLS )
  public static String REGION_SSL_PROTOCOLS = REGION_DEFAULT_SSL_PROTOCOLS;

  @ConfigurableField(
      description = "Ciphers to use for region SSL",
      initial = RegionConfigurations.REGION_DEFAULT_SSL_CIPHERS )
  public static String REGION_SSL_CIPHERS = REGION_DEFAULT_SSL_CIPHERS;

  public static RegionConfiguration parse( final String configuration ) throws RegionConfigurationException {
    final ObjectMapper mapper = new ObjectMapper( );
    mapper.setPropertyNamingStrategy( PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE );
    final RegionConfiguration regionConfiguration;
    try {
      regionConfiguration = mapper.readValue( new StringReader( configuration ) {
        @Override public String toString( ) { return "property"; } // overridden for better source in error message
      }, RegionConfiguration.class );
    } catch ( IOException e ) {
      throw new RegionConfigurationException( e.getMessage( ) );
    }
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult( regionConfiguration, "RegionConfiguration" );
    ValidationUtils.invokeValidator( new RegionConfigurationValidator( errors ), regionConfiguration, errors );
    if ( validateConfiguration && errors.hasErrors( ) ) {
      MessageSource source = new StaticMessageSource( ); // default messages will be used
      throw new RegionConfigurationException( source.getMessage( errors.getAllErrors( ).get( 0 ), Locale.getDefault( ) ) );
    }
    return regionConfiguration;
  }

  /**
   * Get the region configuration.
   *
   * @return The configuration (if set)
   */
  @Nonnull
  public static Optional<RegionConfiguration> getRegionConfiguration( ) {
    Optional<RegionConfiguration> configuration = Optional.absent( );
    String configurationText = REGION_CONFIGURATION;
    if ( !Strings.isNullOrEmpty( configurationText ) ) {
      try {
        configuration = Optional.of( parse( configurationText ) );
      } catch ( Exception e ) {
        Logs.extreme( ).error( e, e );
        logger.error( "Invalid region configuration: " + e.getMessage( ) );
      }
    }
    return configuration;
  }

  /**
   * Get the name of the local region
   *
   * @return The name (if set)
   */
  @Nonnull
  public static Optional<String> getRegionName( ) {
    return Optional.fromNullable( Strings.emptyToNull( REGION_NAME ) );
  }

  public static String getRegionNameOrDefault( ) {
    return getRegionName( ).or( "eucalyptus" );
  }

  public static boolean isUseSsl( ) {
    return Objects.firstNonNull( REGION_ENABLE_SSL, Boolean.TRUE );
  }

  public static boolean isUseDefaultCAs( ) {
    return Objects.firstNonNull( REGION_SSL_DEFAULT_CAS, Boolean.FALSE );
  }

  public static boolean isVerifyHostnames( ) {
    return Objects.firstNonNull( REGION_SSL_VERIFY_HOSTNAMES, Boolean.TRUE );
  }

  public static String getSslProtocols( ) {
    return Objects.firstNonNull( REGION_SSL_PROTOCOLS, REGION_DEFAULT_SSL_PROTOCOLS );
  }

  public static String getSslCiphers( ) {
    return Objects.firstNonNull( REGION_SSL_CIPHERS, REGION_DEFAULT_SSL_CIPHERS );
  }

  public static class RegionNamePropertyChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( final ConfigurableProperty property,
                            final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) try {
        if ( !RegionValidator.REGION_NAME_PATTERN.matcher( newValue ).matches( ) ) {
          throw new ConfigurablePropertyException( "Invalid region name: " + newValue );
        }
      } catch ( Exception e ) {
        throw new ConfigurablePropertyException( "Invalid region name: " + newValue, e );
      }
    }
  }

  public static class RegionConfigurationPropertyChangeListener implements PropertyChangeListener<String> {
    @Override
    public void fireChange( final ConfigurableProperty property,
                            final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) {
        try {
          parse( newValue );
        } catch ( Exception e ) {
          throw new ConfigurablePropertyException( e.getMessage( ), e );
        }
      }
    }
  }
}
