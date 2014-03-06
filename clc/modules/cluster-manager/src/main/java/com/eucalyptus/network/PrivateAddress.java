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
 ************************************************************************/
package com.eucalyptus.network;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cloud.util.PersistentReference;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.RestrictedType;
import com.eucalyptus.vm.VmInstance;

/**
 * Entity for recording address ownership, reservation, and usage.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_private_addresses" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PrivateAddress extends PersistentReference<PrivateAddress, VmInstance> implements RestrictedType {
  private static final long serialVersionUID = 1L;

  @Column( name = "metadata_address", unique = true, nullable = false, updatable = false )
  private Integer address;

  @OneToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_instance_fk" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private VmInstance                       instance;

  @Column( name = "metadata_partition_name" )
  private String assignedPartition;

  private PrivateAddress( ) {
    this( (Integer) null );
  }

  private PrivateAddress( final String address ) {
    super( null, address );
    if ( address != null ) {
      this.address = PrivateAddresses.asInteger( address );
    }
  }

  private PrivateAddress( final Integer address ) {
    super( null, null );
    this.address = address;
  }

  public static PrivateAddress named( String address ) {
    return new PrivateAddress( address );
  }

  public static PrivateAddress named( Integer address ) {
    return new PrivateAddress( address );
  }

  public static PrivateAddress inState( State state ) {
    return inState( state, null );
  }

  public static PrivateAddress inState( State state, String partition ) {
    final PrivateAddress privateAddress = new PrivateAddress(  );
    privateAddress.setState( state );
    privateAddress.setStateChangeStack( null );
    privateAddress.setLastState( null );
    privateAddress.setAssignedPartition( partition );
    return privateAddress;
  }

  public static PrivateAddress create( String address ) {
    final PrivateAddress privateAddress = new PrivateAddress( address );
    privateAddress.setOwner( Principals.nobodyFullName( ) );
    privateAddress.setState( State.FREE );
    return privateAddress;
  }

  protected String createUniqueName( ) {
    return getDisplayName( );
  }

  @Override
  protected void setReference( VmInstance referrer ) {
    this.setInstance( referrer );
    this.setAssignedPartition( referrer==null ? null : referrer.getPartition() );
  }

  @Override
  protected VmInstance getReference( ) {
    return this.getInstance( );
  }

  public void releasing( ) throws ResourceAllocationException {
    doSetReferer( null, State.EXTANT, State.RELEASING );
  }

  @Override
  public String toString( ) {
    final StringBuilder builder = new StringBuilder( );
    builder.append( "PrivateAddress:" );
    if ( this.getDisplayName() != null ) {
      builder.append( this.getDisplayName() );
    }
    return builder.toString( );
  }

  @Nullable
  public String getInstanceId( ) {
    return this.instance == null ? null : this.instance.getDisplayName( );
  }

  private VmInstance getInstance( ) {
    return this.instance;
  }

  private void setInstance( VmInstance instance ) {
    this.instance = instance;
  }

  public String getAssignedPartition( ) {
    return assignedPartition;
  }

  private void setAssignedPartition( final String assignedPartition ) {
    this.assignedPartition = assignedPartition;
  }

  /**
   * @see com.eucalyptus.util.HasFullName#getPartition()
   */
  @Override
  public String getPartition( ) {
    return Eucalyptus.INSTANCE.getName( );
  }

  /**
   * @see com.eucalyptus.util.HasFullName#getFullName()
   */
  @Override
  public FullName getFullName( ) {
    return FullName.create.vendor( "euca" )
        .region( ComponentIds.lookup( Eucalyptus.class ).name( ) )
        .namespace( this.getOwnerAccountNumber( ) )
        .relativeId( "private-address", this.getDisplayName() );
  }

  @Override
  protected void ensureTransaction() {
  }
}
