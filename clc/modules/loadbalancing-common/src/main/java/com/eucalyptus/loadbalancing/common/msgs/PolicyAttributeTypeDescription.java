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
package com.eucalyptus.loadbalancing.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class PolicyAttributeTypeDescription extends EucalyptusData {

  private static final long serialVersionUID = 1L;
  private String attributeName;
  private String attributeType;
  private String description;
  private String defaultValue;
  private String cardinality;

  public PolicyAttributeTypeDescription( ) {
  }

  public String getAttributeName( ) {
    return attributeName;
  }

  public void setAttributeName( String attributeName ) {
    this.attributeName = attributeName;
  }

  public String getAttributeType( ) {
    return attributeType;
  }

  public void setAttributeType( String attributeType ) {
    this.attributeType = attributeType;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public String getDefaultValue( ) {
    return defaultValue;
  }

  public void setDefaultValue( String defaultValue ) {
    this.defaultValue = defaultValue;
  }

  public String getCardinality( ) {
    return cardinality;
  }

  public void setCardinality( String cardinality ) {
    this.cardinality = cardinality;
  }
}
