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

import java.util.List;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import javaslang.collection.Stream;

public class AccountAttributeSetItemType extends EucalyptusData {

  private String attributeName;
  private AccountAttributeValueSetType attributeValueSet;

  public AccountAttributeSetItemType( ) {
  }

  public AccountAttributeSetItemType( String name, List<String> values ) {
    attributeName = name;
    attributeValueSet = new AccountAttributeValueSetType( Stream.ofAll( values ).map( AccountAttributeValueSetItemType.forValue( ) ).toJavaList( ) );
  }

  public String getAttributeName( ) {
    return attributeName;
  }

  public void setAttributeName( String attributeName ) {
    this.attributeName = attributeName;
  }

  public AccountAttributeValueSetType getAttributeValueSet( ) {
    return attributeValueSet;
  }

  public void setAttributeValueSet( AccountAttributeValueSetType attributeValueSet ) {
    this.attributeValueSet = attributeValueSet;
  }
}
