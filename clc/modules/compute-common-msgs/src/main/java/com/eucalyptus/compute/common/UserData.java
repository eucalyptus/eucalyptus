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

import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class UserData extends EucalyptusData {

  @HttpParameterMapping( parameter = { "UserData", "UserData.Data" } )
  private String data;
  private String version = "1.0";
  private String encoding = "base64";

  public String getData( ) {
    return data;
  }

  public void setData( String data ) {
    this.data = data;
  }

  public String getVersion( ) {
    return version;
  }

  public void setVersion( String version ) {
    this.version = version;
  }

  public String getEncoding( ) {
    return encoding;
  }

  public void setEncoding( String encoding ) {
    this.encoding = encoding;
  }
}
