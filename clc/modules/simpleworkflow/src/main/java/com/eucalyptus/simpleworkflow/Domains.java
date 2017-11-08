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
package com.eucalyptus.simpleworkflow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata;
import com.eucalyptus.simpleworkflow.common.model.DomainConfiguration;
import com.eucalyptus.simpleworkflow.common.model.DomainDetail;
import com.eucalyptus.simpleworkflow.common.model.DomainInfo;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes;
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
                      Predicate<? super Domain> filter,
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

  <T> List<T> listDeprecatedExpired( long time,
                                     Function<? super Domain,T> transform ) throws SwfMetadataException;

  Domain updateByExample( Domain example,
                          OwnerFullName ownerFullName,
                          String key,
                          Callback<Domain> updateCallback ) throws SwfMetadataException;

  Domain save( Domain domain ) throws SwfMetadataException;


  List<Domain> deleteByExample( Domain example ) throws SwfMetadataException;

  AbstractPersistentSupport<SimpleWorkflowMetadata.DomainMetadata,Domain,SwfMetadataException> withRetries( );

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

  @RestrictedTypes.QuantityMetricFunction( SimpleWorkflowMetadata.DomainMetadata.class )
  public enum CountDomains implements Function<OwnerFullName, Long> {
    INSTANCE;

    @Override
    public Long apply( @Nullable final OwnerFullName input ) {
      try ( final TransactionResource tx = Entities.transactionFor( Domain.class ) ) {
        return Entities.count( Domain.exampleWithOwner( input ) );
      }
    }
  }
}
