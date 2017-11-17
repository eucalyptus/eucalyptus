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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.eucalyptus.component.annotation.ComponentMessage;
import com.google.common.collect.Sets;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 *
 */
@ComponentMessage( Compute.class )
public class ComputeMessage extends BaseMessage implements Cloneable, Serializable {

  public ComputeMessage( ) {
  }

  public ComputeMessage( ComputeMessage msg ) {
    this( );
    regarding( msg );
    regardingUserRequest( msg );
    this.setUserId( msg.getUserId( ) );
    this.setEffectiveUserId( msg.getEffectiveUserId( ) );
    this.setCorrelationId( msg.getCorrelationId( ) );
  }

  public ComputeMessage( final String userId ) {
    this( );
    this.setUserId( userId );
    this.setEffectiveUserId( userId );
  }

  protected Set<String> getTagKeys( List<ResourceTagSpecification> tagSpecifications, String resourceType, String resourceId ) {
    final Set<String> tagKeys = Sets.newLinkedHashSet( );
    if ( tagSpecifications != null && resourceId == null ) {
      for ( final ResourceTagSpecification tagSpecification : tagSpecifications ) {
        if ( tagSpecification.getTagSet( ) != null && tagSpecification.getResourceType( ) != null && Objects.equals( tagSpecification.getResourceType( ), resourceType ) ) {
          for ( final ResourceTag tag : tagSpecification.getTagSet( ) ) {
            tagKeys.add( tag.getKey( ) );
          }

        }

      }

    }

    return tagKeys;
  }

  protected String getTagValue( List<ResourceTagSpecification> tagSpecifications, String resourceType, String resourceId, String tagKey ) {
    String value = null;
    if ( tagSpecifications != null && resourceId == null ) {
      specifications:
      for ( final ResourceTagSpecification tagSpecification : tagSpecifications ) {
        if ( tagSpecification.getTagSet( ) != null && tagSpecification.getResourceType( ) != null && Objects.equals( tagSpecification.getResourceType( ), resourceType ) ) {
          for ( final ResourceTag tag : tagSpecification.getTagSet( ) ) {
            if ( Objects.equals( tag.getKey( ), tagKey ) ) {
              value = tag.getValue( );
              break specifications;
            }

          }

        }

      }

    }

    return value;
  }

}
