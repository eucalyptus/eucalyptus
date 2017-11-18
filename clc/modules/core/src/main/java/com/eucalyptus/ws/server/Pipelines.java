/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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

package com.eucalyptus.ws.server;

import static com.eucalyptus.auth.principal.TemporaryAccessKey.TemporaryKeyType;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.ws.Handlers;
import com.eucalyptus.ws.handlers.HmacHandler;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.handlers.SoapHandler;
import com.eucalyptus.ws.util.HmacUtils.SignatureVersion;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

/**
 * Utilities for discovering pipelines.
 */
public class Pipelines {
  private static final Logger                                                    LOG               = Logger.getLogger( Pipelines.class );
  private static final Set<FilteredPipeline>                                     internalPipelines = Sets.newHashSet( );
  private static final Set<FilteredPipeline>                                     pipelines         = Sets.newHashSet( );
  //GRZE:TODO: this is not happy ==> {@link DomainNames}
  private static final Supplier<String> subDomain = () -> SystemConfiguration.getSystemConfiguration( ).getDnsDomain( );



  /**
   * Finds and returns a pipeline that accepts the {@code request} by checking registered pipelines.
   *
   * @throws NoAcceptingPipelineException if no accepting pipeline for the {@code request} can be found
   */
  static FilteredPipeline find( final HttpRequest request ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    final FilteredPipeline candidate = findAccepting( request );
    if ( candidate == null ) {
      if ( Logs.isExtrrreeeme( ) ) {
        if ( request instanceof MappingHttpMessage ) {
          ( ( MappingHttpMessage ) request ).logMessage( );
          for ( final FilteredPipeline p : pipelines ) {
            LOG.debug( "PIPELINE: " + p );
          }
          for ( final FilteredPipeline p : internalPipelines ) {
            LOG.debug( "PIPELINE: " + p );
          }
        }
      }
      throw new NoAcceptingPipelineException( );
    }
    if ( Logs.isExtrrreeeme( ) ) {
      EventRecord.here( Pipelines.class, EventType.PIPELINE_UNROLL, candidate.toString( ) ).extreme( );
    }
    return candidate;
  }

  /**
   * Finds and returns a pipeline that accepts the {@code request} by checking registered pipelines.
   *
   * @return an accepting pipeline else {@code null}
   */
  private static FilteredPipeline findAccepting( final HttpRequest request ) {
    for ( final FilteredPipeline f : pipelines ) {
      if ( f.checkAccepts( request ) ) {
        return f;
      }
    }

    final String hostHeader = request.getHeader( HttpHeaders.Names.HOST );
    if ( hostHeader != null && ( hostHeader.contains( "amazonaws.com" ) || hostHeader.contains( subDomain.get( ) ) ) ) {
      final String host = hostHeader.indexOf( ':' ) > 0 ? hostHeader.substring( 0, hostHeader.indexOf( ':' ) ) : hostHeader;
      LOG.debug( "Trying to intercept request for " + hostHeader );
      for ( final FilteredPipeline f : pipelines ) {
        if ( Ats.from( f ).has( ComponentPart.class ) ) {
          Class<? extends ComponentId> compIdClass = Ats.from( f ).get( ComponentPart.class ).value( );
          ComponentId compId = ComponentIds.lookup( compIdClass );
          if ( Ats.from( compIdClass ).has( PublicService.class ) ) {
            if ( request.getHeaderNames().contains( "SOAPAction" ) && f.addHandlers( Channels.pipeline( ) ).get( SoapHandler.class ) == null ) {
              continue;//Skip pipeline which doesn't handle SOAP for this SOAP request
            } else if ( !request.getHeaderNames().contains( "SOAPAction" ) && f.addHandlers( Channels.pipeline( ) ).get( SoapHandler.class ) != null ) {
              continue;//Skip pipeline which handles SOAP for this non-SOAP request
            }
            LOG.debug( "Maybe intercepting: " + hostHeader + " using " + f.getClass( ) );
            if ( Ats.from( compIdClass ).has( AwsServiceName.class )
                && host.matches( "[\\w\\.-_]*" + compId.getAwsServiceName( ) + "(?:\\.[\\w\\-]+)?\\.amazonaws.com" ) ) {
              return f;//Return pipeline which can handle the request for ${service}.${region}.amazonaws.com
            } else if ( host.matches( "[\\w\\.-_]*" + compId.name( ) + "\\." + subDomain.get( ) ) ) {
              return f;//Return pipeline which can handle the request for ${service}.${system.dns.dnsdomain}
            }
          }
        }
      }
    }

    for ( final FilteredPipeline f : internalPipelines ) {
      if ( f.checkAccepts( request ) ) {
        return f;
      }
    }

    return null;
  }

  /**
   * Registers internal query and SOAP pipelines for all components.
   */
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.UnprivilegedConfiguration )
  public static class PipelineBootstrapper extends Bootstrapper.Simple {

    @Override
    public boolean load( ) throws Exception {
      for ( final ComponentId comp : ComponentIds.list( ) ) {
        Pipelines.internalPipelines.add( new InternalSoapPipeline( comp ) );
      }
      return true;
    }

  }

  /**
   * Discovers and registers FilteredPipeline instances for discovered components.
   */
  public static class PipelineDiscovery extends ServiceJarDiscovery {

    @SuppressWarnings( { "rawtypes", "unchecked", "synthetic-access" } )
    @Override
    public boolean processClass( final Class candidate ) throws Exception {
      if ( FilteredPipeline.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) )
           && !Modifier.isInterface( candidate.getModifiers( ) ) && Ats.from( candidate ).has( ComponentPart.class ) ) {
        try {
          final ComponentId compId = Ats.from( candidate ).get( ComponentPart.class ).value( ).newInstance( );
          final Class<? extends FilteredPipeline> pipelineClass = candidate;
          final FilteredPipeline pipeline = Classes.newInstance( pipelineClass );
          Pipelines.pipelines.add( pipeline );
          return true;
        } catch ( final Exception ex ) {
          LOG.trace( ex, ex );
          return false;
        }
      } else {
        return false;
      }
    }

    @Override
    public Double getPriority( ) {
      return 0.1d;
    }

  }

  private static class InternalSoapPipeline extends FilteredPipeline.InternalPipeline {
    private final String servicePath;
    private final String internalServicePath;
    private final String serviceName;

    public InternalSoapPipeline( final ComponentId componentId ) {
      super( componentId );
      this.servicePath = componentId.getServicePath( );
      this.internalServicePath = componentId.getInternalServicePath( );
      this.serviceName = componentId.getFullName( ).toString( );
    }

    @Override
    public boolean checkAccepts( final HttpRequest message ) {
      return ( message.getUri( ).endsWith( this.servicePath ) || message.getUri( ).endsWith( this.internalServicePath ) )
             && message.getHeaderNames( ).contains( "SOAPAction" );
    }

    @Override
    public String getName( ) {
      return "internal-soap-pipeline-" + this.serviceName.toLowerCase( ) + "-" + this.servicePath;
    }

    @Override
    public ChannelPipeline addHandlers( final ChannelPipeline pipeline ) {
      pipeline.addLast( "deserialize", Handlers.soapMarshalling( ) );
      pipeline.addLast( "security-detection-handler", new MessageStackHandler( ) {
        @Override
        public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
          boolean addWsSecurity = true;
          if ( event.getMessage( ) instanceof MappingHttpRequest ) {
            final MappingHttpRequest request = (MappingHttpRequest) event.getMessage( );
            addWsSecurity = !request.containsHeader( HttpHeaders.Names.AUTHORIZATION );
          }
          if ( addWsSecurity ) {
            ctx.getPipeline( ).addAfter( ctx.getName( ), "ws-security", Handlers.internalWsSecHandler( ) );
          } else {
            ctx.getPipeline( ).addAfter( ctx.getName( ), "timestamp-verify", Handlers.queryTimestamphandler() );
            ctx.getPipeline( ).addAfter( ctx.getName( ), "hmac-verify",
                new HmacHandler( EnumSet.of(TemporaryKeyType.Session,TemporaryKeyType.Access), EnumSet.of(SignatureVersion.SignatureV4) ) );
          }
          ctx.getPipeline( ).remove( this );
        }

        @Override
        public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
          ctx.getPipeline( ).addBefore( ctx.getName( ), "ws-security", Handlers.internalWsSecHandler( ) );
          ctx.getPipeline( ).remove( this );
        }
      } );
      pipeline.addLast( "ws-addressing", Handlers.addressingHandler( ) );
      pipeline.addLast( "build-soap-envelope", Handlers.soapHandler( ) );
      pipeline.addLast( "binding", Handlers.bindingHandler( ) );
      return pipeline;
    }

    @Override
    public String toString( ) {
      return String.format( "InternalSoapPipeline:servicePath=%s:serviceName=%s:toString()=%s", this.servicePath, this.serviceName, super.toString( ) );
    }
  }
}
