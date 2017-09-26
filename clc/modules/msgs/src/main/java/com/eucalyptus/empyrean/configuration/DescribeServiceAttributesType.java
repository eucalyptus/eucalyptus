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

public class DescribeServiceAttributesType extends ServiceRegistrationMessage {

  private Boolean verbose = Boolean.FALSE;
  private Boolean reset = Boolean.FALSE;
  private String type;
  private String partition;
  private String name;

  public Boolean getVerbose( ) {
    return verbose;
  }

  public void setVerbose( Boolean verbose ) {
    this.verbose = verbose;
  }

  public Boolean getReset( ) {
    return reset;
  }

  public void setReset( Boolean reset ) {
    this.reset = reset;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public String getPartition( ) {
    return partition;
  }

  public void setPartition( String partition ) {
    this.partition = partition;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }
}
