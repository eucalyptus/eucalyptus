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
package com.eucalyptus.compute.common.internal.vpc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.compute.common.internal.tags.TagSupport;
import com.eucalyptus.compute.common.internal.tags.Tags;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 *
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloud" )
@Table( name = "metadata_tags_route_tables" )
@Cache( usage = CacheConcurrencyStrategy.TRANSACTIONAL )
@DiscriminatorValue( "route-table" )
public class RouteTableTag extends Tag<RouteTableTag> {
  private static final long serialVersionUID = 1L;

  @JoinColumn( name = "metadata_tag_resource_id", updatable = false, nullable = false )
  @ManyToOne( fetch = FetchType.LAZY )
  private RouteTable routeTable;

  protected RouteTableTag( ) {
    super( "route-table", ResourceIdFunction.INSTANCE );
  }

  public RouteTableTag( @Nonnull final RouteTable routeTable,
                        @Nonnull final OwnerFullName ownerFullName,
                        @Nullable final String key,
                        @Nullable final String value ) {
    super( "route-table", ResourceIdFunction.INSTANCE, ownerFullName, key, value );
    setRouteTable( routeTable );
    init();
  }

  public RouteTable getRouteTable( ) {
    return routeTable;
  }

  public void setRouteTable( final RouteTable routeTable ) {
    this.routeTable = routeTable;
  }

  @Nonnull
  public static Tag named( @Nonnull final RouteTable routeTable,
                           @Nonnull final OwnerFullName ownerFullName,
                           @Nullable final String key ) {
    return namedWithValue( routeTable, ownerFullName, key, null );
  }

  @Nonnull
  public static Tag namedWithValue( @Nonnull final RouteTable routeTable,
                                    @Nonnull final OwnerFullName ownerFullName,
                                    @Nullable final String key,
                                    @Nullable final String value ) {
    Preconditions.checkNotNull( routeTable, "routeTable" );
    Preconditions.checkNotNull( ownerFullName, "ownerFullName" );
    return new RouteTableTag( routeTable, ownerFullName, key, value );
  }

  private enum ResourceIdFunction implements Function<RouteTableTag,String> {
    INSTANCE {
      @Override
      public String apply( final RouteTableTag routeTableTag ) {
        return routeTableTag.getRouteTable( ).getDisplayName( );
      }
    }
  }

  public static final class RouteTableTagSupport extends TagSupport {
    public RouteTableTagSupport() {
      super( RouteTable.class, "rtb", "displayName", "routeTable", " InvalidRouteTableID.NotFound", "The route table '%s' does not exist." );
    }

    @Override
    public Tag createOrUpdate( final CloudMetadata metadata, final OwnerFullName ownerFullName, final String key, final String value ) {
      return Tags.createOrUpdate( new RouteTableTag( (RouteTable) metadata, ownerFullName, key, value ) );
    }

    @Override
    public Tag example( @Nonnull final CloudMetadata metadata, @Nonnull final OwnerFullName ownerFullName, final String key, final String value ) {
      return RouteTableTag.namedWithValue( (RouteTable) metadata, ownerFullName, key, value );
    }

    @Override
    public Tag example( @Nonnull final OwnerFullName ownerFullName ) {
      return example( new RouteTableTag( ), ownerFullName );
    }

    @Override
    public CloudMetadata lookup( final String identifier ) throws TransactionException {
      return Entities.uniqueResult( RouteTable.exampleWithName( null, identifier ) );
    }
  }
}
