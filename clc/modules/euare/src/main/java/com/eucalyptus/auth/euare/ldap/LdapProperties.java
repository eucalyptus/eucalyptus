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
package com.eucalyptus.auth.euare.ldap;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.LicParseException;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;

/**
 *
 */
@ConfigurableClass( root = "authentication", description = "Parameters for authentication." )
public class LdapProperties {
  private static final Logger logger = Logger.getLogger( LdapProperties.class );

  private static final String LDAP_SYNC_DISABLED = "{ \"sync\": { \"enable\":\"false\" } }";

  @ConfigurableField(
      description = "LDAP integration configuration, in JSON",
      initial = LDAP_SYNC_DISABLED,
      changeListener = LicChangeListener.class,
      displayName = "lic"
  )
  public static volatile String LDAP_INTEGRATION_CONFIGURATION;

  public static class LicChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      logger.debug( "LDAP integration configuration changed to " + newValue );

      String licText = ( String ) newValue;
      try {
        LdapIntegrationConfiguration lic = LicParser.getInstance( ).parse( licText );
        LdapSync.setLic( lic );
      } catch ( LicParseException e ) {
        logger.error( e, e );
        throw new ConfigurablePropertyException( "Failed to parse LDAP integration configuration: " + licText + " due to " + e, e );
      }
    }
  }
}
