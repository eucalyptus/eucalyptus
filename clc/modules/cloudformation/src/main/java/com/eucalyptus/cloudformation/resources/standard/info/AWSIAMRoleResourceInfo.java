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

import java.util.ArrayList;
import java.util.Collection;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.eucalyptus.cloudformation.template.TemplateParser.Capabilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;

public class AWSIAMRoleResourceInfo extends ResourceInfo {

  @AttributeJson
  private String arn;

  public AWSIAMRoleResourceInfo( ) {
    setType( "AWS::IAM::Role" );
  }

  @Override
  public Collection<String> getRequiredCapabilities( JsonNode propertiesJson ) {
    ArrayList<String> capabilities = new ArrayList<String>( );
    // Hack....Really didn't want to have to depend on named properties
    if ( propertiesJson != null && propertiesJson.isObject( ) && propertiesJson.has( "RoleName" ) ) {
      capabilities.add( Capabilities.CAPABILITY_NAMED_IAM.toString( ) );
    } else {
      capabilities.add( Capabilities.CAPABILITY_IAM.toString( ) );
    }

    return capabilities;
  }

  public String getArn( ) {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "arn", arn )
        .toString( );
  }
}
