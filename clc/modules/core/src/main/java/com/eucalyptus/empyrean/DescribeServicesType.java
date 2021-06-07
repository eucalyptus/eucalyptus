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
