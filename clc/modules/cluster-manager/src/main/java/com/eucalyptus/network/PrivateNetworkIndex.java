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

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.NaturalId;
import com.eucalyptus.cloud.util.PersistentResource;
import com.eucalyptus.cloud.util.ResourceAllocation;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.entities.AbstractStatefulPersistent;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.util.Logs;
import com.eucalyptus.util.TransactionException;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_network_indices" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PrivateNetworkIndex extends PersistentResource<PrivateNetworkIndex, VmInstance> implements Comparable<PrivateNetworkIndex> {
  @NaturalId
  @Column( name = "metadata_network_index" )
  private final Long          index;
  @NaturalId
  @Column( name = "metadata_network_index_network_tag" )
  private final Long          tag;
  private final ExtantNetwork network;
  @Column( name = "metadata_network_index_vm_perm_uuid" )
  private String              instanceNaturalId;
  
  private PrivateNetworkIndex( ) {
    super( );
    this.index = null;
    this.tag = null;
    this.network = null;
  }
  
  private PrivateNetworkIndex( ExtantNetwork network, Long index ) {
    super( );
    this.network = network;
    this.tag = network.getTag( );
    this.index = index;
  }
  
  public PrivateNetworkIndex( Long tag, Long index ) {
    this.tag = tag;
    this.network = null;
    this.index = index;
  }
  
  public static PrivateNetworkIndex bogus( ) {
    return new PrivateNetworkIndex( -1l, -1l );
  }
  
  public Long getIndex( ) {
    return this.index;
  }
  
  public String getInstanceNaturalId( ) {
    return this.instanceNaturalId;
  }
  
  public void setInstanceNaturalId( String instanceNaturalId ) {
    this.instanceNaturalId = instanceNaturalId;
  }
  
  @Override
  protected void setReferer( VmInstance referer ) {
    this.instanceNaturalId = referer.getNaturalId( );
  }
  
  @Override
  protected VmInstance getReferer( ) {
    try {
      return Transactions.find( new VmInstance( ) {
        {
          this.setNaturalId( PrivateNetworkIndex.this.getInstanceNaturalId( ) );
        }
      } );
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
    return ( this.tag.equals( o.tag )
      ? this.index.compareTo( o.index )
      : this.tag.compareTo( o.tag ) );
  }
}
