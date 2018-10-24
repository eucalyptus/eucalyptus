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
package com.eucalyptus.tags;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.ImageMetadata;
import com.eucalyptus.compute.common.internal.tags.Tag;
import com.eucalyptus.compute.common.internal.tags.TagSupport;
import com.eucalyptus.compute.common.internal.tags.Tags;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.compute.ClientComputeException;
import com.eucalyptus.compute.ComputeException;
import com.eucalyptus.compute.common.internal.identifier.ResourceIdentifiers;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.compute.common.internal.images.ImageInfo;
import com.eucalyptus.images.Images;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.type.RestrictedType;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.InvalidParameterValueException;
import com.eucalyptus.compute.common.backend.CreateTagsResponseType;
import com.eucalyptus.compute.common.backend.CreateTagsType;
import com.eucalyptus.compute.common.backend.DeleteTagsResponseType;
import com.eucalyptus.compute.common.backend.DeleteTagsType;
import com.eucalyptus.compute.common.DeleteResourceTag;
import com.eucalyptus.compute.common.ResourceTag;

/**
 * Service implementation for Tag operations
 */
@ConfigurableClass( root = "tagging", description = "Parameters controlling tagging")
@ComponentNamed("computeTagManager")
public class TagManager {
  private static final Logger log = Logger.getLogger( TagManager.class );
  
  @ConfigurableField(initial = "10", description = "The maximum number of tags per resource for each account")
  public static long MAX_TAGS_PER_RESOURCE = 50;

  public CreateTagsResponseType createTags( final CreateTagsType request ) throws EucalyptusCloudException {
    final CreateTagsResponseType reply = request.getReply( );
    reply.set_return( false );

    final Context context = Contexts.lookup();
    final UserFullName userFullName = context.getUserFullName();
    final AccountFullName accountFullName = userFullName.asAccountFullName();
    final List<String> resourceIds = MoreObjects.firstNonNull( request.getResourcesSet(), Collections.<String>emptyList() );
    final List<ResourceTag> resourceTags = MoreObjects.firstNonNull( request.getTagSet(), Collections.<ResourceTag>emptyList() );

    try {
      TagHelper.validateTags( resourceTags );
    } catch ( MetadataException e ) {
      throw new ClientComputeException( "InvalidParameterValue", e.getMessage( ) );
    }

    if ( resourceTags.size() > 0 && resourceIds.size() > 0 ) {      
      final Predicate<Void> creator = new Predicate<Void>(){
        @Override
        public boolean apply( final Void v ) {
          final List<CloudMetadata> resources = Lists.transform( resourceIds, resourceLookup(true) );
          if ( !Iterables.all( resources, Predicates.and( Predicates.notNull(), typeSpecificFilters(), permissionsFilter() ) )  ) {
            return false;
          }
          TagHelper.createOrUpdateTags( userFullName, resources, resourceTags );
          return true;
        }
      };

      try {
        reply.set_return( Entities.asTransaction( Tag.class, creator ).apply( null ) );
      } catch ( TagLimitException e ) {
        throw new ClientComputeException( "TagLimitExceeded", "The maximum number of Tags for a resource has been reached." );
      } catch ( RuntimeException e ) {
        handleException( e );
      }
    }
    
    return reply;
  }

  public DeleteTagsResponseType deleteTags( final DeleteTagsType request ) throws EucalyptusCloudException {
    final DeleteTagsResponseType reply = request.getReply( );
    reply.set_return( false );

    final Context context = Contexts.lookup();
    final OwnerFullName ownerFullName = context.getUserFullName().asAccountFullName();
    final List<String> resourceIds = MoreObjects.firstNonNull( request.getResourcesSet(), Collections.<String>emptyList() );
    final List<DeleteResourceTag> resourceTags = MoreObjects.firstNonNull( request.getTagSet(), Collections.<DeleteResourceTag>emptyList() );

    for ( final DeleteResourceTag resourceTag : resourceTags ) {
      final String key = resourceTag.getKey();
      if ( Strings.isNullOrEmpty( key ) || key.trim().length() > 128 || TagHelper.isReserved( key ) ) {
        throw new InvalidParameterValueException(
            "Invalid key (max length 128, must not be empty, reserved prefixes "+TagHelper.describeReserved()+"): "+key );
      }
    }

    if ( resourceIds.size() > 0 && resourceIds.size() > 0 ) {
      final Predicate<Void> delete = new Predicate<Void>(){
        @Override
        public boolean apply( final Void v ) {
          final Iterable<CloudMetadata> resources = Iterables.filter( Iterables.transform( resourceIds, resourceLookup(false) ), Predicates.notNull() );
          if ( !Iterables.all( resources, Predicates.and( Predicates.notNull(), typeSpecificFilters(), permissionsFilter() ) )  ) {
            return false;
          }
          for ( final CloudMetadata resource : resources ) {
            for ( final DeleteResourceTag resourceTag : resourceTags ) {
              try {
                final Tag example = TagSupport.fromResource( resource ).example( resource, ownerFullName, resourceTag.getKey(), resourceTag.getValue() );                
                Tags.delete( example );
              } catch ( NoSuchMetadataException e ) {
                log.trace( e );
              }
            }
          }

          return true;
        }
      };

      try {
        reply.set_return( Entities.asTransaction( Tag.class, delete ).apply( null ) );
      } catch ( RuntimeException e ) {
        handleException( e );
      }
    }

    return reply;
  }

  private static Predicate<Object> typeSpecificFilters() {
    return TypeSpecificFilters.INSTANCE;
  }

  private static Predicate<RestrictedType> permissionsFilter() {
    return PermissionsFilter.INSTANCE;
  }

  /**
   * A function to lookup cloud metadata by resource identifier.
   * 
   * The returned function may return null values. 
   */
  private static Function<String, CloudMetadata> resourceLookup( final boolean required ) {
    return new Function<String,CloudMetadata>() {
      @Override
      public CloudMetadata apply( final String resourceId ) {
        final TagSupport tagSupport = TagSupport.fromIdentifier( resourceId );
        try {
          if ( tagSupport != null && resourceId.matches( "[a-z]{1,32}-[0-9a-fA-F]{8}(?:[0-9a-fA-F]{9})?" )) {
            return tagSupport.lookup( ResourceIdentifiers.tryNormalize( ).apply( resourceId ) );
          } else {
            throw Exceptions.toUndeclared( new ClientComputeException(
                "InvalidID",
                String.format( "The ID '%s' is not valid", resourceId ) ) );
          }
        } catch ( TransactionException e ) {
          throw Exceptions.toUndeclared( e );
        } catch ( NoSuchElementException e ) {
          if ( required ) {
            throw Exceptions.toUndeclared( new ClientComputeException(
                tagSupport.getNotFoundErrorCode( ),
                String.format( tagSupport.getNotFoundFormatString( ), resourceId ) ) );
          }
        }
        return null;
      }
    };
  }

  private static void handleException( final RuntimeException e ) throws EucalyptusCloudException {
    final ComputeException computeException = Exceptions.findCause( e, ComputeException.class );
    if ( computeException != null ) {
      throw computeException;
    }
    throw e;
  }

  private static enum PermissionsFilter implements Predicate<RestrictedType> {
    INSTANCE;

    @Override
    public boolean apply( final RestrictedType metadata ) {
      if ( metadata instanceof ImageMetadata ) {
        return RestrictedTypes.filterPrivilegedWithoutOwner( ).apply( metadata );
      } else {
        return RestrictedTypes.filterPrivileged( ).apply( metadata );
      }
    }
  }

  /**
   * Access filtering using type specific approach (e.g. launch permissions for images)
   */
  private static enum TypeSpecificFilters implements Predicate<Object> {
    INSTANCE;

    private List<Predicate<Object>> predicates = ImmutableList.of(
        typedPredicate( Images.FilterPermissions.INSTANCE, ImageInfo.class )
    );

    private <PT> Predicate<Object> typedPredicate( final Predicate<? super PT> predicate,
                                                   final Class<PT> predicateTarget ) {
      return new Predicate<Object>() {
        @Override
        public boolean apply( @Nullable final Object object ) {
          return
              !predicateTarget.isInstance( object ) ||
              predicate.apply( predicateTarget.cast( object ) );
        }
      };
    }

    @Override
    public boolean apply( final Object object ) {
      return Predicates.and( predicates ).apply( object );
    }
  }

}
