/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.server;

import java.lang.reflect.Modifier;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ComponentPart;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Classes;
import com.eucalyptus.ws.handlers.BindingHandler;
import com.eucalyptus.ws.handlers.HmacHandler;
import com.eucalyptus.ws.handlers.InternalWsSecHandler;
import com.eucalyptus.ws.handlers.QueryTimestampHandler;
import com.eucalyptus.ws.handlers.SoapMarshallingHandler;
import com.eucalyptus.ws.protocol.AddressingHandler;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.protocol.SoapHandler;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Pipelines {
  private static final Logger                                  LOG                = Logger.getLogger( Pipelines.class );
  private static final Multimap<ComponentId, FilteredPipeline> componentPipelines = TreeMultimap.create( );
  private static final Set<FilteredPipeline>                   pipelines          = new ConcurrentSkipListSet<FilteredPipeline>( );
  
  static FilteredPipeline find( final HttpRequest request ) throws DuplicatePipelineException, NoAcceptingPipelineException {
    FilteredPipeline candidate = findAccepting( request );
    if ( candidate == null ) {
      if ( Logs.isExtrrreeeme( ) ) {
        if ( request instanceof MappingHttpMessage ) {
          ( ( MappingHttpMessage ) request ).logMessage( );
          for ( FilteredPipeline p : pipelines ) {
            LOG.debug( "PIPELINE: " + p );
          }
          for ( FilteredPipeline p : componentPipelines.values( ) ) {
            LOG.debug( "PIPELINE: " + p );
          }
        }
      }
      throw new NoAcceptingPipelineException( );
    }
    if ( Logs.isExtrrreeeme( ) ) {
      EventRecord.here( Pipelines.class, EventType.PIPELINE_UNROLL, candidate.toString( ) ).debug( );
    }
    return candidate;
  }
  
  private static FilteredPipeline findAccepting( final HttpRequest request ) {
    FilteredPipeline candidate = null;
    for ( FilteredPipeline f : componentPipelines.values( ) ) {
      if ( f.checkAccepts( request ) ) {
        return f;
      }
    }
    if ( candidate == null ) {
      for ( FilteredPipeline f : pipelines ) {
        return f;
      }
    }
    return candidate;
  }
  
  public static void enable( ComponentId compId ) {
    LOG.info( "-> Registering component pipeline: " + compId.getName( ) + " " + componentPipelines.get( compId ) );
  }
  
  public static void disable( ComponentId compId ) {
    LOG.info( "-> Deregistering component pipeline: " + compId.getName( ) + " " + componentPipelines.get( compId ) );
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.UnprivilegedConfiguration )
  public static class PipelineBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      for ( ComponentId comp : ComponentIds.list( ) ) {
        Pipelines.pipelines.add( new InternalQueryPipeline( comp ) );
        Pipelines.pipelines.add( new InternalSoapPipeline( comp ) );
      }
      return true;
    }
    
  }
  
  public static class PipelineDiscovery extends ServiceJarDiscovery {
    
    @SuppressWarnings( { "rawtypes", "unchecked", "synthetic-access" } )
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( FilteredPipeline.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) )
           && !Modifier.isInterface( candidate.getModifiers( ) ) && Ats.from( candidate ).has( ComponentPart.class ) ) {
        try {
          ComponentId compId = ( ComponentId ) Ats.from( candidate ).get( ComponentPart.class ).value( ).newInstance( );
          Class<? extends FilteredPipeline> pipelineClass = candidate;
          FilteredPipeline pipeline = Classes.newInstance( pipelineClass );
          Pipelines.componentPipelines.put( compId, pipeline );
          return true;
        } catch ( Exception ex ) {
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
    private String servicePath;
    private String serviceName;
    
    public InternalSoapPipeline( ComponentId componentId ) {
      super( componentId );
      this.servicePath = componentId.getLocalEndpointUri( ).getPath( );
      this.serviceName = componentId.getLocalEndpointName( );
    }
    
    @Override
    public boolean checkAccepts( HttpRequest message ) {
      return message.getUri( ).endsWith( servicePath ) && message.getHeaderNames( ).contains( "SOAPAction" );
    }
    
    @Override
    public String getName( ) {
      return "internal-soap-pipeline-" + this.serviceName.toLowerCase( ) + "-" + this.servicePath;
    }
    
    @Override
    public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
      pipeline.addLast( "deserialize", new SoapMarshallingHandler( ) );
      try {
        pipeline.addLast( "ws-security", new InternalWsSecHandler( ) );
      } catch ( GeneralSecurityException e ) {
        LOG.error( e, e );
      }
      pipeline.addLast( "ws-addressing", new AddressingHandler( ) );
      pipeline.addLast( "build-soap-envelope", new SoapHandler( ) );
      pipeline.addLast( "binding", new BindingHandler( ) );
      return pipeline;
    }
    
    @Override
    public String toString( ) {
      return String.format( "InternalSoapPipeline:servicePath=%s:serviceName=%s:toString()=%s", this.servicePath, this.serviceName, super.toString( ) );
    }
    
  }
  
  private static class InternalQueryPipeline extends FilteredPipeline.InternalPipeline {
    public enum RequiredQueryParams {
      SignatureVersion,
      Version
    }
    
    private String servicePath;
    private String serviceName;
    
    public InternalQueryPipeline( ComponentId componentId ) {
      super( componentId );
      this.servicePath = componentId.getLocalEndpointUri( ).getPath( );
      this.serviceName = componentId.getLocalEndpointName( );
    }
    
    @Override
    public boolean checkAccepts( HttpRequest message ) {
      if ( message instanceof MappingHttpRequest ) {
        MappingHttpRequest httpRequest = ( MappingHttpRequest ) message;
        if ( httpRequest.getMethod( ).equals( HttpMethod.POST ) ) {
          Map<String, String> parameters = new HashMap<String, String>( httpRequest.getParameters( ) );
          ChannelBuffer buffer = httpRequest.getContent( );
          buffer.markReaderIndex( );
          byte[] read = new byte[buffer.readableBytes( )];
          buffer.readBytes( read );
          String query = new String( read );
          buffer.resetReaderIndex( );
          for ( String p : query.split( "&" ) ) {
            String[] splitParam = p.split( "=" );
            String lhs = splitParam[0];
            String rhs = splitParam.length == 2
              ? splitParam[1]
              : null;
            try {
              if ( lhs != null ) lhs = new URLCodec( ).decode( lhs );
            } catch ( DecoderException e ) {}
            try {
              if ( rhs != null ) rhs = new URLCodec( ).decode( rhs );
            } catch ( DecoderException e ) {}
            parameters.put( lhs, rhs );
          }
          for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
            if ( !parameters.containsKey( p.toString( ) ) ) {
              return false;
            }
          }
          httpRequest.getParameters( ).putAll( parameters );
        } else {
          for ( RequiredQueryParams p : RequiredQueryParams.values( ) ) {
            if ( !httpRequest.getParameters( ).containsKey( p.toString( ) ) ) {
              return false;
            }
          }
        }
        return true && message.getUri( ).startsWith( servicePath );
      }
      return false;
    }
    
    @Override
    public String getName( ) {
      return "internal-query-pipeline-" + this.serviceName.toLowerCase( ) + "-" + this.servicePath;
    }
    
    @Override
    public ChannelPipeline addHandlers( ChannelPipeline pipeline ) {
      pipeline.addLast( "hmac-v2-verify", new HmacHandler( true ) );
      pipeline.addLast( "timestamp-verify", new QueryTimestampHandler( ) );
      pipeline.addLast( "restful-binding", new InternalQueryBinding( ) );
      return pipeline;
    }
    
    @Override
    public String toString( ) {
      return String.format( "InternalQueryPipeline:servicePath=%s:serviceName=%s:toString()=%s", this.servicePath, this.serviceName, super.toString( ) );
    }
    
  }
  
  static class InternalQueryBinding extends BaseQueryBinding<OperationParameter> implements ChannelHandler {
    
    public InternalQueryBinding( ) {
      super( "http://ec2.amazonaws.com/doc/%s/", "2009-04-04", OperationParameter.Action, OperationParameter.Operation );
    }
    
  }
  
  enum InternalOnlyHandler implements ChannelUpstreamHandler {
    INSTANCE;
    @Override
    public void handleUpstream( ChannelHandlerContext ctx, ChannelEvent e ) throws Exception {
      final MappingHttpMessage request = MappingHttpMessage.extractMessage( e );
      final BaseMessage msg = BaseMessage.extractMessage( e );
      if ( request != null && msg != null ) {
        final User user = Contexts.lookup( request.getCorrelationId( ) ).getUser( );
        if ( user.isSystemInternal( ) || user.isSystemAdmin( ) ) {
          ctx.sendUpstream( e );
        } else {
          Contexts.clear( Contexts.lookup( msg.getCorrelationId( ) ) );
          ctx.getChannel( ).write( new MappingHttpResponse( request.getProtocolVersion( ), HttpResponseStatus.FORBIDDEN ) );
        }
      } else {
        ctx.sendUpstream( e );
      }
    }
    
  }
  
}
