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
package com.eucalyptus.empyrean;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.binding.HttpParameterMapping;
import com.google.common.collect.Lists;

public class DescribeServicesType extends ServiceTransitionType {

  private Boolean listAll;
  private Boolean listInternal;
  private Boolean listUserServices;
  private Boolean showEvents;
  private Boolean showEventStacks;
  private String byServiceType;
  private String byHost;
  private String byState;
  private String byPartition;
  @HttpParameterMapping( parameter = "ServiceName" )
  private ArrayList<String> serviceNames = new ArrayList( );
  @HttpParameterMapping( parameter = "Filter" )
  @HttpEmbedded( multiple = true )
  private ArrayList<Filter> filters = Lists.newArrayList( );

  public Boolean getListAll( ) {
    return listAll;
  }

  public void setListAll( Boolean listAll ) {
    this.listAll = listAll;
  }

  public Boolean getListInternal( ) {
    return listInternal;
  }

  public void setListInternal( Boolean listInternal ) {
    this.listInternal = listInternal;
  }

  public Boolean getListUserServices( ) {
    return listUserServices;
  }

  public void setListUserServices( Boolean listUserServices ) {
    this.listUserServices = listUserServices;
  }

  public Boolean getShowEvents( ) {
    return showEvents;
  }

  public void setShowEvents( Boolean showEvents ) {
    this.showEvents = showEvents;
  }

  public Boolean getShowEventStacks( ) {
    return showEventStacks;
  }

  public void setShowEventStacks( Boolean showEventStacks ) {
    this.showEventStacks = showEventStacks;
  }

  public String getByServiceType( ) {
    return byServiceType;
  }

  public void setByServiceType( String byServiceType ) {
    this.byServiceType = byServiceType;
  }

  public String getByHost( ) {
    return byHost;
  }

  public void setByHost( String byHost ) {
    this.byHost = byHost;
  }

  public String getByState( ) {
    return byState;
  }

  public void setByState( String byState ) {
    this.byState = byState;
  }

  public String getByPartition( ) {
    return byPartition;
  }

  public void setByPartition( String byPartition ) {
    this.byPartition = byPartition;
  }

  public ArrayList<String> getServiceNames( ) {
    return serviceNames;
  }

  public void setServiceNames( ArrayList<String> serviceNames ) {
    this.serviceNames = serviceNames;
  }

  public ArrayList<Filter> getFilters( ) {
    return filters;
  }

  public void setFilters( ArrayList<Filter> filters ) {
    this.filters = filters;
  }
}
