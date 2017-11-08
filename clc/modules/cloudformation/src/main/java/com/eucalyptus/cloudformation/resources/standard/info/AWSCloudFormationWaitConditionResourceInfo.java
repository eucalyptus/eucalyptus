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

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

public class AWSCloudFormationWaitConditionResourceInfo extends ResourceInfo {

  @AttributeJson
  private String data;
  @AttributeJson
  private String eucaCreateStartTime;

  public AWSCloudFormationWaitConditionResourceInfo( ) {
    setType( "AWS::CloudFormation::WaitCondition" );
  }

  @Override
  public boolean supportsSignals( ) {
    return true;
  }

  public String getData( ) {
    return data;
  }

  public void setData( String data ) {
    this.data = data;
  }

  public String getEucaCreateStartTime( ) {
    return eucaCreateStartTime;
  }

  public void setEucaCreateStartTime( String eucaCreateStartTime ) {
    this.eucaCreateStartTime = eucaCreateStartTime;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "data", data )
        .add( "eucaCreateStartTime", eucaCreateStartTime )
        .toString( );
  }
}
