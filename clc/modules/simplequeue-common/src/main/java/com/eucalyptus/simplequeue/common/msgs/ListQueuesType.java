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
package com.eucalyptus.simplequeue.common.msgs;

import java.util.ArrayList;
import com.eucalyptus.binding.HttpEmbedded;

public class ListQueuesType extends SimpleQueueMessage {

  private String queueNamePrefix;
  @HttpEmbedded( multiple = true )
  private ArrayList<Attribute> attribute = new ArrayList<Attribute>( );

  public String getQueueNamePrefix( ) {
    return queueNamePrefix;
  }

  public void setQueueNamePrefix( String queueNamePrefix ) {
    this.queueNamePrefix = queueNamePrefix;
  }

  public ArrayList<Attribute> getAttribute( ) {
    return attribute;
  }

  public void setAttribute( ArrayList<Attribute> attribute ) {
    this.attribute = attribute;
  }
}
