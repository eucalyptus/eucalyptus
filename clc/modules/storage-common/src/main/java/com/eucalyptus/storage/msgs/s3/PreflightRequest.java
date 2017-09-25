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
package com.eucalyptus.storage.msgs.s3;

import java.util.ArrayList;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class PreflightRequest extends EucalyptusData {

  private String origin;
  private String method;
  private ArrayList<String> requestHeaders;

  public String toString( ) {
    StringBuffer output = new StringBuffer( );
    output.append( "Origin: " + origin + "\nMethod: " + method + "\nRequest Headers:" );
    if ( requestHeaders == null || requestHeaders.size( ) == 0 ) {
      output.append( " null" );
    } else {
      for ( String requestHeader : requestHeaders ) {
        output.append( "\n  " + requestHeader );
      }

    }
    return output.toString( );
  }

  public String getOrigin( ) {
    return origin;
  }

  public void setOrigin( String origin ) {
    this.origin = origin;
  }

  public String getMethod( ) {
    return method;
  }

  public void setMethod( String method ) {
    this.method = method;
  }

  public ArrayList<String> getRequestHeaders( ) {
    return requestHeaders;
  }

  public void setRequestHeaders( ArrayList<String> requestHeaders ) {
    this.requestHeaders = requestHeaders;
  }
}
