/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.cluster.common;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import com.eucalyptus.cluster.common.provider.ClusterProvider;
import com.eucalyptus.cluster.common.vm.VmStateUpdate;
import com.eucalyptus.cluster.common.msgs.NodeType;
import com.eucalyptus.component.*;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.compute.common.CloudMetadata.AvailabilityZoneMetadata;
import com.eucalyptus.context.ServiceStateException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMappers;
import com.google.common.collect.ForwardingMap;

import com.eucalyptus.cluster.common.msgs.NodeInfo;

public class Cluster implements AvailabilityZoneMetadata, HasFullName<Cluster> {
  private static Logger                                  LOG            = Logger.getLogger( Cluster.class );
  private static final AtomicReference<Consumer<VmStateUpdate>> vmInfoUpdateConsumer = new AtomicReference<>( info -> { } );

  private final ClusterProvider clusterProvider;
  private final ServiceConfiguration                     configuration;
  private final ReadWriteLock                            gateLock       = new ReentrantReadWriteLock( );
  private final ResourceState                            nodeState;
  private final ConcurrentNavigableMap<String, NodeInfo> nodeMap;
  private final Map<String, NodeInfo>                    nodeHostAddrMap = new ForwardingMap<String, NodeInfo>( ) {
    
    @Override
    protected Map<String, NodeInfo> delegate( ) {
      return Cluster.this.nodeMap;
    }
    
    @Override
    public boolean containsKey( Object keyObject ) {
      return delegate( ).containsKey( findRealKey( keyObject ) );
    }
    
    @Override
    public NodeInfo get( Object key ) {
      return delegate( ).get( findRealKey( key ) );
    }
    
    public String findRealKey( Object keyObject ) {
      if ( keyObject instanceof String ) {
        String key = ( String ) keyObject;
        for ( String serviceTag : delegate( ).keySet( ) ) {
          try {
            URI tag = new URI( serviceTag );
            String host = tag.getHost( );
            if ( host != null && host.equals( key ) ) {
              return serviceTag;
            } else {
              InetAddress addr = InetAddress.getByName( host );
              String hostAddr = addr.getHostAddress( );
              if ( hostAddr != null && hostAddr.equals( key ) ) {
                return serviceTag;
              }
            }
          } catch ( UnknownHostException ex ) {
            LOG.debug( ex );
          } catch ( URISyntaxException ex ) {
            LOG.debug( ex );
          }
        }
        return key;
      } else {
        return "" + keyObject;
      }
    }
    
  };

  public Cluster( final ClusterProvider clusterProvider ) {
    this( clusterProvider, clusterProvider.getConfiguration( ) );
  }

  public Cluster( final ServiceConfiguration configuration ) {
    this( TypeMappers.transform( configuration, ClusterProvider.class ), configuration );
  }

  protected Cluster( final ClusterProvider clusterProvider, final ServiceConfiguration configuration ) {
    this.clusterProvider = clusterProvider;
    this.configuration = configuration;
    this.nodeMap = new ConcurrentSkipListMap<>( );
    this.nodeState = new ResourceState( clusterProvider.getName( ) );
    clusterProvider.init( this );
  }

  @Deprecated
  public ClusterProvider getClusterProvider( ) {
    return clusterProvider;
  }

  @Override
  public String getName( ) {
    return this.configuration.getName();
  }
  
  public NavigableSet<String> getNodeTags( ) {
    return this.nodeMap.navigableKeySet();
  }
  
  public NodeInfo getNode( final String serviceTag ) {
    if ( this.nodeMap.containsKey( serviceTag ) ) {
    return this.nodeMap.get( serviceTag );
    } else {
      try {
        URI tag = new URI( serviceTag );
        String host = tag.getHost( );
        InetAddress addr = InetAddress.getByName( host );
        String hostAddr = addr.getHostAddress( );
        String altTag = serviceTag.replace( host, hostAddr );
        if ( this.nodeMap.containsKey( altTag ) ) {
          return this.nodeMap.get( altTag );
        } else {
          return null;//TODO:GRZE: sigh.
        }
      } catch ( Exception ex ) {
        return null;//TODO:GRZE: sigh.
      }
      
    }
  }
  
  @Override
  public int compareTo( final Cluster that ) {
    return this.getName( ).compareTo( that.getName( ) );
  }
  
  public ServiceConfiguration getConfiguration( ) {
    return this.configuration;
  }
  
  public ResourceState getNodeState( ) {
    return this.nodeState;
  }
  
  public void start( ) throws ServiceRegistrationException {
    try {
      ClusterRegistry.getInstance( ).registerDisabled( this );
      clusterProvider.start( );
    } catch ( final NoSuchElementException ex ) {
      Logs.extreme().debug(ex, ex);
      throw ex;
    } catch ( final Exception ex ) {
      Logs.extreme( ).debug( ex, ex );
      throw new ServiceRegistrationException( "Failed to call start() on cluster " + this.configuration
                                              + " because of: "
                                              + ex.getMessage( ), ex );
    }
  }
  
  public void enable( ) throws ServiceRegistrationException {
    clusterProvider.enable( );
  }
  
  public void disable( ) throws ServiceRegistrationException {
    try {
      clusterProvider.disable();
    } finally {
      try {
        ClusterRegistry.getInstance( ).disable(this.getName());
      } catch ( Exception ex ) {}
    }
  }
  
  public void stop( ) throws ServiceRegistrationException {
    try {
      clusterProvider.stop();
    } finally {
      ClusterRegistry.getInstance( ).deregister( this.getName( ) );
    }
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result
             + ( ( this.configuration == null )
                                               ? 0
                                               : this.configuration.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( this.getClass( ) != obj.getClass( ) ) {
      return false;
    }
    final Cluster other = ( Cluster ) obj;
    if ( this.configuration == null ) {
      if ( other.configuration != null ) {
        return false;
      }
    } else if ( !this.configuration.equals( other.configuration ) ) {
      return false;
    }
    return true;
  }
  
  public String getHostName( ) {
    return clusterProvider.getHostName( );
  }
  
  public Integer getPort( ) {
    return this.configuration.getPort();
  }
  
  @Override
  public String toString( ) {
    final StringBuilder buf = new StringBuilder( 512 );
    buf.append( "Cluster " ).append( this.configuration ).append( '\n' );
    buf.append( "Cluster " ).append( this.configuration.getName( ) );
    for ( final NodeInfo node : this.nodeMap.values( ) ) {
      buf.append( "Cluster " ).append( this.configuration.getName( ) ).append( " node=" ).append( node ).append( '\n' );
    }
    for ( final ResourceState.VmTypeAvailability avail : this.nodeState.getAvailabilities( ) ) {
      buf.append( "Cluster " ).append( this.configuration.getName( ) ).append( " node=" ).append( avail ).append( '\n' );
    }
    return buf.toString( );
  }
  
  public void refreshResources( ) {
    try {
      clusterProvider.refreshResources( );
    } catch ( Exception ex ) {
      LOG.error( ex );
      LOG.debug(  ex, ex );
    }
  }
  
  public void check( ) throws Faults.CheckException, IllegalStateException, InterruptedException, ServiceStateException {
    if ( this.gateLock.readLock( ).tryLock( 60, TimeUnit.SECONDS ) ) {
      try {    	
        clusterProvider.check( );
      } finally {
        //#6 Unmark this cluster as gated.
        this.gateLock.readLock( ).unlock( );
      }
    } else {
      throw new ServiceStateException( "Failed to check state in the zone " + this.getPartition( ) + ", it is currently locked for maintenance." );
    }
  }
  
  @Override
  public String getPartition( ) {
    return this.clusterProvider.getPartition( );
  }
  
  public Partition lookupPartition( ) {
    return clusterProvider.lookupPartition( );
  }
  
  @Override
  public FullName getFullName( ) {
    return this.configuration.getFullName();
  }
  
  @Override
  public String getDisplayName( ) {
    return this.getPartition( );
  }
  
  @Override
  public OwnerFullName getOwner( ) {
    return Principals.systemFullName( );
  }

  public ConcurrentNavigableMap<String, NodeInfo> getNodeMap( ) {
    return this.nodeMap;
  }
  
  /**
   * GRZE:WARNING: this is a temporary method to expose the forwarding map of NC info
   * @return
   */
  public Map<String,NodeInfo> getNodeHostMap( ) {
    return this.nodeHostAddrMap;
  }
  
  public ReadWriteLock getGateLock( ) {
    return this.gateLock;
  }

  public void updateVmInfo( VmStateUpdate update ) {
    vmInfoUpdateConsumer.get( ).accept( update );
  }

  public void updateNodeInfo( final long time, final ArrayList<NodeType> nodes ) {
    clusterProvider.updateNodeInfo( time, nodes );
  }

  public boolean hasNode( final String sourceHost ) {
    return clusterProvider.hasNode( sourceHost );
  }

  public void cleanup( final Exception ex ) {
    clusterProvider.cleanup( this, ex );
  }

  public static void registerVmStateUpdateConsumer( final Consumer<VmStateUpdate> consumer ) {
    if ( consumer != null ) {
      vmInfoUpdateConsumer.set( consumer );
    }
  }
}
