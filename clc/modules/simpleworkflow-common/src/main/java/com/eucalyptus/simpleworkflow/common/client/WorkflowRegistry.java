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

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import com.eucalyptus.component.ComponentId;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 *
 */
public class WorkflowRegistry {
  private static final ListMultimap<Class<? extends ComponentId>, Class<?>> workflowClasses =
      Multimaps.synchronizedListMultimap( ArrayListMultimap.<Class<? extends ComponentId>, Class<?>>create() );

  private static final ListMultimap<Class<? extends ComponentId>, Class<?>> activityClasses =
      Multimaps.synchronizedListMultimap( ArrayListMultimap.<Class<? extends ComponentId>, Class<?>>create( ) );

  static void registerWorkflow( @Nonnull final Class<? extends ComponentId> componentIdClass,
                                @Nonnull final Class<?> implementationClass ) {
    register( workflowClasses, componentIdClass, implementationClass );
  }

  static void registerActivities( @Nonnull final Class<? extends ComponentId> componentIdClass,
                                  @Nonnull final Class<?> implementationClass ) {
    register( activityClasses, componentIdClass, implementationClass );
  }

  @Nonnull
  static Iterable<Class<?>> lookupWorkflows( final Class<? extends ComponentId> componentIdClass ) {
    return lookup( workflowClasses, componentIdClass );
  }

  @Nonnull
  static Iterable<Class<?>> lookupActivities( final Class<? extends ComponentId> componentIdClass ) {
    return lookup( activityClasses, componentIdClass );
  }

  private static void register( final Multimap<Class<? extends ComponentId>, Class<?>> registry,
                                final Class<? extends ComponentId> componentIdClass,
                                final Class<?> implementationClass ) {
    registry.put( componentIdClass, implementationClass );
  }

  private static Iterable<Class<?>> lookup( final Multimap<Class<? extends ComponentId>, Class<?>> registry,
                                            final Class<? extends ComponentId> componentIdClass ) {
    return registry.get( componentIdClass );
  }
}
