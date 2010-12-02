package com.eucalyptus.auth.policy;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class ActionDiscovery extends ServiceJarDiscovery {
  
  private static Logger LOG = Logger.getLogger( ActionDiscovery.class );
  
  @SuppressWarnings( "unchecked" )
  @Override
  public boolean processClass( Class candidate ) throws Throwable {
    if ( BaseMessage.class.isAssignableFrom( candidate ) && Ats.from( candidate ).has( PolicyAction.class ) ) {
      PolicyAction action = Ats.from( candidate ).get( PolicyAction.class );
      LOG.debug( "Register policy action " + action.action( ) + " for " + candidate.getCanonicalName( ) );
      if ( !PolicySpec.registerAction( candidate, action.vendor( ) + ":" + action.action( ) ) ) {
        LOG.error( "Registration conflict for " + candidate.getCanonicalName( ) );
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
