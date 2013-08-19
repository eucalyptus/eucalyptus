/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import com.eucalyptus.cloud.util.PersistentReference;
import com.eucalyptus.cloud.util.Reference;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.AccountMetadata;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.HasFullName;
import com.eucalyptus.vm.VmInstance;

@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_indices" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PrivateNetworkIndex extends PersistentReference<PrivateNetworkIndex, VmInstance> {
  @Transient
  private static final PrivateNetworkIndex bogusIndex = new PrivateNetworkIndex( -1, -1l );
  @Column( name = "metadata_network_index" )
  private final Long                       index;
  @Column( name = "metadata_network_index_bogus_id", unique = true )
  private final String                     bogusId;
  @ManyToOne
  @JoinColumn( name = "metadata_network_index_extant_network_fk" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private ExtantNetwork                    extantNetwork;
  @NotFound( action = NotFoundAction.IGNORE )
  @OneToOne( mappedBy = "networkIndex", fetch = FetchType.LAZY, optional = true )
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
    this.setState( Reference.State.FREE );
    this.bogusId = null;
    this.index = null;
  }
  
  private PrivateNetworkIndex( ExtantNetwork network, Long index ) {
    super( network.getOwner( ), network.getTag( ) + ":" + index );
    this.extantNetwork = network;
    this.setState( Reference.State.FREE );
    this.bogusId = network.getTag( ) + ":" + index;
    this.index = index;
  }
  
  private PrivateNetworkIndex( Integer tag, Long index ) {
    super( null, null );
    this.bogusId = tag + ":" + index;
    this.extantNetwork = null;
    this.index = index;
  }
  
  public static PrivateNetworkIndex named( Integer vlan, Long networkIndex ) {
    return new PrivateNetworkIndex( vlan, networkIndex );
  }
  
  public static PrivateNetworkIndex named( ExtantNetwork exNet, Long index ) {
    return new PrivateNetworkIndex( exNet.getTag( ), index );
  }
  
  public static PrivateNetworkIndex create( ExtantNetwork exNet, Long index ) {//TODO:GRZE: fix for sanity.
    return new PrivateNetworkIndex( exNet, index );
  }
  
  public static PrivateNetworkIndex bogus( ) {
    return bogusIndex;
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
  protected void setReference( VmInstance referer ) {
    this.setInstance( referer );
    if ( referer != null ) {
      referer.setNetworkIndex( this );
    }
  }
  
  @Override
  protected VmInstance getReference( ) {
    return this.getInstance( );
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
  public int compareTo( AccountMetadata that ) {
    if ( that instanceof PrivateNetworkIndex ) {
      PrivateNetworkIndex o = ( PrivateNetworkIndex ) that;
      if ( this.getExtantNetwork( ) != null ) {
        return ( this.getExtantNetwork( ).getTag( ).equals( o.getExtantNetwork( ).getTag( ) )
          ? this.getIndex( ).compareTo( o.getIndex( ) )
          : this.getExtantNetwork( ).compareTo( o.getExtantNetwork( ) ) );
      } else {
        return ( o.getExtantNetwork( ) == null
          ? 0
          : -1 );
      }
    } else {
      return super.compareTo( that );
    }
  }
  
  @Override
  public String toString( ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "PrivateNetworkIndex:" );
    if ( this.extantNetwork != null ) builder.append( this.extantNetwork.getName( ) ).append( " tag=" ).append( this.extantNetwork.getTag( ) );
    if ( this.index != null ) builder.append( " idx=" ).append( this.index );
    return builder.toString( );
  }
  
  private VmInstance getInstance( ) {
    return this.instance;
  }
  
  private void setInstance( VmInstance instance ) {
    this.instance = instance;
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
                          .relativeId( "security-group", this.getExtantNetwork( ).getNetworkGroup( ).getDisplayName( ),
                                       "tag", this.getExtantNetwork( ).getTag( ).toString( ),
                                       "index", this.getIndex( ).toString( ) );
  }
  
  @Override
  public PrivateNetworkIndex release( ) throws ResourceAllocationException {
    this.extantNetwork = null;
    return super.release( );
  }
  
}
