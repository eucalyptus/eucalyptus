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

import java.util.concurrent.Callable;
import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.bootstrap.Databases;
import com.eucalyptus.cloud.util.PersistentReference;
import com.eucalyptus.cloud.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.util.FullName;
import com.eucalyptus.util.RestrictedType;
import com.eucalyptus.vm.VmInstance;
import com.google.common.base.Objects;
import groovy.sql.Sql;

/**
 * Entity for recording address ownership, reservation, and usage.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_private_addresses" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
public class PrivateAddress extends PersistentReference<PrivateAddress, VmInstance> implements RestrictedType {
  private static final long serialVersionUID = 1L;

  @OneToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_instance_fk" )
  @Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
  private VmInstance                       instance;

  @Column( name = "metadata_address_scope" )
  private String scope;

  @Column( name = "metadata_address_tag" )
  private String tag;

  @Column( name = "metadata_partition_name" )
  private String assignedPartition;

  private PrivateAddress( ) {
    super( null, null );
  }

  private PrivateAddress( final String address ) {
    super( null, address );
  }

  public static PrivateAddress named( String scope, String address ) {
    final PrivateAddress privateAddress = new PrivateAddress( address );
    privateAddress.setUniqueName( buildUniqueName( scope, privateAddress.getDisplayName( ) ) );
    return privateAddress;
  }

  public static PrivateAddress tagged( String tag ) {
    final PrivateAddress privateAddress = new PrivateAddress( );
    privateAddress.setTag( tag );
    return privateAddress;
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

  public static PrivateAddress create( final String scope, final String tag, final String address ) {
    final PrivateAddress privateAddress = new PrivateAddress( address );
    privateAddress.setOwner( Principals.nobodyFullName( ) );
    privateAddress.setState( State.FREE );
    privateAddress.setScope( scope );
    privateAddress.setTag( tag );
    return privateAddress;
  }

  protected String createUniqueName( ) {
    return buildUniqueName( getScope( ), getDisplayName( ) );
  }

  private static String buildUniqueName( final String scope, final String name ) {
    return scope == null ?
        name :
        scope + ":" + name;
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
    return builder.toString();
  }

  @Nullable
  public String getInstanceId( ) {
    return CloudMetadatas.toDisplayName( ).apply( instance );
  }

  private VmInstance getInstance( ) {
    return this.instance;
  }

  private void setInstance( VmInstance instance ) {
    this.instance = instance;
  }

  public String getScope( ) {
    return scope;
  }

  private void setScope( final String scope ) {
    this.scope = scope;
  }

  public String getTag( ) {
    return tag;
  }

  public void setTag( final String tag ) {
    this.tag = tag;
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
        .relativeId(
            "scope", Objects.firstNonNull( this.getScope( ), "global" ),
            "private-address", this.getDisplayName( ) );
  }

  @Override
  protected void ensureTransaction() {
  }

  @Upgrades.PreUpgrade( value = Eucalyptus.class, since = Upgrades.Version.v4_1_0 )
  public static class PrivateAddressPreUpgrade41 implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( PrivateAddressPreUpgrade41.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Databases.getBootstrapper().getConnection( "eucalyptus_cloud" );
        sql.execute( "alter table metadata_private_addresses drop column if exists metadata_address" );
        return true;
      } catch ( Exception ex ) {
        logger.error( "Error deleting column metadata_address for metadata_private_addresses", ex );
        return false;
      } finally {
        if ( sql != null ) {
          sql.close( );
        }
      }
    }
  }
}
