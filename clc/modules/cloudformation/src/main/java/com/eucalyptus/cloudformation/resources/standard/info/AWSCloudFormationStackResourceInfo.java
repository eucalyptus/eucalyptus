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
