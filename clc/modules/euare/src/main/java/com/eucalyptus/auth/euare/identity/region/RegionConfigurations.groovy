/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.euare.identity.region

import com.eucalyptus.configurable.ConfigurableClass
import com.eucalyptus.configurable.ConfigurableField
import com.eucalyptus.configurable.ConfigurableProperty
import com.eucalyptus.configurable.ConfigurablePropertyException
import com.eucalyptus.configurable.PropertyChangeListener
import com.eucalyptus.configurable.PropertyChangeListeners
import com.eucalyptus.records.Logs
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.google.common.base.Objects
import com.google.common.base.Optional
import com.google.common.base.Strings
import groovy.transform.CompileStatic
import org.apache.log4j.Logger
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.ValidationUtils

import javax.annotation.Nonnull


/**
 *
 */
@ConfigurableClass(
    root = "region",
    description = "Configuration for cloud regions."
)
@CompileStatic
class RegionConfigurations {

  private static final Logger logger = Logger.getLogger( RegionConfigurations )

  private static final boolean validateConfiguration =
      Boolean.valueOf( System.getProperty( 'com.eucalyptus.auth.euare.identity.region.validateRegionConfiguration', 'true' ) )

  private static final String REGION_DEFAULT_SSL_PROTOCOLS = "TLSv1.2";

  private static final String REGION_DEFAULT_SSL_CIPHERS = "RSA:DSS:ECDSA:TLS_EMPTY_RENEGOTIATION_INFO_SCSV:!NULL:!EXPORT:!EXPORT1024:!MD5:!DES:!RC4:!ECDHE";

  @ConfigurableField(
      description = "Region configuration document.",
      changeListener = RegionConfigurationPropertyChangeListener )
  public static String REGION_CONFIGURATION = "";

  @ConfigurableField(
      description = "Region name.",
      changeListener = RegionNamePropertyChangeListener )
  public static String REGION_NAME = "";

  @ConfigurableField(
      description = "Enable SSL (HTTPS) for regions.",
      initial = "true",
      changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean REGION_ENABLE_SSL = true

  @ConfigurableField(
      description = "Use default CAs for region SSL connections.",
      initial = "true",
      changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean REGION_SSL_DEFAULT_CAS = true

  @ConfigurableField(
      description = "Verify hostnames for region SSL connections.",
      initial = "true",
      changeListener = PropertyChangeListeners.IsBoolean.class )
  public static Boolean REGION_SSL_VERIFY_HOSTNAMES = true

  @ConfigurableField(
      description = "Protocols to use for region SSL",
      initial = RegionConfigurations.REGION_DEFAULT_SSL_PROTOCOLS )
  public static String REGION_SSL_PROTOCOLS = REGION_DEFAULT_SSL_PROTOCOLS;

  @ConfigurableField(
      description = "Ciphers to use for region SSL",
      initial = RegionConfigurations.REGION_DEFAULT_SSL_CIPHERS )
  public static String REGION_SSL_CIPHERS = REGION_DEFAULT_SSL_CIPHERS;

  static RegionConfiguration parse( final String configuration ) throws RegionConfigurationException {
    final ObjectMapper mapper = new ObjectMapper( )
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE )
    final RegionConfiguration regionConfiguration
    try {
      regionConfiguration = mapper.readValue( new StringReader( configuration ){
        @Override String toString() { "property" } // overridden for better source in error message
      }, RegionConfiguration.class )
    } catch ( JsonProcessingException e ) {
      throw new RegionConfigurationException( e.message )
    }
    final BeanPropertyBindingResult errors = new BeanPropertyBindingResult( regionConfiguration, "RegionConfiguration");
    ValidationUtils.invokeValidator( new RegionConfigurationValidator(errors), regionConfiguration, errors )
    if ( validateConfiguration && errors.hasErrors( ) ) {
      MessageSource source = new StaticMessageSource( ) // default messages will be used
      throw new RegionConfigurationException( source.getMessage( errors.getAllErrors( ).get( 0 ), Locale.getDefault( ) ) )
    }
    regionConfiguration
  }

  /**
   * Get the region configuration.
   *
   * @return The configuration (if set)
   */
  @Nonnull
  static Optional<RegionConfiguration> getRegionConfiguration( ) {
    Optional<RegionConfiguration> configuration = Optional.absent( )
    String configurationText = REGION_CONFIGURATION
    if ( !Strings.isNullOrEmpty( configurationText ) ) {
      try {
        configuration = Optional.of( parse( configurationText ) )
      } catch ( Exception e ) {
        Logs.extreme( ).error( e, e )
        logger.error( "Invalid region configuration: " + e.message )
      }
    }
    configuration
  }

  /**
   * Get the name of the local region
   *
   * @return The name (if set)
   */
  @Nonnull
  static Optional<String> getRegionName( ) {
    Optional.fromNullable( Strings.emptyToNull( REGION_NAME ) )
  }

  static String getRegionNameOrDefault( ) {
    getRegionName( ).or( "eucalyptus" )
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

  static class RegionNamePropertyChangeListener implements PropertyChangeListener<String> {
    @Override
    void fireChange( final ConfigurableProperty property,
                     final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) try {
        if ( !RegionValidator.REGION_NAME_PATTERN.matcher( newValue ).matches( ) ) {
          throw new ConfigurablePropertyException( "Invalid region name: ${newValue}" );
        }
      } catch ( Exception e ) {
        throw new ConfigurablePropertyException( "Invalid region name: ${newValue}", e )
      }
    }
  }

  static class RegionConfigurationPropertyChangeListener implements PropertyChangeListener<String> {
    @Override
    void fireChange( final ConfigurableProperty property,
                     final String newValue ) throws ConfigurablePropertyException {
      if ( !Strings.isNullOrEmpty( newValue ) ) {
        try {
          parse( newValue )
        } catch ( e ) {
          throw new ConfigurablePropertyException( e.getMessage( ), e )
        }
      }
    }
  }
}
