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

package com.eucalyptus.bootstrap;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Logger;
import org.jgroups.Address;
import com.eucalyptus.component.Topology;
import com.eucalyptus.util.Internets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

public class Host implements Serializable, Comparable<Host> {
  private static final long                serialVersionUID = 1;
  private static Logger                    LOG              = Logger.getLogger( Host.class );
  private final String                     displayName;
  private final Address                    groupsId;
  private final InetAddress                bindAddress;
  private final ImmutableList<InetAddress> hostAddresses;
  private final Boolean                    hasDatabase;
  private final Boolean                    hasBootstrapped;
  private final AtomicLong                 timestamp        = new AtomicLong( System.currentTimeMillis( ) );
  private final Long                       startedTime;
  private final Integer                    epoch;

  Host( final String displayName,
        final Address groupsId,
        final InetAddress bindAddress,
        final ImmutableList<InetAddress> hostAddresses,
        final Boolean hasDatabase,
        final Boolean hasBootstrapped,
        final Long startedTime,
        final Integer epoch  ) {
    this.displayName = displayName;
    this.groupsId = groupsId;
    this.bindAddress = bindAddress;
    this.hostAddresses = hostAddresses;
    this.hasDatabase = hasDatabase;
    this.hasBootstrapped = hasBootstrapped;
    this.startedTime = startedTime;
    this.epoch = epoch;
  }

  Host( ) {
    this(
        Internets.localHostIdentifier( ),
        Hosts.getLocalGroupAddress( ),
        Internets.localHostInetAddress( ),
        ImmutableList.copyOf( Ordering.from( Internets.INET_ADDRESS_COMPARATOR ).sortedCopy( Internets.getAllInetAddresses( ) ) ),
        BootstrapArgs.isCloudController( ),
        Bootstrap.isFinished( ),
        Hosts.getStartTime( ),
        Topology.epoch( )
    );
  }
  
  public static Host create( ) {
    return new Host( );
  }
  
  public Address getGroupsId( ) {
    return this.groupsId;
  }
  
  public ImmutableList<InetAddress> getHostAddresses( ) {
    return this.hostAddresses;
  }

  public InetAddress getBindAddress( ) {
    return this.bindAddress;
  }
  
  public Boolean hasDatabase( ) {
    return this.hasDatabase;
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.displayName == null ) ? 0 : this.displayName.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( obj == null ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    Host other = ( Host ) obj;
    if ( this.displayName == null ) {
      if ( other.displayName != null ) {
        return false;
      }
    } else if ( !this.displayName.equals( other.displayName ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo( Host that ) {
    return this.getDisplayName( ).compareTo( that.getDisplayName() );
  }
  
  public boolean isLocalHost( ) {
    return Internets.testLocal( this.getBindAddress( ) );
  }
  
  public Boolean hasBootstrapped( ) {
    return this.hasBootstrapped;
  }
  
  public Integer getEpoch( ) {
    return this.epoch;
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "Host " )
           .append( this.groupsId ).append( " " )
           .append( "#" ).append( this.epoch ).append( " " )
           .append( this.bindAddress ).append( " " );
    try {
      Host coordinator = Hosts.getCoordinator( );
      String coordinatorName = ( coordinator != null ? coordinator.getDisplayName( ) : "pending" );
      builder.append( "coordinator=" ).append( coordinatorName ).append( " " );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
    builder.append( this.hasBootstrapped ? "booted " : "booting " )
           .append( this.hasDatabase ? "db" : "nodb" );
    builder.append( " started=" ).append( this.startedTime ).append( " " )
           .append( this.hostAddresses );
    return builder.toString( );
  }
  
  public Date getTimestamp( ) {
    return new Date( this.timestamp.get( ) );
  }
  
  public String getDisplayName( ) {
    return this.displayName;
  }
  
  public Long getStartedTime( ) {
    return this.startedTime;
  }
  
}
