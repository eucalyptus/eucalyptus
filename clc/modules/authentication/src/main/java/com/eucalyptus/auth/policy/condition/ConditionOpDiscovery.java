/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
