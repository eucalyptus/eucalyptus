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
package com.eucalyptus.compute.common.internal.vpc;

import static com.eucalyptus.compute.common.CloudMetadata.InternetGatewayMetadata;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.compute.common.CloudMetadatas;
import com.eucalyptus.compute.common.InternetGatewayType;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.compute.common.internal.tags.FilterSupport;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
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
          .withStringProperty( "owner-id", FilterStringFunctions.ACCOUNT_ID )
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
          .withPersistenceFilter( "owner-id", "ownerAccountNumber" )
      );
    }
  }

  public enum FilterStringFunctions implements Function<InternetGateway,String> {
    ACCOUNT_ID {
      @Override
      public String apply( final InternetGateway internetGateway ) {
        return internetGateway.getOwnerAccountNumber( );
      }
    },
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
