/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.network;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.transaction.Synchronization;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import com.eucalyptus.cloud.util.Reference;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AccountMetadata;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.entities.TransientEntityException;
import com.eucalyptus.entities.UserMetadata;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.util.Numbers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_extant_network" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ExtantNetwork extends UserMetadata<Reference.State> {
  private static final long                     serialVersionUID = 1L;
  private static final Logger                   LOG              = Logger.getLogger( ExtantNetwork.class );
  private static final ConcurrentMap<NetworkIndexKey,Long> inFlightNetworkIndexes = Maps.newConcurrentMap();


  @Column( name = "metadata_extant_network_tag", unique = true )
  private Integer                        tag;
  
  @NotFound( action = NotFoundAction.IGNORE )
  @OneToMany( fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL )
  @JoinColumn( name = "metadata_extant_network_index_fk" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private final Set<PrivateNetworkIndex> indexes          = new HashSet<PrivateNetworkIndex>( );
  
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
    this.setState( Reference.State.PENDING );
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
  
  private static AtomicInteger bogusTag = new AtomicInteger(0);
  public static ExtantNetwork bogus( final NetworkGroup networkGroup ) {
	  for(;;){
		final EntityTransaction db = Entities.get( ExtantNetwork.class );
	    try {
	      final ExtantNetwork net = Entities.uniqueResult( new ExtantNetwork(bogusTag.decrementAndGet()) );
	      db.rollback();
	    } catch ( final Exception ex ) {	
	    	db.rollback();
	    	return new ExtantNetwork( networkGroup, bogusTag.get());
	    }    
	  }
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

  PrivateNetworkIndex allocateNetworkIndex( ) throws TransactionException {
    if ( !Entities.isPersistent( this ) ) {
      throw new TransientEntityException( this.toString( ) );
    } else {
      try ( final TransactionResource db = Entities.transactionFor( PrivateNetworkIndex.class ) ){
        final List<Long> networkIndexHolder = Lists.newArrayList();
        Entities.registerSynchronization( ExtantNetwork.class, new Synchronization() {
          @Override public void beforeCompletion() { }
          @Override public void afterCompletion( final int status ) {
            clearInFlight( networkIndexHolder );
          }
        } );
        for ( final Long i : Numbers.shuffled( NetworkGroups.networkIndexInterval() ) ) {
          try {
            Entities.uniqueResult( PrivateNetworkIndex.named( this, i ) );
          } catch ( final NoSuchElementException ex ) {
            if ( ifNotInFlight( i ) ) try {
              networkIndexHolder.add( i );
              PrivateNetworkIndex netIdx = Entities.persist( PrivateNetworkIndex.create( this, i ) );
              PrivateNetworkIndex ref = netIdx.allocate( );
              db.commit( );
              return ref;
            } catch ( final Exception ex1 ) {
              Logs.exhaust().debug( ex1 );
            }
          }
        }
        throw new NoSuchElementException( );
      } catch ( Exception ex ) {
        Logs.exhaust( ).error( ex, ex );
        throw new TransactionExecutionException(
          "Too many instances running in security group '"+this.displayName+"', unable to allocate a private network " +
          "index. Please either reduce the number of instances in the security group or use another security group.",
          ex );
      }
    }
  }

  private void clearInFlight( final Collection<Long> indexes ) {
    for ( final Long index: indexes ) {
      final NetworkIndexKey key = new NetworkIndexKey( getTag(), index );
      inFlightNetworkIndexes.remove( key );
    }
  }

  private boolean ifNotInFlight( final Long index ) {
    final NetworkIndexKey key = new NetworkIndexKey( getTag(), index );
    final Long reservedTime = inFlightNetworkIndexes.putIfAbsent( key, System.currentTimeMillis() );
    return
        reservedTime == null ||
            ( reservedTime + TimeUnit.MINUTES.toMillis( 1 ) < System.currentTimeMillis() &&
              inFlightNetworkIndexes.replace( key, reservedTime, System.currentTimeMillis() ) );
  }

  public NetworkGroup getNetworkGroup( ) {
    return this.networkGroup;
  }
  
  void setNetworkGroup( final NetworkGroup networkGroup ) {
    this.networkGroup = networkGroup;
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
  
  boolean teardown( ) {
    if ( !this.indexes.isEmpty( ) ) {
      for ( PrivateNetworkIndex index : this.indexes ) {
        switch ( index.getState( ) ) {
          case PENDING:
            if ( index.lastUpdateMillis( ) < TimeUnit.MINUTES.toMillis( NetworkGroups.NETWORK_INDEX_PENDING_TIMEOUT ) ) {
              LOG.warn( "Failing teardown of extant network " + this + ": Found pending index " + index + " which is within the timeout window." );
              return false;
            } else {
              this.indexes.remove( index );
              try {
                index.release( );
                index.teardown( );
              } catch ( ResourceAllocationException ex ) {
                LOG.error( ex, ex );
              }
              break;
            }
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

  private static final class NetworkIndexKey {
    private Integer tag;
    private Long index;

    private NetworkIndexKey( final Integer tag,
                             final Long index ) {
      this.tag = tag;
      this.index = index;
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final NetworkIndexKey that = (NetworkIndexKey) o;

      if ( index != null ? !index.equals( that.index ) : that.index != null ) return false;
      if ( tag != null ? !tag.equals( that.tag ) : that.tag != null ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = tag != null ? tag.hashCode() : 0;
      result = 31 * result + (index != null ? index.hashCode() : 0);
      return result;
    }
  }

}
