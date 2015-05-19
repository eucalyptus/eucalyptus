/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.compute.common.internal.network;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import com.eucalyptus.compute.common.internal.util.Reference;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AccountMetadata;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.util.HasFullName;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_extant_network" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ExtantNetwork extends UserMetadata<Reference.State> {
  private static final long                     serialVersionUID = 1L;
  private static final Logger                   LOG              = Logger.getLogger( ExtantNetwork.class );

  @Column( name = "metadata_extant_network_tag", unique = true )
  private Integer                        tag;
  
  @NotFound( action = NotFoundAction.IGNORE )
  @OneToMany( fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL, mappedBy = "extantNetwork" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<PrivateNetworkIndex> indexes           = new HashSet<>( );
  
  @OneToOne( fetch = FetchType.EAGER )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private NetworkGroup                   networkGroup;
  
  @SuppressWarnings( "unused" )
  private ExtantNetwork( ) {
    super( );
  }
  
  private ExtantNetwork( final Integer tag ) {
    super( null, null );
    this.tag = tag;
  }
  
  private ExtantNetwork( final NetworkGroup networkGroup, final Integer tag ) {
    super( networkGroup.getOwner( ), networkGroup.getDisplayName( ) );
    this.tag = tag;
    this.networkGroup = networkGroup;
    this.setState( Reference.State.EXTANT );
  }
  
  private ExtantNetwork( final NetworkGroup networkGroup ) {
    super( networkGroup.getOwner( ), networkGroup.getDisplayName( ) );
  }
  
  public static ExtantNetwork named( final Integer tag ) {
    return new ExtantNetwork( tag );
  }
  
  public static ExtantNetwork named( final NetworkGroup networkGroup ) {
    return new ExtantNetwork( networkGroup );
  }
  
  public static ExtantNetwork create( final NetworkGroup networkGroup, final Integer tag ) {
    return new ExtantNetwork( networkGroup, tag );
  }
  
  public Integer getTag( ) {
    return this.tag;
  }
  
  protected void setTag( final Integer tag ) {
    this.tag = tag;
  }

  public PrivateNetworkIndex reclaimNetworkIndex( final Long idx ) throws Exception {
    try {
      return Entities.uniqueResult( PrivateNetworkIndex.named( this, idx ) );
    } catch ( final Exception ex ) {
      return Entities.persist( PrivateNetworkIndex.create( this, idx ) ).allocate( );
    }
  }

  public boolean inUse( ) {
    return !indexes.isEmpty( ) || lastUpdateMillis( ) < TimeUnit.MINUTES.toMillis( 1 );
  }

  public NetworkGroup getNetworkGroup( ) {
    return this.networkGroup;
  }
  
  void setNetworkGroup( final NetworkGroup networkGroup ) {
    this.networkGroup = networkGroup;
  }

  public Set<PrivateNetworkIndex> getIndexes() {
    return indexes;
  }

  public void setIndexes( final Set<PrivateNetworkIndex> indexes ) {
    this.indexes = indexes;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.tag == null )
      ? 0
      : this.tag.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( final Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( !super.equals( obj ) ) {
      return false;
    }
    if ( this.getClass( ) != obj.getClass( ) ) {
      return false;
    }
    final ExtantNetwork other = ( ExtantNetwork ) obj;
    if ( this.tag == null ) {
      if ( other.tag != null ) {
        return false;
      }
    } else if ( !this.tag.equals( other.tag ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo( final AccountMetadata that ) {
    if ( that instanceof ExtantNetwork ) {
      return this.getTag( ).compareTo( ( ( ExtantNetwork ) that ).getTag( ) );
    } else {
      return super.compareTo( that );
    }
  }
  
  @Override
  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( "ExtantNetwork:" );
    if ( this.networkGroup != null ) builder.append( this.networkGroup.getDisplayName( ) ).append( ":" );
    if ( this.tag != null ) builder.append( "tag=" ).append( this.tag ).append( ":" );
    if ( this.indexes != null ) builder.append( "indexes=" ).append( this.indexes ).append( ":" );
    return builder.toString( );
  }
  
  /**
   * @see HasFullName#getPartition()
   */
  @Override
  public String getPartition( ) {
    return Eucalyptus.INSTANCE.getName( );
  }
  
  /**
   * @see HasFullName#getFullName()
   */
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
                          .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
                          .namespace( this.getOwnerAccountNumber( ) )
                          .relativeId( "security-group", this.getNetworkGroup( ).getDisplayName( ),
                                       "tag", this.getTag( ).toString( ) );
  }

  void remove( final PrivateNetworkIndex index ) {
    // Removal works around hibernate issue
    // https://hibernate.atlassian.net/browse/HHH-3799
    int sizeBefore = indexes.size( );
    if ( Iterables.removeIf( indexes, Predicates.equalTo( index ) ) && sizeBefore == indexes.size( ) ) {
      List<PrivateNetworkIndex> temp = Lists.newArrayList( indexes );
      indexes.clear( );
      indexes.addAll( temp );
      indexes.remove( index );
    }
  }

  public boolean teardown( ) {
    if ( !this.indexes.isEmpty( ) ) {
      for ( PrivateNetworkIndex index : this.indexes ) {
        switch ( index.getState( ) ) {
          case PENDING:
            LOG.warn( "Failing teardown of extant network " + this + ": Found pending index " + index + "." );
            return false;
          case EXTANT:
            LOG.warn( "Failing teardown of extant network " + this + ": Found extant index " + index + "." );
            return false;
          case UNKNOWN:
          case FREE:
          case RELEASING:
            this.indexes.remove( index );
            try {
              index.release( );
              index.teardown( );
            } catch ( ResourceAllocationException ex ) {
              LOG.error( ex, ex );
            }
            break;
        }
      }
    }
    this.indexes.clear( );
    return true;
  }

}
