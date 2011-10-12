package com.eucalyptus.auth;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.LdapIntegrationConfiguration;
import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.ldap.LicParser;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;

@ConfigurableClass( root = "authentication", description = "Parameters for authentication." )
public class AuthenticationProperties {

  private static final Logger LOG = Logger.getLogger( AuthenticationProperties.class );

  private static final String LDAP_SYNC_DISABLED = "{ 'sync': { 'enable':'false' } }";
  
  @ConfigurableField( description = "LDAP integration configuration, in JSON", initial = LDAP_SYNC_DISABLED, changeListener = LicChangeListener.class, displayName = "lic" )
  public static String LDAP_INTEGRATION_CONFIGURATION;
  
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
  
  @ConfigurableField( description = "Web session lifetime in minutes", initial = "1440", displayName = "sessionlife" )
  public static Long WEBSESSION_LIFE_IN_MINUTES = 24 * 60L;// 24 hours in minutes
  
}
