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

import java.util.ArrayList;
import com.google.common.collect.Lists;

public class PurchaseHostReservationType extends HostComputeMessage {

  private String clientToken;
  private String currencyCode;
  private ArrayList<String> hostIdSet = Lists.newArrayList( );
  private String limitPrice;
  private String offeringId;

  public String getClientToken( ) {
    return clientToken;
  }

  public void setClientToken( String clientToken ) {
    this.clientToken = clientToken;
  }

  public String getCurrencyCode( ) {
    return currencyCode;
  }

  public void setCurrencyCode( String currencyCode ) {
    this.currencyCode = currencyCode;
  }

  public ArrayList<String> getHostIdSet( ) {
    return hostIdSet;
  }

  public void setHostIdSet( ArrayList<String> hostIdSet ) {
    this.hostIdSet = hostIdSet;
  }

  public String getLimitPrice( ) {
    return limitPrice;
  }

  public void setLimitPrice( String limitPrice ) {
    this.limitPrice = limitPrice;
  }

  public String getOfferingId( ) {
    return offeringId;
  }

  public void setOfferingId( String offeringId ) {
    this.offeringId = offeringId;
  }
}
