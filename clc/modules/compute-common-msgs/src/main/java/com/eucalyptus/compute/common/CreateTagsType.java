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
package com.eucalyptus.compute.common;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.HasTags;

public class CreateTagsType extends ResourceTagMessage implements HasTags {

  @HttpParameterMapping( parameter = "ResourceId" )
  private ArrayList<String> resourcesSet = new ArrayList<String>( );
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  private ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>( );

  @Override
  public Set<String> getTagKeys( @Nullable String resourceType, @Nullable String resourceId ) {
    Set<String> keys = Sets.newLinkedHashSet( );
    for ( final ResourceTag resourceTag : tagSet ) {
      if ( resourceTag.getKey( ) != null && !resourceTag.getKey( ).isEmpty( ) ) {
        keys.add( resourceTag.getKey( ) );
      }

    }

    return keys;
  }

  @Override
  public String getTagValue( @Nullable String resourceType, @Nullable String resourceId, @Nonnull final String tagKey ) {
    String value = null;
    for ( final ResourceTag resourceTag : tagSet ) {
      if ( Objects.equals( resourceTag.getKey( ), tagKey ) ) {
        value = resourceTag.getValue( );
        break;
      }

    }

    return value;
  }

  public ArrayList<String> getResourcesSet( ) {
    return resourcesSet;
  }

  public void setResourcesSet( ArrayList<String> resourcesSet ) {
    this.resourcesSet = resourcesSet;
  }

  public ArrayList<ResourceTag> getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ArrayList<ResourceTag> tagSet ) {
    this.tagSet = tagSet;
  }
}
