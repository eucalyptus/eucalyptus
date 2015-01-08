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
package com.eucalyptus.compute.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.InternetGatewayMetadata;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public interface InternetGateways extends Lister<InternetGateway> {

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super InternetGateway> filter,
                    Function<? super InternetGateway,T> transform ) throws VpcMetadataException;

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super InternetGateway,T> transform ) throws VpcMetadataException;

  <T> T lookupByVpc( @Nullable OwnerFullName ownerFullName,
                     String vpcId,
                     Function<? super InternetGateway,T> transform ) throws VpcMetadataException;

  boolean delete( final InternetGatewayMetadata metadata ) throws VpcMetadataException;

  InternetGateway save( InternetGateway internetGateway ) throws VpcMetadataException;

  InternetGateway updateByExample( InternetGateway example,
                                   OwnerFullName ownerFullName,
                                   String key,
                                   Callback<InternetGateway> updateCallback ) throws VpcMetadataException;

  @TypeMapper
  public enum InternetGatewayToInternetGatewayTypeTransform implements Function<InternetGateway,InternetGatewayType> {
    INSTANCE;

    @Nullable
    @Override
    public InternetGatewayType apply( @Nullable final InternetGateway internetGateway ) {
      return internetGateway == null ?
          null :
          new InternetGatewayType(
              internetGateway.getDisplayName( ),
              CloudMetadatas.toDisplayName( ).apply( internetGateway.getVpc( ) )
          );
    }
  }

  public static class InternetGatewayFilterSupport extends FilterSupport<InternetGateway> {
    public InternetGatewayFilterSupport( ) {
      super( builderFor( InternetGateway.class )
          .withTagFiltering( InternetGatewayTag.class, "internetGateway" )
          .withStringProperty( "attachment.state", FilterStringFunctions.STATE )
          .withStringProperty( "attachment.vpc-id", FilterStringFunctions.VPC_ID )
          .withStringProperty( "internet-gateway-id", CloudMetadatas.toDisplayName( ) )
          .withPersistenceAlias( "vpc", "vpc" )
          .withPersistenceFilter( "attachment.state", "vpc.displayName", new Function<String, String>() {
            @Nullable
            @Override
            public String apply( @Nullable final String value ) {
              // available means there must be an attachment so we check for any VPC (filter wildcard)
              return "available".equals( value ) ? "*" : null;
            }
          } )
          .withPersistenceFilter( "vpc-id", "vpc.displayName" )
          .withPersistenceFilter( "internet-gateway-id", "displayName" )
      );
    }
  }

  public enum FilterStringFunctions implements Function<InternetGateway,String> {
    STATE {
      @Override
      public String apply( final InternetGateway internetGateway ){
        return internetGateway.getVpc() == null ? null : "available";
      }
    },
    VPC_ID {
      @Override
      public String apply( final InternetGateway internetGateway ){
        return CloudMetadatas.toDisplayName( ).apply( internetGateway.getVpc( ) );
      }
    },
  }


  @RestrictedTypes.QuantityMetricFunction( InternetGatewayMetadata.class )
  public enum CountInternetGateways implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( @Nullable final OwnerFullName input ) {
      try ( final TransactionResource tx = Entities.transactionFor( InternetGateway.class ) ) {
        return Entities.count( InternetGateway.exampleWithOwner( input ) );
      }
    }
  }

}
