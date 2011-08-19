package com.eucalyptus.auth.policy.key;

import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.condition.Conditions;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;

public class KeyDiscovery extends ServiceJarDiscovery {
  
  private static Logger LOG = Logger.getLogger( KeyDiscovery.class );
  
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( Key.class.isAssignableFrom( candidate ) && Ats.from( candidate ).has( PolicyKey.class ) ) {
      String key = Ats.from( candidate ).get( PolicyKey.class ).value( );
      if ( key != null && !"".equals( key ) ) {
        LOG.debug( "Register policy key " + key + " for " + candidate.getCanonicalName( ) );
        if ( !Keys.registerKey( key, candidate ) ) {
          LOG.error( "Registration conflict for " + candidate.getCanonicalName( ) );
        }
      }
      return true;
    }
    return false;
  }
  
  @Override
  public Double getPriority( ) {
    return 1.0d;
  }
  
}
