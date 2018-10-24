/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.context;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.BeanFactoryMessageChannelDestinationResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.configurable.PropertyChangeListeners;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Pair;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

@ConfigurableClass( root = "bootstrap.servicebus", description = "Parameters having to do with the service bus." )
public class ServiceContext {
  private static Logger                        LOG                      = Logger.getLogger( ServiceContext.class );
  private static LinkedBlockingQueue<Pair<Long,CompletableFuture<?>>> FUTURE_QUEUE = new LinkedBlockingQueue<>( );

  @ConfigurableField( initial = "0", description = "Do a soft reset", changeListener = HupListener.class )
  public volatile static Integer HUP = 0;

  @ConfigurableField( initial = "256", description = "Common thread pool size (zero enables dispatch/component pools)" )
  public volatile static Integer COMMON_THREAD_POOL_SIZE = 256;

  @ConfigurableField( initial = "64", description = "Component thread pool size",
      changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static Integer COMPONENT_THREAD_POOL_SIZE = 64;

  @ConfigurableField( initial = "256", description = "Message dispatch thread pool size",
      changeListener = PropertyChangeListeners.IsPositiveInteger.class )
  public volatile static Integer DISPATCH_THREAD_POOL_SIZE = 256;

  @ConfigurableField( initial = "60", description = "Message context timeout (seconds)" )
  public volatile static Integer CONTEXT_TIMEOUT = 60;

  @ConfigurableField( description = "Message patterns to match for logging" )
  public volatile static String CONTEXT_MESSAGE_LOG_WHITELIST = "";

  public static class HupListener implements PropertyChangeListener {
    @Override
    public void fireChange( ConfigurableProperty t, Object newValue ) throws ConfigurablePropertyException {
      if ( Bootstrap.isFinished( ) ) {
        ServiceContextManager.restartSync( );
      }
    }
  }

  public static class ServiceContextFutureTimeoutEventListener implements EventListener<ClockTick> {

    public static void register( ) {
      Listeners.register( ClockTick.class, new ServiceContextFutureTimeoutEventListener( ) );
    }

    @Override
    public void fireEvent( final ClockTick event ) {
      try {
        final long timeout = TimeUnit.SECONDS.toMillis( CONTEXT_TIMEOUT * 2 );
        final long now = System.currentTimeMillis( );
        while ( true ) {
          final Pair<Long,CompletableFuture<?>> futurePair = FUTURE_QUEUE.peek( );
          boolean timedOut = false;
          if ( futurePair != null &&
              ( futurePair.getRight( ).isDone( ) || ( timedOut = futurePair.getLeft( ) + timeout < now ) ) ) {
            FUTURE_QUEUE.remove( );
            if ( timedOut ) {
              futurePair.getRight( ).cancel( true );
            }
          } else {
            break;
          }
        }
      } catch ( Exception e ) {
        LOG.error( e, e );
      }
    }
  }

  public static <M> void dispatch( ComponentId dest, M msg ) throws Exception {
    dispatch( getComponentExecutor( dest ), dest.getChannelName( ), msg );
  }

  public static <M> void dispatch( String dest, M msg ) throws Exception {
    dispatch( getDispatchExecutor( ), dest, msg );
  }

  public static <M> void dispatch( Executor executor, String dest, M msg ) throws Exception {
    final CompletableFuture<?> errorFuture = new CompletableFuture<>( );
    executor.execute( runWithContext( dest, msg, ctx -> {
      try {
        final MessagingTemplate template = getMessagingTemplate( );
        final MessageChannel channel;
        try {
          channel = template.getDestinationResolver( ).resolveDestination( dest );
        } catch ( final IllegalStateException e ) {
          throw new MessagingException( "Service context not available", e );
        }
        errorFuture.complete( null );
        final GenericMessage<M> message = new GenericMessage<>( msg );
        template.send( channel, message );
      } catch ( MessagingException e ) {
        if ( errorFuture.isDone( ) ) {
          final MessagingTemplate template = getMessagingTemplate( );
          final Object errorChannelObject = e.getFailedMessage( ) != null ?
              e.getFailedMessage( ).getHeaders( ).get( MessageHeaders.ERROR_CHANNEL ) : null;
          if ( errorChannelObject instanceof MessageChannel ) {
            template.send( (MessageChannel) errorChannelObject, new ErrorMessage( e ) );
          } else {
            template.send( Objects.toString( errorChannelObject, "error-reply-queue" ), new ErrorMessage( e ) );
          }
        } else {
          final String msgType = msg.getClass( ).getSimpleName( );
          final Throwable throwable = Exceptions.trace( new ServiceDispatchException(
              "Failed to dispatch message " + msgType + " to service " + dest + " because: " + e.getMessage( ), e ) );
          errorFuture.completeExceptionally( throwable );
        }
      }
      if ( ctx != null ) {
        Threads.enqueue( Empyrean.class, ServiceContext.class, 8, new Callable<Boolean>( ) {
          private final long clearContextTime = System.currentTimeMillis( ) + TimeUnit.SECONDS.toMillis( CONTEXT_TIMEOUT );
          private final String contextCorrelationId = ctx.getCorrelationId( );

          @Override
          public Boolean call( ) {
            try {
              long sleepTime = clearContextTime - System.currentTimeMillis( );
              if ( sleepTime > 1 ) {
                Thread.sleep( sleepTime );
              }
              Contexts.clear( contextCorrelationId );
            } catch ( InterruptedException ex ) {
              Thread.currentThread( ).interrupt( );
            }
            return true;
          }
        } );
      }
    } ) );
    try {
      errorFuture.get( );
    } catch ( ExecutionException e ) {
      throw Exceptions.toException( e.getCause( ) );
    }
  }

  public static <T> CompletableFuture<T> send( final ComponentId dest, final BaseMessage msg ) {
    return send( getComponentExecutor( dest ), dest.getChannelName( ), msg );
  }

  public static <M,T> CompletableFuture<T> send(
      final String dest,
      final M msg
  ) {
    return send( getDispatchExecutor( ), dest, msg );
  }

  public static <M,T> CompletableFuture<T> send(
      final Executor executor,
      final String dest,
      final M msg
  ) {
    final CompletableFuture<T> completableFuture = future( );
    executor.execute( runWithContext( dest, msg, ctx -> {
      try {
        final MessagingTemplate template = getMessagingTemplate( );
        @SuppressWarnings( "unchecked" )
        final Message<T> response = (Message<T>) template.sendAndReceive( dest, new GenericMessage<>( msg ) );
        completableFuture.complete( response==null?null:response.getPayload( ) );
      } catch ( Exception e ) {
        completableFuture.completeExceptionally( Exceptions.trace(
            new ServiceDispatchException( "Failed to send message " + msg.getClass( ).getSimpleName( ) +
                " to service " + dest + " because: " + e.getMessage( ), e ) ) );
      }
    } ) );
    return completableFuture;
  }

  private static <T> CompletableFuture<T> future( ) {
    final CompletableFuture<T> future = new CompletableFuture<>( );
    FUTURE_QUEUE.offer( Pair.pair( System.currentTimeMillis( ), future ) );
    return future;
  }

  private static MessagingTemplate getMessagingTemplate( ) {
    final MessagingTemplate template = new MessagingTemplate( );
    template.setDestinationResolver(
        new BeanFactoryMessageChannelDestinationResolver( ServiceContextManager.getContext( ) ) );
    return template;
  }

  private static <M> Runnable runWithContext( final String dest, final M msg, final Consumer<Context> runnable ) {
    return () -> {
      Context ctx = null;
      if ( msg instanceof BaseMessage ) {
        BaseMessage baseMessage = (BaseMessage) msg;
        baseMessage.lookupAndSetCorrelationId( );
        ctx = Contexts.createWrapped( dest, baseMessage );
        try {
          Contexts.threadLocal( ctx == null ? Contexts.lookup( baseMessage.getCorrelationId( ) ) : ctx );
        } catch ( NoSuchContextException e ) {
          // no contextual context
        }
      }
      try {
        runnable.accept( ctx );
      } finally {
        Contexts.threadLocal( null );
        Contexts.clear( ctx );
      }
    };
  }

  private static Executor getComponentExecutor( ComponentId componentId ) {
    if ( COMMON_THREAD_POOL_SIZE > 0 ) {
      return getCommonExecutor( );
    } else {
      return Threads
          .lookup( componentId.getClass( ), ServiceContext.class, "component" )
          .limitTo( COMPONENT_THREAD_POOL_SIZE );
    }
  }

  private static Executor getDispatchExecutor( ) {
    if ( COMMON_THREAD_POOL_SIZE > 0 ) {
      return getCommonExecutor( );
    } else {
      return Threads.lookup( Empyrean.class, ServiceContext.class, "dispatch" ).limitTo( DISPATCH_THREAD_POOL_SIZE );
    }
  }

  private static Executor getCommonExecutor( ) {
    return Threads.lookup( Empyrean.class, ServiceContext.class, "common" ).limitTo( COMMON_THREAD_POOL_SIZE );
  }
}
