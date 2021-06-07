/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.binding;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Ats;
import com.google.common.base.Optional;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Resources;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 *
 */
@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.RemoteServicesInit )
@DependsLocal(  Empyrean.class )
public class BindingBootstrap extends Bootstrapper.Simple {
  private final Logger logger = Logger.getLogger( BindingBootstrap.class );

  @Override
  public boolean load( ) throws Exception {
    logger.info( "Registering REST bindings" );

    for ( final ComponentId componentId : ComponentIds.list( ) ) {
      List<String> messageClassList = Collections.emptyList( );
      try {
        messageClassList = Resources.readLines(
            Resources.getResource( componentId.getMessagesIndexFileName( ) ),
            StandardCharsets.UTF_8 );
      } catch ( final Exception ignore ) {
        // message index is optional
      }
      if ( !messageClassList.isEmpty( ) ) {
        logger.info( "Processing message index: " + componentId.getMessagesIndexFileName( ) );
        final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader( );
        final ListMultimap<Tuple2<String,String>, Class<?>> httpToClassMap = LinkedListMultimap.create( );
        for ( final String messageClass : messageClassList ) {
          final Class<?> mappedClass = systemClassLoader.loadClass( messageClass );
          final Ats ats = Ats.from( mappedClass );
          if ( ats.has( HttpRequestMapping.class ) ) {
            final HttpRequestMapping httpRequestMapping = ats.get( HttpRequestMapping.class );
            if ( !httpRequestMapping.uri( ).isEmpty( ) ) {
              httpToClassMap.put( Tuple.of(httpRequestMapping.method(), httpRequestMapping.uri()), mappedClass );
            }
          }
        }
        BindingManager.registerBinding(
            Optional.of( componentId.getClass( ) ),
            RestBinding.of( Multimaps.asMap( httpToClassMap ) ) );
      }
    }
    return true;
  }
}
