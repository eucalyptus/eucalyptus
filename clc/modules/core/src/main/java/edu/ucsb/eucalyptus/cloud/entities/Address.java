/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.entities;

import edu.ucsb.eucalyptus.msgs.DescribeAddressesResponseItemType;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.eucalyptus.util.HasName;

import javax.persistence.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Entity
@Table( name = "addresses" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class Address implements HasName {

  private static Logger LOG = Logger.getLogger( Address.class );

  @Transient
  private final ReadWriteLock canHas = new ReentrantReadWriteLock( true );

  public static String UNALLOCATED_USERID = "nobody";
  public static String UNASSIGNED_INSTANCEID = "available";
  public static String UNASSIGNED_INSTANCEADDR = "0.0.0.0";
  public static String PENDING_ASSIGNMENT = "pending";
  @Id
  @GeneratedValue
  @Column( name = "address_id" )
  private Long id = -1l;
  @Column( name = "address_name" )
  private String name;
  @Column( name = "address_cluster" )
  private String cluster;
  @Column( name = "address_owner_id" )
  private String userId;
  @Column( name = "address_is_assigned" )
  private Boolean assigned;
  @Column( name = "address_instance_id" )
  private String instanceId;
  @Column( name = "address_instance_addr" )
  private String instanceAddress;

  public Address() {}

  public Address( final String name ) {
    this.name = name;
  }

  public Address( final String name, final String cluster, final String userId, final String instanceId, final String instanceAddress ) {
    this.name = name;
    this.cluster = cluster;
    this.userId = userId;
    this.instanceId = instanceId;
    this.instanceAddress = instanceAddress;
  }

  public Address( String address, String cluster ) {
    this.name = address;
    this.cluster = cluster;
    this.userId = UNALLOCATED_USERID;
    this.instanceId = UNASSIGNED_INSTANCEID;
    this.instanceAddress = UNASSIGNED_INSTANCEADDR;
  }

  public Boolean getAssigned() {
    return assigned;
  }

  public void setAssigned( final Boolean assigned ) {
    this.assigned = assigned;
  }

  public String getCluster() {
      return cluster;
  }

  public void release() {
    this.canHas.writeLock().lock();
    try {
      this.userId = UNALLOCATED_USERID;
      this.unassign();
      return;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public void unassign() {
    this.canHas.writeLock().lock();
    try {
      this.instanceId = UNASSIGNED_INSTANCEID;
      this.instanceAddress = UNASSIGNED_INSTANCEADDR;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public boolean isAssigned() {
    this.canHas.writeLock().lock();
    try {
      if ( UNASSIGNED_INSTANCEID.equals( this.instanceId ) || UNASSIGNED_INSTANCEADDR.equals( this.instanceAddress ) ) {
        this.instanceId = UNASSIGNED_INSTANCEID;
        this.instanceAddress = UNASSIGNED_INSTANCEADDR;
        return false;
      }
      return true;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public boolean isPending() {
    return PENDING_ASSIGNMENT.equals( this.instanceId ) && PENDING_ASSIGNMENT.equals( this.instanceAddress );
  }

  public String getUserId() {
      return userId;
  }

  public void setUserId( final String userId ) {
    this.userId = userId;
  }

  public boolean allocate( String userId ) {
    this.canHas.writeLock().lock();
    try {
      this.unassign();
      if( this.userId != null && !this.userId.equals( UNALLOCATED_USERID ) ) return false;
      this.setUserId( userId );
      return true;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public boolean assign( String instanceId, String instanceAddr ) {
    this.canHas.writeLock().lock();
    try {
      if ( UNASSIGNED_INSTANCEADDR.equals( instanceAddr ) ) return false;
      this.instanceId = instanceId;
      this.instanceAddress = instanceAddr;
      return true;
    } finally {
      this.canHas.writeLock().unlock();
    }
  }

  public String getInstanceId() {
    return this.instanceId;
  }

  public String getName() {
    return this.name;
  }

  public DescribeAddressesResponseItemType getDescription( boolean isAdmin ) {
    return new DescribeAddressesResponseItemType( this.getName(), isAdmin ? String.format( "%s (%s)", this.getInstanceId(),this.getUserId() ) : UNASSIGNED_INSTANCEID.equals( this.getInstanceId() )? null : this.getInstanceId( ));
  }

  public int compareTo( final Object o ) {
    Address that = ( Address ) o;
    return this.getName().compareTo( that.getName() );
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof Address ) ) return false;

    Address address = ( Address ) o;

    if ( !name.equals( address.name ) ) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public String getInstanceAddress() {
    return instanceAddress;
  }

  public void setInstanceAddress( final String instanceAddress ) {
    this.instanceAddress = instanceAddress;
  }

  public Long getId() {
    return id;
  }

  public void setId( final Long id ) {
    this.id = id;
  }

  public void setCluster( final String cluster ) {
    this.cluster = cluster;
  }

  @Override
  public String toString() {
    return "Address{" +
           "name='" + name + '\'' +
           ", cluster='" + cluster + '\'' +
           ", sourceUserId='" + userId + '\'' +
           ", assigned=" + assigned +
           ", instanceId='" + instanceId + '\'' +
           ", instanceAddress='" + instanceAddress + '\'' +
           '}';
  }
}
