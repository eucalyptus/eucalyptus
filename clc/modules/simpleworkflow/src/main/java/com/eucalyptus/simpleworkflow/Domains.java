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
package com.eucalyptus.simpleworkflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.simpleworkflow.common.model.DomainConfiguration;
import com.eucalyptus.simpleworkflow.common.model.DomainDetail;
import com.eucalyptus.simpleworkflow.common.model.DomainInfo;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/**
 *
 */
public interface Domains {

  <T> T lookupByName( @Nullable OwnerFullName ownerFullName,
                      String name,
                      Function<? super Domain,T> transform ) throws SwfMetadataException;

  <T> T lookupByExample( Domain example,
                         @Nullable OwnerFullName ownerFullName,
                         String key,
                         Predicate<? super Domain> filter,
                         Function<? super Domain,T> transform ) throws SwfMetadataException;

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super Domain> filter,
                    Function<? super Domain,T> transform ) throws SwfMetadataException;

  Domain updateByExample( Domain example,
                          OwnerFullName ownerFullName,
                          String key,
                          Callback<Domain> updateCallback ) throws SwfMetadataException;

  Domain save( Domain domain ) throws SwfMetadataException;

  @TypeMapper
  public enum DomainToDomainDetailTransform implements Function<Domain,DomainDetail> {
    INSTANCE;

    @Nullable
    @Override
    public DomainDetail apply( @Nullable final Domain domain ) {
      return domain == null ?
          null :
          new DomainDetail( )
              .withConfiguration( new DomainConfiguration()
                  .withWorkflowExecutionRetentionPeriodInDays(
                      Optional.fromNullable( domain.getWorkflowExecutionRetentionPeriodInDays( ) )
                          .transform( Functions.toStringFunction( ) )
                          .or( "NONE" ) ) )
              .withDomainInfo( TypeMappers.transform( domain, DomainInfo.class ) );

    }
  }

  @TypeMapper
  public enum DomainToDomainInfoTransform implements Function<Domain,DomainInfo> {
    INSTANCE;

    @Nullable
    @Override
    public DomainInfo apply( @Nullable final Domain domain ) {
      return domain == null ?
          null :
          new DomainInfo( )
              .withName( domain.getDisplayName( ) )
              .withDescription( domain.getDescription( ) )
              .withStatus( Objects.toString( domain.getState( ), null ) );
    }
  }

  enum StringFunctions implements Function<Domain,String> {
    REGISTRATION_STATUS {
      @Nullable
      @Override
      public String apply( @Nullable final Domain domain ) {
        return domain == null ?
            null :
            Objects.toString( domain.getState( ), null );
      }
    }
  }
}
