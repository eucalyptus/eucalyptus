/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.eucalyptus.component.annotation.ServiceOperation;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Timers;
import com.eucalyptus.util.Wrapper;
import com.eucalyptus.util.Wrappers;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncExceptions.AsyncWebServiceError;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.dns.DomainNames;
import com.eucalyptus.ws.EucalytpusWebServiceStatusException;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.util.RequestQueue;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.common.net.InetAddresses;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;

@SuppressWarnings( "Guava" )
public class ServiceOperations {
  private static Logger                                                  LOG               = Logger.getLogger( ServiceOperations.class );
  private static final Map<Class<? extends BaseMessage>, Function<?, ?>> serviceOperations = Maps.newHashMap( );
  private static Boolean                                                 ASYNCHRONOUS      = Boolean.FALSE;                               //TODO:GRZE: @Configurable
  @SuppressWarnings( "unchecked" )
  public static <T extends BaseMessage, I, O> Function<I, O> lookup( final Class<T> msgType ) {
    return ( Function<I, O> ) serviceOperations.get( msgType );
  }

  public static boolean isUserOperation( final BaseMessage msg ) {
    return serviceOperations.containsKey( msg.getClass( ) ) ?
        Ats.from(
            Wrappers.unwrap( Function.class, serviceOperations.get( msg.getClass( ) ) )
        ).get( ServiceOperation.class ).user( ) :
        false;
  }


  public static class ServiceOperationDiscovery extends ServiceJarDiscovery {

    public ServiceOperationDiscovery( ) {
      super( );
    }

    @SuppressWarnings( { "synthetic-access", "unchecked" } )
    @Override
    public boolean processClass( final Class candidate ) throws Exception {
      if ( Ats.from( candidate ).has( ServiceOperation.class ) && Function.class.isAssignableFrom( candidate ) ) {
        final ServiceOperation opInfo = Ats.from( candidate ).get( ServiceOperation.class );
        final Function<? extends BaseMessage, ? extends BaseMessage> op =
            ( Function<? extends BaseMessage, ? extends BaseMessage> ) Classes.newInstance( candidate );
        final List<Class> msgTypes = Classes.genericsToClasses( op );
        LOG.info( "Registered @ServiceOperation:       " + msgTypes.get( 0 ).getSimpleName( )
                  + ","
                  + msgTypes.get( 1 ).getSimpleName( )
                  + " => "
                  + candidate );
        serviceOperations.put( msgTypes.get( 0 ), perhapsWrap( op ) );
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

  @SuppressWarnings( "unchecked" )
  public static <I extends BaseMessage, O extends BaseMessage> void dispatch( final I request ) {
    if ( !serviceOperations.containsKey( request.getClass( ) ) || !StackConfiguration.OOB_INTERNAL_OPERATIONS ) {
      try {
        ServiceContext.dispatch( RequestQueue.ENDPOINT, request );
      } catch ( Exception ex ) {
        Contexts.responseError( request.getCorrelationId( ), ex );
      }
    } else {
      try {
        final Context ctx = Contexts.lookup( request.getCorrelationId( ) );
        final Function<I, O> op = ( Function<I, O> ) serviceOperations.get( request.getClass( ) );
        Timers.loggingWrapper( new Callable( ) {
          @Override
          public Object call( ) throws Exception {
            if ( StackConfiguration.ASYNC_INTERNAL_OPERATIONS ) {
              Threads.enqueue( Empyrean.class, ServiceOperations.class, new Callable<Boolean>( ) {

                @Override
                public Boolean call( ) {
                  executeOperation( request, ctx, op );
                  return Boolean.TRUE;
                }
              } );
            } else {
              executeOperation( request, ctx, op );
            }
            return null;
          }

          @Override
          public String toString( ) {
            return "Service operation " + op.getClass( ).getSimpleName( );
          }

        } ).call( );
      } catch ( final Exception ex ) {
        Logs.extreme( ).error( ex, ex );
        Contexts.responseError( request.getCorrelationId( ), ex );
      }
    }
  }

  private static <I extends BaseMessage, O extends BaseMessage> void executeOperation( final I request, final Context ctx, final Function<I, O> op ) {
    Contexts.threadLocal( ctx );
    try {
      final O reply = op.apply( request );
      Contexts.response( reply );
    } catch ( final Exception ex ) {
      LOG.debug( ex );
      LOG.trace( ex, ex );
      Contexts.responseError( request.getCorrelationId( ), ex );
    } finally {
      Contexts.removeThreadLocal( );
    }
  }

  private static <F extends BaseMessage, T extends BaseMessage> Function<F, T> perhapsWrap( final Function<F, T> op ) {
    final ServiceOperation opInfo = Ats.from( op ).get( ServiceOperation.class );
    if ( opInfo.hostDispatch( ) ) {
      return new HostDispatchFunction<>( op );
    }
    return op;
  }

  private static <T extends BaseMessage> T send(
      final ServiceConfiguration configuration,
      final BaseMessage request
  ) throws Throwable {
    try {
      return AsyncRequests.sendSyncWithCurrentIdentity( configuration, request );
    } catch ( final Exception e ) {
      final FailedRequestException failedRequestException = Exceptions.findCause( e, FailedRequestException.class );
      if ( failedRequestException != null ) {
        if ( request.getReply( ).getClass( ).isInstance( failedRequestException.getRequest( ) ) ) {
          return failedRequestException.getRequest( ); // if it is a (failure) response then return it
        }
        throw e.getCause( ) == null ? e : e.getCause( );
      } else {
        final Optional<AsyncWebServiceError> errorOptional = AsyncExceptions.asWebServiceError( e );
        if ( errorOptional.isPresent( ) ) {
          final AsyncExceptions.AsyncWebServiceError serviceError = errorOptional.get( );
          throw new EucalytpusWebServiceStatusException(
              serviceError.getCode( ),
              serviceError.getHttpErrorCode( ),
              serviceError.getMessage( )
          );
        }
        throw e;
      }
    }
  }

  private static class HostDispatchFunction<F, T extends BaseMessage> implements Function<F, T>, Wrapper<Function<F, T>> {
    private final Function<F, T> op;

    private HostDispatchFunction( final Function<F, T> op ) {
      this.op = op;
    }

    @Override
    public Function<F, T> unwrap() {
      return op;
    }

    @Nullable
    @Override
    public T apply( @Nullable final F f ) {
      if ( f instanceof BaseMessage ) try {
        final BaseMessage baseMessage = (BaseMessage) f;
        final ComponentMessage componentMessage =
            Ats.inClassHierarchy( baseMessage.getClass( ) ).get( ComponentMessage.class );
        final ComponentId componentId = ComponentIds.lookup( componentMessage.value( ) );
        final Context context = Contexts.lookup( ( (BaseMessage) f ).getCorrelationId( ) );
        final MappingHttpRequest request = context.getHttpRequest( );
        final String hostHeader = request.getHeader( HttpHeaders.HOST );
        if ( hostHeader != null && componentId.isAlwaysLocal( ) ) {
          final String host = HostAndPort.fromString( hostHeader ).getHostText( );
          final Host coordinator = Hosts.getCoordinator( );
          if ( !InetAddresses.isInetAddress( host ) &&
              (coordinator == null || !coordinator.isLocalHost( )) &&
              DomainNames.isExternalSubdomain( Name.fromString( host, Name.root ) ) ) {
            if ( coordinator == null ) {
              throw Exceptions.toUndeclared( new ServiceStateException( "Service not available" ) );
            }
            try {
              final BaseMessage backendRequest = BaseMessages.deepCopy( baseMessage, baseMessage.getClass( ) );
              final BaseMessage backendResponse = send(
                  ServiceConfigurations.createEphemeral( componentId, coordinator.getBindAddress( ) ),
                  backendRequest );
              final T response = BaseMessages.deepCopy( backendResponse, (Class<T>)baseMessage.getReply( ).getClass( ) );
              response.setCorrelationId( request.getCorrelationId( ) );
              return response;
            } catch ( final Throwable e ) {
              throw Exceptions.toUndeclared( e );
            }
          }
        }
      } catch ( NoSuchContextException | IllegalArgumentException | TextParseException ignore ) {
      }
      return op.apply( f );
    }
  }
}
