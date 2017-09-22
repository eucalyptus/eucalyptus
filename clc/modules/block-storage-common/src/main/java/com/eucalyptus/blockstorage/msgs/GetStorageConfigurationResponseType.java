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
package com.eucalyptus.blockstorage.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class GetStorageConfigurationResponseType extends StorageResponseType {

  private String name;
  private ArrayList<ComponentProperty> storageParams;

  public GetStorageConfigurationResponseType( ) { }

  public GetStorageConfigurationResponseType( String name, ArrayList<ComponentProperty> storageParams ) {
    this.name = name;
    this.storageParams = storageParams;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public ArrayList<ComponentProperty> getStorageParams( ) {
    return storageParams;
  }

  public void setStorageParams( ArrayList<ComponentProperty> storageParams ) {
    this.storageParams = storageParams;
  }
}
