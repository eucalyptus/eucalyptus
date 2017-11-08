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
package com.eucalyptus.cloudformation.resources.standard.info;

import java.util.Collection;
import java.util.Map;
import com.eucalyptus.cloudformation.CloudFormationException;
import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AWSCloudFormationStackResourceInfo extends ResourceInfo {

  public static final String EUCA_DELETE_STATUS_UPDATE_COMPLETE_CLEANUP_IN_PROGRESS = "Euca.DeleteStatusUpdateCompleteCleanupInProgress";
  public static final String EUCA_DELETE_STATUS_UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS = "Euca.DeleteStatusUpdateCompleteCleanupInProgress";
  private Map<String, String> outputAttributes = Maps.newLinkedHashMap( );
  private Map<String, String> eucaAttributes = Maps.newLinkedHashMap( );

  public AWSCloudFormationStackResourceInfo( ) {
    setType( "AWS::CloudFormation::Stack" );
  }

  @Override
  public boolean isAttributeAllowed( String attributeName ) {
    return attributeName != null && ( attributeName.startsWith( "Euca." ) || attributeName.startsWith( "Outputs." ) );
  }

  @Override
  public String getResourceAttributeJson( String attributeName ) throws CloudFormationException {
    if ( outputAttributes.containsKey( attributeName ) ) {
      return outputAttributes.get( attributeName );
    } else if ( eucaAttributes.containsKey( attributeName ) ) {
      return eucaAttributes.get( attributeName );
    } else {
      throw new ValidationErrorException( "Stack does not have an attribute named " + attributeName );
    }

  }

  @Override
  public void setResourceAttributeJson( String attributeName, String attributeValueJson ) throws CloudFormationException {
    if ( attributeName == null || ( !attributeName.startsWith( "Outputs." ) && !attributeName.startsWith( "Euca." ) ) ) {
      throw new ValidationErrorException( "Stack can not have an attribute named " + attributeName );
    } else if ( attributeName.startsWith( "Outputs." ) ) {
      outputAttributes.put( attributeName, attributeValueJson );
    } else if ( attributeName.startsWith( "Euca." ) ) {
      eucaAttributes.put( attributeName, attributeValueJson );
    }

  }

  @Override
  public Collection<String> getAttributeNames( ) throws CloudFormationException {
    Collection<String> copy = Lists.newArrayList( outputAttributes.keySet( ) );
    copy.addAll( eucaAttributes.keySet( ) );
    return copy;
  }

  public Map<String, String> getOutputAttributes( ) {
    return outputAttributes;
  }

  public void setOutputAttributes( Map<String, String> outputAttributes ) {
    this.outputAttributes = outputAttributes;
  }

  public Map<String, String> getEucaAttributes( ) {
    return eucaAttributes;
  }

  public void setEucaAttributes( Map<String, String> eucaAttributes ) {
    this.eucaAttributes = eucaAttributes;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "outputAttributes", outputAttributes )
        .add( "eucaAttributes", eucaAttributes )
        .toString( );
  }
}
