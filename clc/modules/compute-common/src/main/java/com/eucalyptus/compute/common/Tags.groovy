/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import edu.ucsb.eucalyptus.msgs.HasTags

import javax.annotation.Nonnull
import javax.annotation.Nullable

public class ResourceTag extends EucalyptusData {
  String key;
  String value = ""

  public ResourceTag() {
  }

  public ResourceTag( String key, String value ) {
    this.key = key;
    this.value = value;
  }
}

public class ResourceTagSpecification extends EucalyptusData {
  String resourceType
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>()
}

public class DeleteResourceTag extends EucalyptusData {
  String key;   // optional
  String value; // optional

  DeleteResourceTag() {
  }

  DeleteResourceTag( String key ) {
    this.key = key;
  }
}

public class ResourceTagMessage extends ComputeMessage {
  public ResourceTagMessage( ) { }

  public ResourceTagMessage( ComputeMessage msg ) {
    super( msg );
  }

  public ResourceTagMessage( String userId ) {
    super( userId );
  }
}

public class DescribeTagsType extends ResourceTagMessage  {
  Integer maxResults
  String nextToken
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  ArrayList<Filter> filterSet = new ArrayList<Filter>();
}

public class DescribeTagsResponseType extends ResourceTagMessage  {
  ArrayList<TagInfo> tagSet = new ArrayList<TagInfo>( );
}

public class DeleteTagsResponseType extends ResourceTagMessage {
}

public class DeleteTagsType extends ResourceTagMessage implements HasTags {
  @HttpParameterMapping( parameter = "ResourceId" )
  ArrayList<String> resourcesSet = new ArrayList<String>();
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  ArrayList<DeleteResourceTag> tagSet = new ArrayList<DeleteResourceTag>();

  @Override
  Set<String> getTagKeys( @Nullable String resourceType, @Nullable String resourceId ) {
    Set<String> keys = []
    tagSet?.each{ DeleteResourceTag deleteTag -> if ( deleteTag.key ) keys << deleteTag.key }
    keys
  }

  @Override
  String getTagValue( @Nullable String resourceType, @Nullable String resourceId, @Nonnull final String tagKey ) {
    tagSet?.find{ DeleteResourceTag deleteTag -> tagKey == deleteTag.key }?.value
  }
}

public class TagInfo extends EucalyptusData {
  String resourceId;
  String resourceType;
  String key;
  String value;
}

public class CreateTagsResponseType extends ResourceTagMessage  {
}

public class CreateTagsType extends ResourceTagMessage implements HasTags {
  @HttpParameterMapping( parameter = "ResourceId" )
  ArrayList<String> resourcesSet = new ArrayList<String>();
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();

  @Override
  Set<String> getTagKeys( @Nullable String resourceType, @Nullable String resourceId ) {
    Set<String> keys = []
    tagSet?.each{ ResourceTag resourceTag -> if ( resourceTag.key ) keys << resourceTag.key }
    keys
  }

  @Override
  String getTagValue( @Nullable String resourceType, @Nullable String resourceId, @Nonnull final String tagKey ) {
    tagSet?.find{ ResourceTag resourceTag -> tagKey == resourceTag.key }?.value
  }
}
