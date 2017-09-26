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

public class RegisterServiceType extends ServiceRegistrationMessage {

  private String type;
  private String partition;
  private String name;
  private String host;
  private Integer port;

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

  public String getHost( ) {
    return host;
  }

  public void setHost( String host ) {
    this.host = host;
  }

  public Integer getPort( ) {
    return port;
  }

  public void setPort( Integer port ) {
    this.port = port;
  }
}
