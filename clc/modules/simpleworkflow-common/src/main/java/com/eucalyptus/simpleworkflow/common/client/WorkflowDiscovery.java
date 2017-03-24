/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow.common.client;

import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.amazonaws.services.simpleworkflow.flow.annotations.Activities;
import com.amazonaws.services.simpleworkflow.flow.annotations.Workflow;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.system.Ats;

/**
 *
 */
public class WorkflowDiscovery extends ServiceJarDiscovery {

  private static final Logger logger = Logger.getLogger( WorkflowDiscovery.class );

  @Override
  public boolean processClass( final Class candidate ) throws Exception {
    final Ats ats = Ats.inClassHierarchy( candidate );

    if ( ( ats.has( Workflow.class ) || ats.has( Activities.class ) ) &&
        ats.has( ComponentPart.class ) &&
        !Modifier.isAbstract( candidate.getModifiers() ) &&
        !Modifier.isInterface( candidate.getModifiers( ) ) &&
        !candidate.isLocalClass( ) &&
        !candidate.isAnonymousClass( ) ) {
      final Class<? extends ComponentId> componentIdClass = ats.get( ComponentPart.class ).value( );
      if ( ats.has( Workflow.class ) ) {
        WorkflowRegistry.registerWorkflow( componentIdClass, candidate );
        if ( ats.has ( Hourly.class ) ) {
          WorkflowTimer.addHourlyWorkflow( candidate );
        } else if ( ats.has( Daily.class) ) {
          WorkflowTimer.addDailyWorkflow( candidate );
        } else if ( ats.has( Repeating.class) ) {
          WorkflowTimer.addRepeatingWorkflow( candidate );
        }
      } else {
        WorkflowRegistry.registerActivities( componentIdClass, candidate );
      }
      logger.debug( "Discovered workflow implementation class: " + candidate.getName( ) );
      return true;
    } else {
      return false;
    }
  }

  @Override
  public Double getPriority( ) {
    return 0.3d;
  }
}
