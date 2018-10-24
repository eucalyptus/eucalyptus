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
