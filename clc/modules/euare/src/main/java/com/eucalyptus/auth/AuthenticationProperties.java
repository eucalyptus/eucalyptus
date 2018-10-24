/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

  private static final String DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE = "Absent";

  private static final String DEFAULT_AUTHORIZATION_CACHE = "maximumSize=1000, expireAfterWrite=1m";

  private static final String DEFAULT_MAX_ATTACHMENTS_TEXT = "10";

  private static final String DEFAULT_MAX_POLICY_SIZE_TEXT = "16384";

  @ConfigurableField( description = "CIDR to match against for host address selection", initial = "", changeListener = CidrChangeListener.class )
  public static volatile String CREDENTIAL_DOWNLOAD_HOST_MATCH = "";

  @ConfigurableField( description = "Port to use in service URLs when 'bootstrap.webservices.port' is not appropriate.", changeListener = PortChangeListener.class )
  public static volatile String CREDENTIAL_DOWNLOAD_PORT; // String as null value is valid

  @ConfigurableField(
      description = "Strategy for generation of certificates on credential download ( Never | Absent | Limited )",
      initial = DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE )
  public static volatile String CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE = DEFAULT_CREDENTIAL_DOWNLOAD_GENERATE_CERTIFICATE;

  @ConfigurableField( description = "Limit for access keys per user", initial = "2", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public static volatile Integer ACCESS_KEYS_LIMIT = 2;

  @ConfigurableField( description = "Limit for signing certificates per user", initial = "2", changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public static volatile Integer SIGNING_CERTIFICATES_LIMIT = 2;

  @ConfigurableField( description = "Process quotas for system accounts", initial = "true" )
  public static volatile Boolean SYSTEM_ACCOUNT_QUOTA_ENABLED = true;

  @ConfigurableField( description = "Default password expiry time", initial = "60d", changeListener = AuthenticationIntervalPropertyChangeListener.class )
  public static volatile String DEFAULT_PASSWORD_EXPIRY = "60d";

  @ConfigurableField(
      description = "Authorization cache configuration, for credentials and authorization metadata",
      initial = DEFAULT_AUTHORIZATION_CACHE,
      changeListener = PropertyChangeListeners.CacheSpecListener.class )
  public static volatile String AUTHORIZATION_CACHE = DEFAULT_AUTHORIZATION_CACHE;

  @ConfigurableField( description = "Default expiry for cached authorization metadata", initial = "5s", changeListener = AuthenticationIntervalPropertyChangeListener.class )
  public static volatile String AUTHORIZATION_EXPIRY = "5s";

  @ConfigurableField( description = "Default expiry for re-use of cached authorization metadata on failure", initial = "0s", changeListener = AuthenticationIntervalPropertyChangeListener.class )
  public static volatile String AUTHORIZATION_REUSE_EXPIRY = "0s";

  @ConfigurableField( description = "Maximum number of attached managed policies", initial = DEFAULT_MAX_ATTACHMENTS_TEXT )
  public static volatile int MAX_POLICY_ATTACHMENTS = Integer.parseInt( DEFAULT_MAX_ATTACHMENTS_TEXT );

  @ConfigurableField( description = "Maximum size for an IAM policy (bytes)", initial = DEFAULT_MAX_POLICY_SIZE_TEXT )
  public static volatile int MAX_POLICY_SIZE = Integer.parseInt( DEFAULT_MAX_POLICY_SIZE_TEXT );

  @ConfigurableField( description = "Use strict validation for IAM policy syntax", initial = "true" )
  public static volatile boolean STRICT_POLICY_VALIDATION = true;

  private static AtomicLong DEFAULT_PASSWORD_EXPIRY_MILLIS = new AtomicLong( TimeUnit.DAYS.toMillis( 60 ) );

  private static AtomicLong AUTHORIZATION_EXPIRY_MILLIS = new AtomicLong( TimeUnit.SECONDS.toMillis( 5 ) );

  private static AtomicLong AUTHORIZATION_REUSE_EXPIRY_MILLIS = new AtomicLong( 0 );

  public static long getAuthorizationExpiry( ) {
    return AUTHORIZATION_EXPIRY_MILLIS.get( );
  }

  public static long getAuthorizationReuseExpiry( ) {
    return AUTHORIZATION_REUSE_EXPIRY_MILLIS.get( );
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

    @Override
    public int getPolicyAttachmentLimitSpi( ) {
      return MAX_POLICY_ATTACHMENTS;
    }
    @Override
    public int getPolicySizeLimitSpi( ) {
      return MAX_POLICY_SIZE;
    }

    @Override
    public boolean getUseValidatingPolicyParserSpi( ) {
      return STRICT_POLICY_VALIDATION;
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

    @Override
    public boolean apply( Class arg0 ) {
      try {
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
