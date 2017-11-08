/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.records.Logs;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Filterable;
import com.eucalyptus.util.HasName;
import com.eucalyptus.util.dns.DomainNames;
import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public abstract class FilteredPipeline implements HasName<FilteredPipeline>, Filterable<HttpRequest> {
  private static Logger LOG = Logger.getLogger( FilteredPipeline.class );
  private static final Splitter hostSplitter = Splitter.on( ':' ).limit( 2 );
  private final Supplier<Set<Name>> nameSupplier =
      Suppliers.memoizeWithExpiration( new NamesSupplier( getClass() ), 15, TimeUnit.SECONDS );

  protected abstract static class InternalPipeline extends FilteredPipeline {
    private final ComponentId componentId;
    private final Supplier<Set<Name>> internalNameSupplier;

    InternalPipeline( ComponentId componentId ) {
      this.componentId = componentId;
      this.internalNameSupplier =
          Suppliers.memoizeWithExpiration( new NamesSupplier( componentId ), 15, TimeUnit.SECONDS );
    }
        
    private ComponentId getComponentId( ) {
      return this.componentId;
    }

    @Override
    protected Supplier<Set<Name>> getNameSupplier() {
      return internalNameSupplier;
    }
  }
  
  public FilteredPipeline( ) {}
  
  public abstract String getName( );
  
  public void unroll( final ChannelPipeline pipeline ) {
    try {
      this.addHandlers( pipeline );
      if ( Logs.isExtrrreeeme( ) ) {
        for ( final Map.Entry<String, ChannelHandler> e : pipeline.toMap( ).entrySet( ) ) {
          EventRecord.here( this.getClass( ), EventType.PIPELINE_HANDLER, e.getKey( ), e.getValue( ).getClass( ).getSimpleName( ) ).trace( );
        }
      }
    } catch ( final Exception e ) {
      LOG.error( "Error unrolling pipeline: " + this.getName( ) );
      final ChannelFuture close = pipeline.getChannel( ).close( );
      close.awaitUninterruptibly( );
      LOG.error( "Forced pipeline to close due to exception: ", e );
    }
  }
  
  public abstract ChannelPipeline addHandlers( ChannelPipeline pipeline );
  
  @Override
  public abstract boolean checkAccepts( HttpRequest message );
  
  @Override
  public final int compareTo( final FilteredPipeline o ) {
    return this.getName( ).compareTo( this.getName( ) );
  }

  protected boolean resolvesByHost( @Nullable final String host ) {
    boolean match = false;
    if ( host != null ) try {
      final Name hostName =
          Name.fromString( Iterables.getFirst( hostSplitter.split( host ), host ) );
      match = getNameSupplier( ).get( ).contains( DomainNames.absolute( hostName ) );
    } catch ( TextParseException e ) {
      Logs.exhaust( ).error( "Invalid host: " + host, e );
    }
    return match;
  }

  protected Supplier<Set<Name>> getNameSupplier( ) {
    return nameSupplier;
  }

  private static class NamesSupplier implements Supplier<Set<Name>> {
    private final Class<? extends ComponentId> componentIdClass;

    private NamesSupplier( final Class<?> componentClass ) {
      final ComponentPart part = Ats.inClassHierarchy( componentClass ).get( ComponentPart.class );
      this.componentIdClass = part == null ? null : part.value( );
    }

    private NamesSupplier( final ComponentId componentId ) {
      this.componentIdClass = componentId.getClass( );
    }

    @Override
    public Set<Name> get( ) {
      final Class<? extends ComponentId> component = componentIdClass;
      final Set<Name> names;
      if ( component != null ) {
        names = ImmutableSet.copyOf( Iterables.concat(
            DomainNames.internalSubdomains( component ),
            DomainNames.externalSubdomains( component )
        ) );
      } else {
        names = Collections.emptySet();
      }

      return names;
    }
  }


  @Override
  public final int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.getName( ) == null )
      ? 0
      : this.getName( ).hashCode( ) );
    return result;
  }
  
  @Override
  public final boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    FilteredPipeline other = ( FilteredPipeline ) obj;
    return this.compareTo( other ) == 0;
  }
  
  @Override
  public String toString( ) {
    return String.format( "FilteredPipeline:name=%s:hashCode=%s:class=%s", this.getName( ), this.hashCode( ), this.getClass( ).getSimpleName( ) );
  }
  
}
