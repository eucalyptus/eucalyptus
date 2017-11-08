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
