/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.network;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.EntityTransaction;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import com.eucalyptus.cloud.util.NotEnoughResourcesException;
import com.eucalyptus.cloud.util.PersistentResource;
import com.eucalyptus.cloud.util.Resource;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.cloud.util.Resource.SetReference;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.RecoverablePersistenceException;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Numbers;
import com.google.common.base.Predicate;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_indices" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PrivateNetworkIndex extends PersistentResource<PrivateNetworkIndex, VmInstance> {
  @Transient
  private static final PrivateNetworkIndex bogusIndex = new PrivateNetworkIndex( -1, -1l );
  @Column( name = "metadata_network_index" )
  private final Long                       index;
  @Column( name = "metadata_network_index_bogus_id", unique = true )
  private final String                     bogusId;
  @ManyToOne
  @JoinColumn( name = "metadata_network_index_extant_network" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private ExtantNetwork                    extantNetwork;
  @OneToOne( mappedBy = "networkIndex" )
  private VmInstance                       instance;
  
  private PrivateNetworkIndex( ) {
    super( null, null );
    this.index = null;
    this.extantNetwork = null;
    this.bogusId = null;
  }
  
  private PrivateNetworkIndex( ExtantNetwork network ) {
    super( null, null );
    this.extantNetwork = network;
    this.setState( Resource.State.FREE );
    this.bogusId = null;
    this.index = null;
  }
  
  private PrivateNetworkIndex( ExtantNetwork network, Long index ) {
    super( network.getOwner( ), network.getTag( ) + ":" + index );
    this.extantNetwork = network;
    this.setState( Resource.State.FREE );
    this.bogusId = network.getTag( ) + ":" + index;
    this.index = index;
  }
  
  public PrivateNetworkIndex( Integer tag, Long index ) {
    super( null, null );
    this.bogusId = tag + ":" + index;
    this.extantNetwork = null;
    this.index = index;
  }
  
  public static PrivateNetworkIndex create( ExtantNetwork exNet, Long index ) {
    return new PrivateNetworkIndex( exNet, index );
  }
  
  public static PrivateNetworkIndex bogus( ) {
    return bogusIndex;
  }
  
  public static SetReference<PrivateNetworkIndex, VmInstance> bogusSetReference( ) {
    return new SetReference<PrivateNetworkIndex, VmInstance>( ) {
      
      public int compareTo( PrivateNetworkIndex o ) {
        return bogusIndex.compareTo( o );
      }
      
      @Override
      public PrivateNetworkIndex set( VmInstance referer ) throws ResourceAllocationException {
        return bogusIndex;
      }
      
      @Override
      public PrivateNetworkIndex get( ) {
        return bogusIndex;
      }
      
      public PrivateNetworkIndex abort( ) throws ResourceAllocationException {
        return bogusIndex;
      }
      
      @Override
      public PrivateNetworkIndex clear( ) throws ResourceAllocationException {
        return bogusIndex;
      }
    };
  }
  
  public Long getIndex( ) {
    return this.index;
  }
  
  public void setExtantNetwork( ExtantNetwork exNet ) {
    this.extantNetwork = exNet;
  }
  
  public ExtantNetwork getExtantNetwork( ) {
    return this.extantNetwork;
  }
  
  private String getBogusId( ) {
    return this.bogusId;
  }
  
  @Override
  protected void setReferer( VmInstance referer ) {
    this.setInstance( referer );
  }
  
  @Override
  protected VmInstance getReferer( ) {
    try {
      return Transactions.find( this.getInstance( ) );
    } catch ( TransactionException ex ) {
      Logs.extreme( ).error( ex, ex );
      return null;
    }
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.index == null )
      ? 0
      : this.index.hashCode( ) );
    return result;
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) {
      return true;
    }
    if ( !super.equals( obj ) ) {
      return false;
    }
    if ( getClass( ) != obj.getClass( ) ) {
      return false;
    }
    PrivateNetworkIndex other = ( PrivateNetworkIndex ) obj;
    if ( this.index == null ) {
      if ( other.index != null ) {
        return false;
      }
    } else if ( !this.index.equals( other.index ) ) {
      return false;
    }
    return true;
  }
  
  @Override
  public int compareTo( PrivateNetworkIndex o ) {
    if ( this.getExtantNetwork( ) != null ) {
      return ( this.getExtantNetwork( ).getTag( ).equals( o.getExtantNetwork( ).getTag( ) )
        ? this.getIndex( ).compareTo( o.getIndex( ) )
        : this.getExtantNetwork( ).compareTo( o.getExtantNetwork( ) ) );
    } else {
      return ( o.getExtantNetwork( ) == null
        ? 0
        : -1 );
    }
  }
  
  public static Predicate<PrivateNetworkIndex> filterFree( ) {
    return new Predicate<PrivateNetworkIndex>( ) {
      
      @Override
      public boolean apply( PrivateNetworkIndex input ) {
        return Resource.State.FREE.equals( input.getState( ) );
      }
    };
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "PrivateNetworkIndex:" );
    if ( this.extantNetwork != null ) builder.append( this.extantNetwork.getName( ) ).append( " tag=" ).append( this.extantNetwork.getTag( ) );
    if ( this.index != null ) builder.append( "idx=" ).append( this.index );
    return builder.toString( );
  }
  
  private VmInstance getInstance( ) {
    return this.instance;
  }
  
  private void setInstance( VmInstance instance ) {
    this.instance = instance;
  }
  
}
