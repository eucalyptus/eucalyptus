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

import java.util.Collection;
import com.eucalyptus.util.CompatFunction;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class DhcpOptionsType extends EucalyptusData implements VpcTagged {

  private String dhcpOptionsId;
  private DhcpConfigurationItemSetType dhcpConfigurationSet;
  private ResourceTagSetType tagSet;

  public DhcpOptionsType( ) {
  }

  public DhcpOptionsType( String dhcpOptionsId, Collection<DhcpConfigurationItemType> configuration ) {
    this.dhcpOptionsId = dhcpOptionsId;
    this.dhcpConfigurationSet = new DhcpConfigurationItemSetType( configuration );
  }

  public static CompatFunction<DhcpOptionsType, String> id( ) {
    return DhcpOptionsType::getDhcpOptionsId;
  }

  public String getDhcpOptionsId( ) {
    return dhcpOptionsId;
  }

  public void setDhcpOptionsId( String dhcpOptionsId ) {
    this.dhcpOptionsId = dhcpOptionsId;
  }

  public DhcpConfigurationItemSetType getDhcpConfigurationSet( ) {
    return dhcpConfigurationSet;
  }

  public void setDhcpConfigurationSet( DhcpConfigurationItemSetType dhcpConfigurationSet ) {
    this.dhcpConfigurationSet = dhcpConfigurationSet;
  }

  public ResourceTagSetType getTagSet( ) {
    return tagSet;
  }

  public void setTagSet( ResourceTagSetType tagSet ) {
    this.tagSet = tagSet;
  }
}
