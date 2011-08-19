package com.eucalyptus.auth.policy.condition;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;

public class ConditionOpDiscovery extends ServiceJarDiscovery {
  
  private static Logger LOG = Logger.getLogger( ConditionOpDiscovery.class );
  
  @SuppressWarnings( "unchecked" )
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( ConditionOp.class.isAssignableFrom( candidate ) && Ats.from( candidate ).has( PolicyCondition.class ) ) {
      String[] conditionOps = Ats.from( candidate ).get( PolicyCondition.class ).value( );
      for ( String op : conditionOps ) {
        if ( op != null && !"".equals( op ) ) {
          LOG.debug( "Register policy condition " + op + " for " + candidate.getCanonicalName( ) );
          if ( !Conditions.registerCondition( op, candidate ) ) {
            LOG.error( "Registration conflict for " + candidate.getCanonicalName( ) );
          }
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
