/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.tags;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.ResourceTagSpecification;
import com.eucalyptus.compute.common.internal.tags.TagSupport;
import com.eucalyptus.compute.common.internal.util.MetadataException;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.util.Assert;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 */
public class TagHelper {

  private static final Set<String> reservedPrefixes =
      ImmutableSet.<String>builder().add("aws:").add("euca:").build();

  /**
   * Check if the tag is reserved in the given context.
   *
   * @param tagName The tag name to check.
   * @return true if reserved (not permitted for C_UD)
   * @see Contexts#lookup
   */
  public static boolean isReserved( final String tagName ) {
    return
        !Contexts.lookup( ).isPrivileged( ) &&
        Iterables.any( reservedPrefixes, prefix( tagName ) );
  }

  /**
   * Reserved tag key description
   */
  @Nonnull
  public static String describeReserved( ) {
    return String.valueOf( reservedPrefixes );
  }

  /**
   * Validate any key/value pairs present in the given specification for the current context.
   *
   * @param tagSpecifications The optional tag specification to validate
   * @throws MetadataException
   */
  public static void validateTagSpecifications(
      @Nullable final List<ResourceTagSpecification> tagSpecifications
  ) throws MetadataException {
    if ( tagSpecifications != null ) {
      final Set<String> resourceTypes = Sets.newHashSet( );
      for ( final ResourceTagSpecification tagSpecification : tagSpecifications ) {
        if ( tagSpecification.getResourceType( ) != null &&
            !resourceTypes.add( tagSpecification.getResourceType( ) ) ) {
          throw new InvalidTagMetadataException(
              "The same resource type may not be specified more than once in tag specifications" );
        }
        TagHelper.validateTags( tagSpecification.getTagSet( ) );
      }
    }
  }

  /**
   * Validate the given list of tags for the current context.
   *
   * @param resourceTags
   * @throws MetadataException
   */
  public static void validateTags(
      @Nullable final List<ResourceTag> resourceTags
  ) throws MetadataException {
    final Set<String> tagKeys = Sets.newHashSet( );
    if ( resourceTags != null ) for ( final ResourceTag resourceTag : resourceTags ) {
      final String key = resourceTag.getKey();
      final String value = Strings.nullToEmpty( resourceTag.getValue() ).trim();

      if ( isReserved( key ) ) {
        throw new InvalidTagMetadataException( "Tag keys starting with 'aws:' and 'euca:' are reserved for internal use" );
      }

      if ( Strings.isNullOrEmpty( key ) || key.trim().length() > 127 ) {
        throw new InvalidTagMetadataException( "Tag key exceeds the maximum length of 127 characters" );
      }

      if ( value.length() > 255 ) {
        throw new InvalidTagMetadataException( "Tag value exceeds the maximum length of 255 characters" );
      }

      if ( !tagKeys.add( key ) ) {
        throw new InvalidTagMetadataException( "Duplicate tag key" );
      }
    }
  }

  /**
   * Get the tags for the specified resource.
   *
   * @param tagSpecifications The optional specifications
   * @param resource The resource type
   * @return tags for the resource type (may be empty)
   */
  @Nonnull
  public static List<ResourceTag> tagsForResource(
      @Nullable final List<ResourceTagSpecification> tagSpecifications,
      @Nonnull  final String resource
  ) {
    Assert.arg( PolicySpec.EC2_RESOURCES.contains( resource ), "Invalid EC2 resource: %1$s", resource );
    final List<ResourceTag> tags = Lists.newArrayList( );
    if ( tagSpecifications != null ) for( final ResourceTagSpecification tagSpecification : tagSpecifications ) {
      final List<ResourceTag> resourceTags = tagSpecification.getTagSet( );
      if ( resourceTags != null && resource.equals( tagSpecification.getResourceType( ) ) ) {
        tags.addAll( resourceTags );
      }
    }
    return tags;
  }

  /**
   * Caller must have open transaction for resources.
   *
   * Caller must check permissions.
   */
  public static void createOrUpdateTags(
      @Nonnull final UserFullName userFullName,
      @Nonnull final CloudMetadata resource,
      @Nonnull final List<ResourceTag> resourceTags
  ) {
    createOrUpdateTags( userFullName, Lists.newArrayList( resource ), resourceTags );
  }

  /**
   * Caller must have open transaction for resources.
   *
   * Caller must check permissions.
   */
  public static void createOrUpdateTags(
      @Nonnull final UserFullName userFullName,
      @Nonnull final List<CloudMetadata> resources,
      @Nonnull final List<ResourceTag> resourceTags
  ) {
    for ( final CloudMetadata resource : resources ) {
      for ( final ResourceTag resourceTag : resourceTags ) {
        final String key = Strings.nullToEmpty( resourceTag.getKey() ).trim();
        final String value = Strings.nullToEmpty( resourceTag.getValue() ).trim();
        TagSupport.fromResource( resource ).createOrUpdate( resource, userFullName, key, value );
      }

      if ( TagSupport.fromResource( resource ).count( resource, userFullName.asAccountFullName( ) ) >
          TagManager.MAX_TAGS_PER_RESOURCE ) {
        throw new TagLimitException();
      }
    }

  }

  public static boolean createTagsAuthorized(
      @Nonnull final Context ctx,
      @Nonnull final String resourceType ) {
    return createTagsAuthorized( ctx.getAuthContext( ), ctx.getAccount( ), resourceType );
  }

  public static boolean createTagsAuthorized(
      @Nonnull final AuthContextSupplier authContext,
      @Nonnull final AccountFullName accountFullName,
      @Nonnull final String resourceType
  ) {
    return Permissions.isAuthorized(
        PolicySpec.VENDOR_EC2,
        resourceType,
        "",
        accountFullName,
        PolicySpec.EC2_CREATETAGS,
        authContext );
  }

  private static Predicate<String> prefix( final String text ) {
    return new Predicate<String>() {
      @Override
      public boolean apply( final String prefix ) {
        return text != null && text.trim().startsWith( prefix );
      }
    };
  }


}
