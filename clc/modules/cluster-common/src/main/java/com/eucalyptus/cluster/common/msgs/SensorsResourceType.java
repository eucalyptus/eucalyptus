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
package com.eucalyptus.cluster.common.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SensorsResourceType extends EucalyptusData {

  private String resourceName;
  private String resourceType;
  private String resourceUuid;
  private ArrayList<MetricsResourceType> metrics = new ArrayList<MetricsResourceType>( );

  public String getResourceName( ) {
    return resourceName;
  }

  public void setResourceName( String resourceName ) {
    this.resourceName = resourceName;
  }

  public String getResourceType( ) {
    return resourceType;
  }

  public void setResourceType( String resourceType ) {
    this.resourceType = resourceType;
  }

  public String getResourceUuid( ) {
    return resourceUuid;
  }

  public void setResourceUuid( String resourceUuid ) {
    this.resourceUuid = resourceUuid;
  }

  public ArrayList<MetricsResourceType> getMetrics( ) {
    return metrics;
  }

  public void setMetrics( ArrayList<MetricsResourceType> metrics ) {
    this.metrics = metrics;
  }
}
