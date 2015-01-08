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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth;

import static com.eucalyptus.upgrade.Upgrades.EntityUpgrade;
import static com.eucalyptus.upgrade.Upgrades.Version.*;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;
import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.ldap.LicParser;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.configurable.StaticDatabasePropertyEntry;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.Intervals;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

@ConfigurableClass( root = "authentication", description = "Parameters for authentication." )
public class AuthenticationProperties {

  private static final Logger LOG = Logger.getLogger( AuthenticationProperties.class );

  private static final String LDAP_SYNC_DISABLED = "{ 'sync': { 'enable':'false' } }";

  private static final String DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE = "Absent";
  
  @ConfigurableField( description = "LDAP integration configuration, in JSON", initial = LDAP_SYNC_DISABLED, changeListener = LicChangeListener.class, displayName = "lic" )
  public static volatile String LDAP_INTEGRATION_CONFIGURATION;

  @ConfigurableField( description = "CIDR to match against for host address selection", initial = "", changeListener = CidrChangeListener.class )
  public static volatile String CREDENTIAL_DOWNLOAD_HOST_MATCH = "";

  @ConfigurableField( description = "Port to use in service URLs when 'bootstrap.webservices.port' is not appropriate.", changeListener = PortChangeListener.class )
  public static volatile String CREDENTIAL_DOWNLOAD_PORT; // String as null value is valid

  @ConfigurableField(
      description = "Strategy for generation of certificates on credential download ( Never | Absent | Limited )",
      initial = DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE,
      changeListener = CredentialDownloadGenerateCertificateChangeListener.class )
  public static volatile String CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE = DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE;

  @ConfigurableField( description = "Limit for access keys per user", initial = "2", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public static volatile Integer ACCESS_KEYS_LIMIT = 2;

  @ConfigurableField( description = "Limit for signing certificates per user", initial = "2", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public static volatile Integer SIGNING_CERTIFICATES_LIMIT = 2;

  @ConfigurableField( description = "Process quotas for system accounts", initial = "true" )
  public static volatile Boolean SYSTEM_ACCOUNT_QUOTA_ENABLED = true;

  @ConfigurableField( description = "Default password expiry time", initial = "60d", changeListener = AuthenticationIntervalPropertyChangeListener.class )
  public static String DEFAULT_PASSWORD_EXPIRY = "60d";

  private static AtomicLong DEFAULT_PASSWORD_EXPIRY_MILLIS = new AtomicLong( TimeUnit.DAYS.toMillis( 60 ) );

  private static volatile CredentialDownloadGenerateCertificateStrategy credentialDownloadGenerateCertificateStrategy =
      Enums.getIfPresent(
          CredentialDownloadGenerateCertificateStrategy.class,
          DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE
      ).orNull( );

  public enum CredentialDownloadGenerateCertificateStrategy {
    /**
     * Include a certificate if the user has no (non-deprecated) certificates
     */
    Absent,

    /**
     * Include a certificate if permitted by IAM limits
     */
    Limited,

    /**
     * Never generated a certificate for credential downloads
     */
    Never,
  }

  @Nonnull
  public static CredentialDownloadGenerateCertificateStrategy getCredentialDownloadGenerateCertificateStrategy( ) {
    return com.google.common.base.Objects.firstNonNull(
        credentialDownloadGenerateCertificateStrategy,
        CredentialDownloadGenerateCertificateStrategy.Never );
  }

  public static class LicChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      LOG.debug( "LDAP integration configuration changed to " + newValue );
      
      String licText = ( String ) newValue;
      try {
        LdapIntegrationConfiguration lic = LicParser.getInstance( ).parse( licText );
        LdapSync.setLic( lic );
      } catch ( LicParseException e ) {
        LOG.error( e, e );
        throw new ConfigurablePropertyException( "Failed to parse LDAP integration configuration: " + licText + " due to " + e, e );
      }
      
    }
  }

  public static final class CidrChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty t, final Object newValue ) throws ConfigurablePropertyException {
      try {
        Optional.fromNullable( Strings.emptyToNull( Objects.toString( newValue, null ) ) ).transform( Cidr.parseUnsafe( ) );
      } catch ( final Exception e ) {
        throw new ConfigurablePropertyException( e.getMessage( ) );
      }
    }
  }

  public static class PortChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      String strValue = Strings.emptyToNull( Objects.toString( newValue, "" ) );
      if ( strValue != null ) {
        final Integer value = Ints.tryParse( strValue );
        if ( value == null || value < 1 || value > 65535 ) {
          throw new ConfigurablePropertyException( "Invalid value: " + newValue );
        }
      }
    }
  }

  public static class CredentialDownloadGenerateCertificateChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty configurableProperty,
                            final Object newValue ) throws ConfigurablePropertyException {

      final Optional<CredentialDownloadGenerateCertificateStrategy> strategy =
          Enums.getIfPresent( CredentialDownloadGenerateCertificateStrategy.class, String.valueOf( newValue ) );
      if ( strategy.isPresent( ) ) {
        credentialDownloadGenerateCertificateStrategy = strategy.get( );
      } else {
        credentialDownloadGenerateCertificateStrategy = Enum.valueOf(
            CredentialDownloadGenerateCertificateStrategy.class, DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE );
        throw new ConfigurablePropertyException( "Invalid certificate download value: " + newValue );
      }
    }
  }

  public static class PropertiesAuthenticationLimitProvider implements AuthenticationLimitProvider {
    @Override
    public long getDefaultPasswordExpirySpi() {
      return DEFAULT_PASSWORD_EXPIRY_MILLIS.get( );
    }

    @Override
    public int getAccessKeyLimitSpi( ) {
      return ACCESS_KEYS_LIMIT;
    }

    @Override
    public int getSigningCertificateLimitSpi( ) {
      return SIGNING_CERTIFICATES_LIMIT;
    }
  }

  public static final class AuthenticationIntervalPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( final ConfigurableProperty configurableProperty,
                            final Object newValue ) throws ConfigurablePropertyException {
      try {
        final String fieldName = configurableProperty.getField().getName() + "_MILLIS";
        final Field field = AuthenticationProperties.class.getDeclaredField( fieldName );
        final long value = Intervals.parse( String.valueOf( newValue ), TimeUnit.MILLISECONDS );
        field.setAccessible( true );
        LOG.info( "Authentication configuration updated " + field.getName() + ": " + value + "ms" );
        ( (AtomicLong) field.get( null ) ).set( value );
      } catch ( ParseException e ) {
        throw new ConfigurablePropertyException( e.getMessage( ), e );
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
  }

  /**
   * Upgrade to raise credential limits on upgraded systems.
   */
  @EntityUpgrade( entities = StaticDatabasePropertyEntry.class, since = v4_1_0, value = Empyrean.class )
  public enum RaiseCredentialLimitPropertyUpgrade implements Predicate<Class> {
    INSTANCE;

    private static Logger LOG = Logger.getLogger( RaiseCredentialLimitPropertyUpgrade.class );

    private static final String CREDENTIAL_LIMIT = "1000000";
    private static final String CERTIFICATE_STRATEGY = CredentialDownloadGenerateCertificateStrategy.Limited.name( );

    @Override
    public boolean apply( Class arg0 ) {
      try {
        LOG.info( "Setting authentication.credential_download_generate_certificate to " + CERTIFICATE_STRATEGY );
        StaticDatabasePropertyEntry.update(
            AuthenticationProperties.class.getName( ) + ".credential_download_generate_certificate",
            "authentication.credential_download_generate_certificate",
            CERTIFICATE_STRATEGY );
        LOG.info( "Setting authentication.access_keys_limit to " + CREDENTIAL_LIMIT );
        StaticDatabasePropertyEntry.update(
            AuthenticationProperties.class.getName( ) + ".access_keys_limit",
            "authentication.access_keys_limit",
            CREDENTIAL_LIMIT );
        LOG.info( "Setting authentication.signing_certificates_limit to " + CREDENTIAL_LIMIT );
        StaticDatabasePropertyEntry.update(
            AuthenticationProperties.class.getName( ) + ".signing_certificates_limit",
            "authentication.signing_certificates_limit",
            CREDENTIAL_LIMIT );
        return true;
      } catch ( final Exception ex ) {
        LOG.error( "Error raising credential limits", ex );
      }
      return true;
    }
  }
}
