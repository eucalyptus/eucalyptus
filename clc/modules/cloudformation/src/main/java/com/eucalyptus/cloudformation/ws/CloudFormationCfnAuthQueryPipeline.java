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
package com.eucalyptus.cloudformation.ws;

import com.eucalyptus.cloudformation.CloudFormation;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.stages.UnrollableStage;


@ComponentPart(CloudFormation.class)
public class CloudFormationCfnAuthQueryPipeline extends CloudFormationQueryPipeline {
  private final CloudFormationCfnAuthenticationStage auth = new CloudFormationCfnAuthenticationStage( );

  public CloudFormationCfnAuthQueryPipeline( ) {
    super( "cloudformation-query-cfn-pipeline" );
  }

  @Override
  protected UnrollableStage getAuthenticationStage( ) {
    return auth;
  }

  @Override
  protected boolean validAuthForPipeline( final MappingHttpRequest request ) {
    return !super.validAuthForPipeline( request );
  }
}
