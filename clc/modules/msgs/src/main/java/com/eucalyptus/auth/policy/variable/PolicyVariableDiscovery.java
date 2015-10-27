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
package com.eucalyptus.auth.policy.variable;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;

/**
 *
 */
public class PolicyVariableDiscovery extends ServiceJarDiscovery {

  private static Logger logger = Logger.getLogger( PolicyVariableDiscovery.class );

  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( PolicyVariable.class.isAssignableFrom( candidate ) && 
        !Modifier.isAbstract( candidate.getModifiers( ) ) &&
        Modifier.isPublic( candidate.getModifiers( ) ) ) {
      try {
        final PolicyVariable policyVariable = (PolicyVariable) candidate.newInstance( );
        logger.debug( "Registering policy variable " + policyVariable.getQName( ) + " for " + candidate.getCanonicalName( ) );
        if ( !PolicyVariables.registerPolicyVariable( policyVariable ) ) {
          logger.error( "Registration conflict for policy variable " + candidate.getCanonicalName( ) );
        }
        return true;
      } catch ( final ReflectiveOperationException e ) {
        logger.error( "Error creating instance for policy variable " + candidate.getCanonicalName( ), e );
      }
    }
    return false;
  }

  @Override
  public Double getPriority( ) {
    return 1.0d;
  }
}
