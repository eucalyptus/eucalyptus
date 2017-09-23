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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class MessageAttributeValue extends EucalyptusData {

  private ArrayList<String> binaryListValue = new ArrayList<String>( );
  private ArrayList<String> stringListValue = new ArrayList<String>( );
  private String stringValue;
  private String binaryValue;
  private String dataType;

  public ArrayList<String> getBinaryListValue( ) {
    return binaryListValue;
  }

  public void setBinaryListValue( ArrayList<String> binaryListValue ) {
    this.binaryListValue = binaryListValue;
  }

  public ArrayList<String> getStringListValue( ) {
    return stringListValue;
  }

  public void setStringListValue( ArrayList<String> stringListValue ) {
    this.stringListValue = stringListValue;
  }

  public String getStringValue( ) {
    return stringValue;
  }

  public void setStringValue( String stringValue ) {
    this.stringValue = stringValue;
  }

  public String getBinaryValue( ) {
    return binaryValue;
  }

  public void setBinaryValue( String binaryValue ) {
    this.binaryValue = binaryValue;
  }

  public String getDataType( ) {
    return dataType;
  }

  public void setDataType( String dataType ) {
    this.dataType = dataType;
  }
}
