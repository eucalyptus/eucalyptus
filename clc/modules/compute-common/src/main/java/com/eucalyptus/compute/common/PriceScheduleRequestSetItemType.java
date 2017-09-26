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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class PriceScheduleRequestSetItemType extends EucalyptusData {

  private Long term;
  private Double price;
  private String currencyCode;

  public Long getTerm( ) {
    return term;
  }

  public void setTerm( Long term ) {
    this.term = term;
  }

  public Double getPrice( ) {
    return price;
  }

  public void setPrice( Double price ) {
    this.price = price;
  }

  public String getCurrencyCode( ) {
    return currencyCode;
  }

  public void setCurrencyCode( String currencyCode ) {
    this.currencyCode = currencyCode;
  }
}
