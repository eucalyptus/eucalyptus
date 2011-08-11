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
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import com.eucalyptus.cloud.util.ResourceAllocation;
import com.eucalyptus.cloud.util.ResourceAllocation.State;
import com.eucalyptus.entities.AbstractStatefulPersistent;

@Entity
@javax.persistence.Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_extant_network" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class ExtantNetwork extends AbstractStatefulPersistent<ResourceAllocation.State> {
  @OneToOne
  @JoinColumn( name = "id" )
  @NotFound( action = NotFoundAction.IGNORE )
  private NetworkGroup             networkGroup;
  @Column( name = "metadata_extant_network_tag", unique = true )
  private Long                     tag;
  @Column( name = "metadata_extant_network_max_addr" )
  private Long                     maxAddr;
  @Column( name = "metadata_extant_network_min_addr" )
  private Long                     minAddr;
  @OneToMany( mappedBy = "network" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private Set<PrivateNetworkIndex> indexes = new HashSet<PrivateNetworkIndex>( );
  
  public ExtantNetwork( ) {
    super( );
  }
  
  public ExtantNetwork( NetworkGroup networkGroup, Long tag ) {
    super( );
    this.networkGroup = networkGroup;
    this.tag = tag;
    this.maxAddr = 2048l;//GRZE:FIXIT
    this.minAddr = 9l;//GRZE:FIXIT
    for( Long i = this.minAddr; i < this.maxAddr; i++ ) { 
      this.getIndexes( ).add( new PrivateNetworkIndex( tag, i ) );
    }
  }

  public ExtantNetwork( State state, String displayName ) {
    super( state, displayName );
  }
  
  public ExtantNetwork( String displayName ) {
    super( displayName );
  }
  
  public ExtantNetwork( NetworkGroup networkGroup ) {
    this.networkGroup = networkGroup;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( this.networkGroup == null )
      ? 0
      : this.networkGroup.hashCode( ) );
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
    if ( this.networkGroup == null ) {
      if ( other.networkGroup != null ) {
        return false;
      }
    } else if ( !this.networkGroup.equals( other.networkGroup ) ) {
      return false;
    }
    if ( this.tag == null ) {
      if ( other.tag != null ) {
        return false;
      }
    } else if ( !this.tag.equals( other.tag ) ) {
      return false;
    }
    return true;
  }
  
  protected NetworkGroup getNetworkGroup( ) {
    return this.networkGroup;
  }
  
  protected void setNetworkGroup( NetworkGroup networkGroup ) {
    this.networkGroup = networkGroup;
  }
  
  protected Long getTag( ) {
    return this.tag;
  }
  
  protected void setTag( Long tag ) {
    this.tag = tag;
  }
  
  protected Set<PrivateNetworkIndex> getIndexes( ) {
    return this.indexes;
  }
  
  protected void setIndexes( Set<PrivateNetworkIndex> indexes ) {
    this.indexes = indexes;
  }
  
  public PrivateNetworkIndex allocateNetworkIndex( ) {
    return null;
  }

  protected Long getMaxAddr( ) {
    return this.maxAddr;
  }

  protected void setMaxAddr( Long maxAddr ) {
    this.maxAddr = maxAddr;
  }

  protected Long getMinAddr( ) {
    return this.minAddr;
  }

  protected void setMinAddr( Long minAddr ) {
    this.minAddr = minAddr;
  }
  
}
