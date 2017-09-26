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
package com.eucalyptus.empyrean.configuration;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ServiceAttribute extends EucalyptusData {

  private String name;
  private String value;
  private String description;
  private Boolean readOnly;
  private Boolean required;
  private String defaultValue;
  private String scope;

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getValue( ) {
    return value;
  }

  public void setValue( String value ) {
    this.value = value;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Boolean getReadOnly( ) {
    return readOnly;
  }

  public void setReadOnly( Boolean readOnly ) {
    this.readOnly = readOnly;
  }

  public Boolean getRequired( ) {
    return required;
  }

  public void setRequired( Boolean required ) {
    this.required = required;
  }

  public String getDefaultValue( ) {
    return defaultValue;
  }

  public void setDefaultValue( String defaultValue ) {
    this.defaultValue = defaultValue;
  }

  public String getScope( ) {
    return scope;
  }

  public void setScope( String scope ) {
    this.scope = scope;
  }
}
