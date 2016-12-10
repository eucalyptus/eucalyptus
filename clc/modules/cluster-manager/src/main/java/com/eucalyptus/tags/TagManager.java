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
package com.eucalyptus.tags;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
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
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
  
  private static final Set<String> reservedPrefixes = 
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();
  
  @ConfigurableField(initial = "10", description = "The maximum number of tags per resource for each account")
  public static long MAX_TAGS_PER_RESOURCE = 10;

  public CreateTagsResponseType createTags( final CreateTagsType request ) throws EucalyptusCloudException {
    final CreateTagsResponseType reply = request.getReply( );
    reply.set_return( false );

    final Context context = Contexts.lookup();
    final UserFullName userFullName = context.getUserFullName();
    final AccountFullName accountFullName = userFullName.asAccountFullName();
    final List<String> resourceIds = Objects.firstNonNull( request.getResourcesSet(), Collections.<String>emptyList() );
    final List<ResourceTag> resourceTags = Objects.firstNonNull( request.getTagSet(), Collections.<ResourceTag>emptyList() );

    for ( final ResourceTag resourceTag : resourceTags ) {
      final String key = resourceTag.getKey();
      final String value = Strings.nullToEmpty( resourceTag.getValue() ).trim();

      if ( isReserved( key ) ) {
        throw new ClientComputeException( "InvalidParameterValue", "Tag keys starting with 'aws:' and 'euca:' are reserved for internal use" );
      }

      if ( Strings.isNullOrEmpty( key ) || key.trim().length() > 127 ) {
        throw new ClientComputeException( "InvalidParameterValue", "Tag key exceeds the maximum length of 127 characters" );
      }
      if ( value.length() > 255 ) {
        throw new ClientComputeException( "InvalidParameterValue", "Tag value exceeds the maximum length of 255 characters" );
      }
    }
    
    if ( resourceTags.size() > 0 && resourceIds.size() > 0 ) {      
      final Predicate<Void> creator = new Predicate<Void>(){
        @Override
        public boolean apply( final Void v ) {
          final List<CloudMetadata> resources = Lists.transform( resourceIds, resourceLookup(true) );
          if ( !Iterables.all( resources, Predicates.and( Predicates.notNull(), typeSpecificFilters(), permissionsFilter() ) )  ) {
            return false;
          }

          for ( final CloudMetadata resource : resources ) {
            for ( final ResourceTag resourceTag : resourceTags ) {
              final String key = Strings.nullToEmpty( resourceTag.getKey() ).trim();
              final String value = Strings.nullToEmpty( resourceTag.getValue() ).trim();
              TagSupport.fromResource( resource ).createOrUpdate( resource, userFullName, key, value );
            }

            if ( TagSupport.fromResource( resource ).count( resource, accountFullName ) > MAX_TAGS_PER_RESOURCE ) {
              throw new TagLimitException();
            }
          }

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
    final List<String> resourceIds = Objects.firstNonNull( request.getResourcesSet(), Collections.<String>emptyList() );
    final List<DeleteResourceTag> resourceTags = Objects.firstNonNull( request.getTagSet(), Collections.<DeleteResourceTag>emptyList() );

    for ( final DeleteResourceTag resourceTag : resourceTags ) {
      final String key = resourceTag.getKey();
      if ( Strings.isNullOrEmpty( key ) || key.trim().length() > 128 || isReserved( key ) ) {
        throw new InvalidParameterValueException( "Invalid key (max length 128, must not be empty, reserved prefixes "+reservedPrefixes+"): "+key );
      }
    }

    if ( resourceIds.size() > 0 && resourceIds.size() > 0 ) {
      final Predicate<Void> delete = new Predicate<Void>(){
        @Override
        public boolean apply( final Void v ) {
          final Iterable<CloudMetadata> resources = Iterables.filter( Iterables.transform( resourceIds, resourceLookup(false) ), Predicates.notNull() );
          for ( final CloudMetadata resource : resources ) {
            for ( final DeleteResourceTag resourceTag : resourceTags ) {
              try {
                final Tag example = TagSupport.fromResource( resource ).example( resource, ownerFullName, resourceTag.getKey(), resourceTag.getValue() );                
                if ( RestrictedTypes.filterPrivileged().apply( example ) ) {
                  Tags.delete( example );
                }
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

  private static boolean isReserved( final String text ) {
    return
        !Contexts.lookup( ).isPrivileged( ) &&
        Iterables.any( reservedPrefixes, prefix( text ) );
  }
  
  private static Predicate<String> prefix( final String text ) {
    return new Predicate<String>() {
      @Override
      public boolean apply( final String prefix ) {
        return text != null && text.trim().startsWith( prefix );
      }
    };  
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

  private static class TagLimitException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
