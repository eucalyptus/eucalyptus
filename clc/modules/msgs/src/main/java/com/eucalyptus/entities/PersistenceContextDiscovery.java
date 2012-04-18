package com.eucalyptus.entities;

import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;

public class PersistenceContextDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( PersistenceContextDiscovery.class );
  
  public PersistenceContextDiscovery( ) {}
  
  @Override
  public Double getPriority( ) {
    return 0.91d;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( PersistenceContexts.isEntityClass( candidate ) ) {
      if ( !Ats.from( candidate ).has( PersistenceContext.class ) ) {
        throw BootstrapException.throwFatal( "Database entity does not have required @PersistenceContext annotation: " + candidate.getCanonicalName( ) );
      } else {
        PersistenceContexts.addEntity( candidate );
        return true;
      }
    } else if ( PersistenceContexts.isSharedEntityClass( candidate ) ) {
      PersistenceContexts.addSharedEntity( candidate );
      return true;
    } else {
      return false;
    }
  }
    
}
