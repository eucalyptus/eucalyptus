/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
