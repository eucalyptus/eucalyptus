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

import java.util.ArrayList;
import java.util.List;
import com.eucalyptus.binding.HttpEmbedded;
import com.eucalyptus.util.StreamUtil;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class DhcpConfigurationItemType extends EucalyptusData {

  private String key;
  @HttpEmbedded
  private DhcpValueSetType valueSet;

  public DhcpConfigurationItemType( ) {
  }

  public DhcpConfigurationItemType( String key, List<String> values ) {
    this.key = key;
    this.valueSet = new DhcpValueSetType( StreamUtil.ofAll( values ).map( DhcpValueType.forValue( ) ).toJavaList( ) );
  }

  public List<String> values( ) {
    ArrayList<String> values = Lists.newArrayList( );
    if ( valueSet != null && valueSet.getItem( ) != null ) {
      for ( DhcpValueType item : valueSet.getItem( ) ) {
        values.add( item.getValue( ) );
      }

    }

    return values;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public DhcpValueSetType getValueSet( ) {
    return valueSet;
  }

  public void setValueSet( DhcpValueSetType valueSet ) {
    this.valueSet = valueSet;
  }
}
