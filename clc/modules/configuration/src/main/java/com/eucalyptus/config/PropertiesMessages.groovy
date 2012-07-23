/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.config

import com.eucalyptus.component.ComponentId.ComponentMessage
import com.eucalyptus.empyrean.Empyrean.PropertiesService
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData

public class Property extends EucalyptusData {
  String name;
  String value;
  String description;
  public Property( String name, String value, String description ) {
    super( );
    this.name = name;
    this.value = value;
    this.description = description;
  }
}
@ComponentMessage(PropertiesService.class)
public class PropertiesMessage extends BaseMessage {}
public class DescribePropertiesType extends PropertiesMessage {
  ArrayList<String> properties = new ArrayList<String>();
}
public class DescribePropertiesResponseType extends PropertiesMessage {
  ArrayList<Property> properties = new ArrayList<Property>();
}
public class ModifyPropertyValueType extends PropertiesMessage {
  String name;
  String value;
  Boolean reset;
}
public class ModifyPropertyValueResponseType extends PropertiesMessage {
  String name;
  String value;
  String oldValue;
}
