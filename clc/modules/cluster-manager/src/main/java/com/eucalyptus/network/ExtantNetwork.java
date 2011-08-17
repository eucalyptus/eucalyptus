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

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.cloud.util.Resource;
import com.eucalyptus.cloud.util.Resource.SetReference;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.cluster.VmInstance;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.entities.TransactionExecutionException;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Logs;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_extant_network" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ExtantNetwork extends UserMetadata<Resource.State> implements Comparable<ExtantNetwork> {
  @Transient
  private static final long        serialVersionUID = 1L;
  @Transient
  private static Logger            LOG              = Logger.getLogger( ExtantNetwork.class );
  @Column( name = "metadata_extant_network_tag", unique = true )
  private Integer                  tag;
  @OneToMany( fetch = FetchType.EAGER, cascade = { CascadeType.ALL }, mappedBy = "extantNetwork" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<PrivateNetworkIndex> indexes          = new HashSet<PrivateNetworkIndex>( );
  @OneToOne
  @JoinColumn( name = "metadata_extant_network_group_id" )
  private NetworkGroup             networkGroup;
  
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
  }
  
  public static ExtantNetwork named( Integer tag ) {
    return new ExtantNetwork( tag );
  }
  
  public static ExtantNetwork create( final NetworkGroup networkGroup, final Integer tag ) {
    return new ExtantNetwork( networkGroup, tag );
  }
  
  public static ExtantNetwork bogus( NetworkGroup networkGroup ) {
    return new ExtantNetwork( networkGroup, -1 );
  }
  
  public boolean hasIndexes( ) {
    for ( PrivateNetworkIndex index : this.getIndexes( ) ) {
      if ( Resource.State.EXTANT.equals( index.getState( ) ) ) {
        return true;
      }
    }
    return false;
  }
  
  public Integer getTag( ) {
    return this.tag;
  }
  
  protected void setTag( final Integer tag ) {
    this.tag = tag;
  }
  
  protected Set<PrivateNetworkIndex> getIndexes( ) {
    return this.indexes;
  }
  
  protected void setIndexes( final Set<PrivateNetworkIndex> indexes ) {
    this.indexes = indexes;
  }
  
  public SetReference<PrivateNetworkIndex, VmInstance> allocateNetworkIndex( ) throws TransactionException {
    EntityWrapper<ExtantNetwork> db = Entities.get( ExtantNetwork.class );
    ExtantNetwork exNet = this;
    try {
      exNet = db.getUnique( this );
      if ( exNet.getIndexes( ).isEmpty( ) ) {
        this.initNetworkIndexes( exNet );
        db = Entities.get( ExtantNetwork.class );
      }
      exNet = db.getUnique( this );
      SetReference<PrivateNetworkIndex, VmInstance> next = PrivateNetworkIndex.allocateNext( exNet );
      return next;
    } catch ( EucalyptusCloudException ex ) {
      Logs.extreme( ).error( ex, ex );
      LOG.debug( ex );
      db.rollback( );
      throw new TransactionExecutionException( ex );
    } catch ( ResourceAllocationException ex ) {
      Logs.extreme( ).error( ex, ex );
      LOG.debug( ex );
      db.rollback( );
      throw new TransactionExecutionException( ex );
    }
  }
  
  private void initNetworkIndexes( ExtantNetwork exNet ) {
    EntityWrapper<PrivateNetworkIndex> db = Entities.get( PrivateNetworkIndex.class );
    for ( long i = NetworkGroups.networkingConfiguration( ).getMinNetworkIndex( ); i < NetworkGroups.networkingConfiguration( ).getMaxNetworkIndex( ); i++ ) {
      PrivateNetworkIndex newIdx = PrivateNetworkIndex.create( exNet, i );
      PrivateNetworkIndex netIdx = db.persist( newIdx );
      this.getIndexes( ).add( netIdx );
    }
    db.commit( );
  }
  
  public NetworkGroup getNetworkGroup( ) {
    return this.networkGroup;
  }
  
  private void setNetworkGroup( NetworkGroup networkGroup ) {
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
    ExtantNetwork other = ( ExtantNetwork ) obj;
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
  public int compareTo( ExtantNetwork that ) {
    return this.getTag( ).compareTo( that.getTag( ) );
  }
  
  @Override
  public String toString( ) {
    return String.format( "ExtantNetwork:networkGroup=%s:tag=%s:indexes=%s", this.networkGroup.getFullName( ), this.tag, this.indexes );
  }
  
}
