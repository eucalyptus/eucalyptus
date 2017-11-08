/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.network;

import java.util.List;
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
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.compute.common.internal.network.PrivateAddressReferrer;
import com.eucalyptus.compute.common.internal.util.PersistentReference;
import com.eucalyptus.compute.common.internal.util.ResourceAllocationException;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.internal.vm.VmInstance_;
import com.eucalyptus.compute.common.internal.vm.VmNetworkConfig_;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterfaceAttachment_;
import com.eucalyptus.compute.common.internal.vpc.NetworkInterface_;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.upgrade.Upgrades;
import com.eucalyptus.auth.principal.FullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import groovy.sql.Sql;

/**
 * Entity for recording address ownership, reservation, and usage.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_private_addresses" )
public class PrivateAddress extends PersistentReference<PrivateAddress, PrivateAddressReferrer> implements RestrictedType {
  private static final long serialVersionUID = 1L;

  @OneToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_instance_fk" )
  private VmInstance instance;

  @OneToOne( fetch = FetchType.LAZY )
  @JoinColumn( name = "metadata_network_interface_fk" )
  private NetworkInterface networkInterface;

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

  public static PrivateAddress scoped( String scope, String tag ) {
    final PrivateAddress privateAddress = new PrivateAddress( );
    privateAddress.setScope( scope );
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
  protected void setReference( PrivateAddressReferrer referrer ) {
    if ( referrer instanceof VmInstance ) {
      this.setInstance( (VmInstance) referrer );
      this.setNetworkInterface( null );
    } else if ( referrer instanceof NetworkInterface ) {
      this.setInstance( null );
      this.setNetworkInterface( (NetworkInterface) referrer );
    } else if ( referrer == null ) {
      this.setInstance( null );
      this.setNetworkInterface( null );
    } else {
      throw new IllegalArgumentException( "Unknown referrer type " + referrer.getClass( ) );
    }
    this.setAssignedPartition( referrer==null ? null : referrer.getPartition() );
  }

  @Override
  protected PrivateAddressReferrer getReference( ) {
    return this.getInstance( ) != null ?
        this.getInstance( ) :
        this.getNetworkInterface( );
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
  public String getOwnerId( ) {
    return CloudMetadatas.toDisplayName( ).apply( getReference( ) );
  }

  private VmInstance getInstance( ) {
    return this.instance;
  }

  private void setInstance( VmInstance instance ) {
    this.instance = instance;
  }

  public NetworkInterface getNetworkInterface() {
    return networkInterface;
  }

  public void setNetworkInterface( final NetworkInterface networkInterface ) {
    this.networkInterface = networkInterface;
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
            "scope", MoreObjects.firstNonNull( this.getScope( ), "global" ),
            "private-address", this.getDisplayName( ) );
  }

  @Override
  protected void ensureTransaction() {
  }

  @SuppressWarnings( { "unused", "WeakerAccess" } )
  @Upgrades.PreUpgrade( value = Eucalyptus.class, since = Upgrades.Version.v4_1_0 )
  public static class PrivateAddressPreUpgrade41 implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger( PrivateAddressPreUpgrade41.class );

    @Override
    public Boolean call( ) throws Exception {
      Sql sql = null;
      try {
        sql = Upgrades.DatabaseFilters.NEWVERSION.getConnection( "eucalyptus_cloud" );
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

  @SuppressWarnings( { "unused", "WeakerAccess" } )
  @Upgrades.EntityUpgrade( entities = PrivateAddress.class, since = Upgrades.Version.v4_3_0, value = Eucalyptus.class )
  public enum PrivateAddressEntityUpgrade43 implements Predicate<Class> {
    INSTANCE;
    private static Logger LOG = Logger.getLogger( PrivateAddress.PrivateAddressEntityUpgrade43.class );

    @Override
    public boolean apply( Class arg0 ) {
      updatePrivateAddressOwnership( );
      return true;
    }

    /**
     * Update private addresses owned by instances to be owned by the primary network interface instead
     */
    private void updatePrivateAddressOwnership( ) {
      try ( final TransactionResource tx = Entities.transactionFor( PrivateAddress.class ) ) {
        final List<PrivateAddress> addresses = Entities.criteriaQuery( PrivateAddress.class )
            .join( PrivateAddress_.instance )
            .join( VmInstance_.networkConfig )
            .join( VmNetworkConfig_.networkInterfaces )
            .join( NetworkInterface_.attachment )
            .whereEqual( NetworkInterfaceAttachment_.deviceIndex, 0 )
            .list( );

        for ( final PrivateAddress address : addresses ) {
          final PrivateAddressReferrer referrer = address.getReference( );
          if ( referrer instanceof VmInstance ) {
            final VmInstance instance = (VmInstance) referrer;
            final NetworkInterface networkInterface = Iterables.get( instance.getNetworkInterfaces( ), 0, null );
            if ( networkInterface != null ) {
              LOG.info( "Updating private ip " + address.getDisplayName( ) + " owner from " +
                  instance.getDisplayName( ) + " to " + networkInterface.getDisplayName( ) );
              try {
                address.reclaim( networkInterface );
              } catch ( final ResourceAllocationException e ) {
                LOG.error( "Error changing owner for private ip " + address.getDisplayName( ), e );
              }
            }
          }
        }

        tx.commit( );
      }
    }
  }
}
