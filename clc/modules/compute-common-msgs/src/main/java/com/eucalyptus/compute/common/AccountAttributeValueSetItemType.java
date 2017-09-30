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

import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AccountAttributeValueSetItemType extends EucalyptusData {

  private String attributeValue;

  public static CompatFunction<String, AccountAttributeValueSetItemType> forValue( ) {
    return new CompatFunction<String, AccountAttributeValueSetItemType>( ) {
      @Override
      public AccountAttributeValueSetItemType apply( final String s ) {
        AccountAttributeValueSetItemType item = new AccountAttributeValueSetItemType( );
        item.setAttributeValue( s );
        return item;
      }
    };
  }

  public String getAttributeValue( ) {
    return attributeValue;
  }

  public void setAttributeValue( String attributeValue ) {
    this.attributeValue = attributeValue;
  }
}
