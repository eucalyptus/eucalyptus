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
package com.eucalyptus.cloud

import com.eucalyptus.bootstrap.ServiceJarDiscovery
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

import java.lang.reflect.Modifier

/**
 *
 */
@CompileStatic
class VmInstanceLifecycleHelperDiscovery extends ServiceJarDiscovery {
  private final Logger logger = Logger.getLogger( VmInstanceLifecycleHelperDiscovery )

  @Override
  boolean processClass( final Class candidate ) throws Exception {
    boolean accepted = false
    if ( VmInstanceLifecycleHelper.isAssignableFrom( candidate ) &&
        !Modifier.isAbstract( candidate.modifiers ) ) {
      try {
        VmInstanceLifecycleHelpers.register( VmInstanceLifecycleHelper.cast( candidate.newInstance( ) ) )
        accepted = true
      } catch ( Exception e ) {
        logger.error( "Error registering instance lifecycle helper: ${candidate.name}", e );
      }
    }
    accepted
  }

  @Override
  Double getPriority() {
    0.3d
  }
}
