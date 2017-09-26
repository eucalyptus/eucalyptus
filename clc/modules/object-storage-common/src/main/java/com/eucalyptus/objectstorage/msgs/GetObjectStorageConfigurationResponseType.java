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
package com.eucalyptus.objectstorage.msgs;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class GetObjectStorageConfigurationResponseType extends ObjectStorageRequestType {

  private String name;
  private ArrayList<ComponentProperty> properties;

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public ArrayList<ComponentProperty> getProperties( ) {
    return properties;
  }

  public void setProperties( ArrayList<ComponentProperty> properties ) {
    this.properties = properties;
  }
}
