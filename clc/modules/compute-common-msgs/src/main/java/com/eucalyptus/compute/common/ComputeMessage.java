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
