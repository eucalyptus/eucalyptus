/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.account;

import static com.eucalyptus.auth.policy.PolicySpec.IAM_RESOURCE_ROLE;
import static com.eucalyptus.auth.policy.PolicySpec.IAM_RESOURCE_USER;
import static com.eucalyptus.auth.policy.PolicySpec.VENDOR_IAM;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.compute.common.IdFormatItemType;
import com.eucalyptus.compute.common.internal.account.IdentityIdFormat.IdResource;
import com.eucalyptus.compute.common.internal.account.IdentityIdFormat.IdType;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.Pair;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 *
 */
@SuppressWarnings( { "Guava", "StaticPseudoFunctionalStyleMethod", "OptionalUsedAsFieldOrParameterType" } )
public class IdentityIdFormats {

  private static final Set<String> LONG_ID_RESOURCES = ImmutableSet.copyOf(
      Stream.of( IdResource.values( ) ).map( IdResource::name ).iterator( )
  );

  private static final Map<String,String> LONG_ID_PREFIX_TO_RESOURCE = ImmutableMap.copyOf(
      Stream.of( IdResource.values( ) ).collect( Collectors.toMap( IdResource::prefix, IdResource::name ) )
  );

  private static final Map<String,String> LONG_ID_RESOURCE_TO_PREFIX = ImmutableMap.copyOf(
      Stream.of( IdResource.values( ) ).collect( Collectors.toMap( IdResource::name, IdResource::prefix ) )
  );

  private static final LoadingCache<Pair<String,String>,Optional<Boolean>> LONG_ID_CONFIG_CACHE =
      CacheBuilder.newBuilder( ).expireAfterWrite( 30, TimeUnit.SECONDS ).maximumSize( 1_000 ).build(
      new CacheLoader<Pair<String,String>,Optional<Boolean>>() {
        @Override
        public Optional<Boolean> load( @Nonnull final Pair<String, String> identityArnAndResource ) {
          final String identityArn = identityArnAndResource.getLeft( );
          final String resource = identityArnAndResource.getRight( );
          final IdResource idResource = IdResource.valueOf( resource );
          final Optional<Pair<String, Pair<IdType, String>>> identityOption = IdentityIdFormats.tryParseIdentity( identityArn );
          if ( identityOption.isPresent( ) ) {
            final Optional<Boolean> identityLongIds = loadIdFormat(
                identityOption.get( ).getLeft( ),
                identityOption.get( ).getRight( ).getLeft( ),
                identityOption.get( ).getRight( ).getRight( ),
                idResource );
            final Optional<Boolean> accountLongIds = identityOption.get( ).getRight( ).getLeft( ) == IdType.account ?
                identityLongIds :
                loadIdFormat(
                    identityOption.get( ).getLeft( ),
                    IdType.account,
                    identityOption.get( ).getLeft( ),
                    idResource );
            return identityLongIds.or( accountLongIds );
          }
          return Optional.absent( );
        }
      }
  );

  public static String generate(
      @Nonnull final String identityArn,
      @Nonnull final String prefix
  ) {
    final String resource = LONG_ID_PREFIX_TO_RESOURCE.get( prefix );
    if ( resource != null && ResourceIdentifiers.useAccountLongIdentifierSettings( ) ) {
      final Optional<Boolean> configuredLongIds =
          LONG_ID_CONFIG_CACHE.getUnchecked( Pair.pair( identityArn, resource ) );
      if ( configuredLongIds.isPresent( ) ) {
        if ( configuredLongIds.get( ) ) {
          return ResourceIdentifiers.generateLongString( prefix );
        } else {
          return ResourceIdentifiers.generateShortString( prefix );
        }
      }
    }
    return ResourceIdentifiers.generateString( prefix );
  }

  public static boolean isValidResource( final String resource ) {
    return LONG_ID_RESOURCES.contains( resource );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static Optional<Pair<String,Pair<IdType,String>>> tryParseIdentity( final String identityArn ) {
    return tryParseIdentity( null, identityArn );
  }

  public static Optional<Pair<String,Pair<IdType,String>>> tryParseIdentity(
      final String accountNumber,
      final String identityArn
  ) {
    final Matcher accountRootArnMatcher = Pattern.compile( "arn:aws:iam::([0-9]{12}):root" ).matcher( identityArn );
    if ( accountRootArnMatcher.matches( ) &&
        ( accountNumber == null || accountNumber.equals( accountRootArnMatcher.group( 1 ) ) ) ) {
      return Optional.of( Pair.pair(
          accountRootArnMatcher.group( 1 ),
          Pair.pair( IdType.account, accountRootArnMatcher.group( 1 ) ) ) );
    }

    try {
      final Ern ern = Ern.parse( identityArn );
      if ( VENDOR_IAM.equals( ern.getService( ) ) && ern.getAccount( ) != null &&
          ( accountNumber == null || accountNumber.equals( ern.getAccount( ) ) ) ) {
        if ( PolicySpec.qualifiedName( VENDOR_IAM, IAM_RESOURCE_ROLE ).equals( ern.getResourceType( ) ) ) {
          return Optional.of( Pair.pair( ern.getAccount( ), Pair.pair( IdType.role, ern.getResourceName( ) ) ) );
        } else if ( PolicySpec.qualifiedName( VENDOR_IAM, IAM_RESOURCE_USER ).equals( ern.getResourceType( ) ) ) {
          return Optional.of( Pair.pair( ern.getAccount( ), Pair.pair( IdType.user, ern.getResourceName( ) ) ) );
        }
      }
    } catch ( Exception e ) {
      // not a valid user/role ARN
    }

    return Optional.absent( );
  }

  @SuppressWarnings( "WeakerAccess" )
  public static Optional<Boolean> loadIdFormat(
      final String accountNumber,
      final IdType type,
      final String id,
      final IdResource resource
  ) {
    //noinspection unused
    try ( final TransactionResource tx = Entities.transactionFor( IdentityIdFormat.class ) ) {
      final IdentityIdFormat idFormat = Entities.criteriaQuery( IdentityIdFormat.class )
          .whereEqual( IdentityIdFormat_.accountNumber, accountNumber )
          .whereEqual( IdentityIdFormat_.identityType, type )
          .whereEqual( IdentityIdFormat_.identityFullName, id )
          .whereEqual( IdentityIdFormat_.resource, resource )
          .uniqueResult( );
      return Optional.of( idFormat.getUseLongIdentifiers( ) );
    } catch ( final NoSuchElementException e ) {
      return Optional.absent( );
    }
  }

  public static boolean saveIdFormat(
      final String accountNumber,
      final IdType type,
      final String id,
      final IdResource resource,
      final Boolean useLongIds
  ) {
    try {
      Entities.asDistinctTransaction( IdentityIdFormat.class, new Function<Void, IdentityIdFormat>( ) {
        @Nullable
        @Override
        public IdentityIdFormat apply( @Nullable final Void aVoid ) {
          final IdentityIdFormat idFormat = Entities.criteriaQuery( IdentityIdFormat.class )
              .whereEqual( IdentityIdFormat_.accountNumber, accountNumber )
              .whereEqual( IdentityIdFormat_.identityType, type )
              .whereEqual( IdentityIdFormat_.identityFullName, id )
              .whereEqual( IdentityIdFormat_.resource, resource )
              .uniqueResult( );
          idFormat.setUseLongIdentifiers( useLongIds );
          return idFormat;
        }
      } ).apply( null );
    } catch ( NoSuchElementException e ) {
      // so create
      try ( final TransactionResource tx = Entities.transactionFor( IdentityIdFormat.class ) ) {
        Entities.persist( IdentityIdFormat.create(
            accountNumber,
            type,
            id,
            resource,
            useLongIds
        ) );
        tx.commit( );
      }
    }
    return true;
  }

  @SuppressWarnings( "WeakerAccess" )
  public static List<IdentityIdFormat> listIdFormats(
      final String accountNumber,
      final IdType type,
      final String id,
      final Optional<IdResource> resource
  ) {
    //noinspection unused
    try ( final TransactionResource tx = Entities.transactionFor( IdentityIdFormat.class ) ) {
      final EntityRestriction<IdentityIdFormat> resourceRestriction = resource.isPresent( ) ?
          Entities.restriction( IdentityIdFormat.class ).equal( IdentityIdFormat_.resource, resource.get( ) ).build( ) :
          Entities.restriction( IdentityIdFormat.class ).build( );
      return Entities.criteriaQuery( IdentityIdFormat.class )
          .whereEqual( IdentityIdFormat_.accountNumber, accountNumber )
          .whereEqual( IdentityIdFormat_.identityType, type )
          .whereEqual( IdentityIdFormat_.identityFullName, id )
          .where( resourceRestriction )
          .readonly( )
          .list( );
    }
  }

  public static List<IdentityIdFormat> listIdFormatsWithDefaults(
      final String accountNumber,
      final IdType type,
      final String id,
      final Optional<IdResource> resource
  ) {
    final Set<IdResource> desiredResources = resource.isPresent( ) ?
        resource.asSet( ) :
        EnumSet.allOf( IdResource.class );
    //noinspection unused
    try ( final TransactionResource tx = Entities.transactionFor( IdentityIdFormat.class ) ) {
      final List<IdentityIdFormat> formats = Lists.newArrayList( listIdFormats( accountNumber, type, id, resource ) );
      final Set<IdResource> idSpecificResources =
          formats.stream( ).map( IdentityIdFormat::getResource ).collect( Collectors.toSet( ) );
      if ( type != IdType.account && !idSpecificResources.containsAll( desiredResources ) ) {
        formats.addAll(
            listIdFormats( accountNumber, IdType.account, accountNumber, resource ).stream( )
                .filter( accountFormat -> !idSpecificResources.contains( accountFormat.getResource( ) ) )
                .collect( Collectors.toList( ) ) );
      }
      final Set<IdResource> configuredResources =
          formats.stream( ).map( IdentityIdFormat::getResource ).collect( Collectors.toSet( ) );
      formats.addAll(
          desiredResources.stream( )
              .filter( configurableResource -> !configuredResources.contains( configurableResource ) )
              .map( configurableResource -> IdentityIdFormat.create(
                  accountNumber,
                  IdType.account,
                  accountNumber,
                  configurableResource,
                  ResourceIdentifiers.useLongIdentifierForPrefix(
                      LONG_ID_RESOURCE_TO_PREFIX.get( configurableResource.name( ) )
                  )
              ) ).collect( Collectors.toList( ) ) );
      formats.sort( Comparator.comparing( IdentityIdFormat::getResource ) );
      return formats;
    }
  }

  @TypeMapper
  public enum IdentityIdFormatToIdFormatItemType implements Function<IdentityIdFormat,IdFormatItemType> {
    INSTANCE;

    @Nullable
    @Override
    public IdFormatItemType apply( @Nullable final IdentityIdFormat format ) {
      return format == null ? null : new IdFormatItemType(
          Objects.toString( format.getResource( ) ),
          format.getUseLongIdentifiers( )
      );
    }
  }
}
