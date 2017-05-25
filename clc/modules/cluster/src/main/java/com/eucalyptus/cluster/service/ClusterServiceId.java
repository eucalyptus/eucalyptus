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
package com.eucalyptus.cluster.service;

import com.eucalyptus.cluster.common.ClusterController;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.ComponentApi;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.util.techpreview.TechPreview;

/**
 *
 */
@Partition( Eucalyptus.class )
@FaultLogPrefix( "services" )
@ComponentApi( ClusterController.class )
@TechPreview( enableByDefaultProperty = "com.eucalyptus.cluster.service.enable" )
@Description( "Cluster controller service" )
public class ClusterServiceId extends ComponentId {
  private static final long serialVersionUID = 1L;

  public ClusterServiceId( ) {
    super( "ClusterService" );
  }

  @Override
  public String getInternalServicePath( final String... pathParts ) {
    return "/internal/Cluster";
  }

  @Override
  public String getServicePath( final String... pathParts ) {
    return "/services/Cluster";
  }
}
