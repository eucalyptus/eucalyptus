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

@GroovyAddClassUUID
package com.eucalyptus.compute.common;

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.binding.HttpEmbedded
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

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

public class DeleteTagsType extends ResourceTagMessage {
  @HttpParameterMapping( parameter = "ResourceId" )
  ArrayList<String> resourcesSet = new ArrayList<String>();
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  ArrayList<DeleteResourceTag> tagSet = new ArrayList<DeleteResourceTag>();
}

public class TagInfo extends EucalyptusData {
  String resourceId;
  String resourceType;
  String key;
  String value;
}

public class CreateTagsResponseType extends ResourceTagMessage  {
}

public class CreateTagsType extends ResourceTagMessage  {
  @HttpParameterMapping( parameter = "ResourceId" )
  ArrayList<String> resourcesSet = new ArrayList<String>();
  @HttpParameterMapping( parameter = "Tag" )
  @HttpEmbedded( multiple = true )
  ArrayList<ResourceTag> tagSet = new ArrayList<ResourceTag>();
}
