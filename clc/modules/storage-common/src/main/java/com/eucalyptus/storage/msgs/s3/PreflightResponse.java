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

public class PreflightResponse extends EucalyptusData {

  private String origin;
  private ArrayList<String> methods;
  private int maxAgeSeconds;
  private ArrayList<String> allowedHeaders;
  private ArrayList<String> exposeHeaders;

  public String toString( ) {
    StringBuffer output = new StringBuffer( );
    output.append( "Origin: " + origin + "\nMax Age, Seconds: " + maxAgeSeconds + "\nAllowed Headers:" );
    if ( allowedHeaders == null || allowedHeaders.size( ) == 0 ) {
      output.append( " null" );
    } else {
      for ( String allowedHeader : allowedHeaders ) {
        output.append( "\n  " + allowedHeader );
      }

    }

    output.append( "\nExpose Headers:" );
    if ( exposeHeaders == null || exposeHeaders.size( ) == 0 ) {
      output.append( " null" );
    } else {
      for ( String exposeHeader : exposeHeaders ) {
        output.append( "\n  " + exposeHeader );
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

  public ArrayList<String> getMethods( ) {
    return methods;
  }

  public void setMethods( ArrayList<String> methods ) {
    this.methods = methods;
  }

  public int getMaxAgeSeconds( ) {
    return maxAgeSeconds;
  }

  public void setMaxAgeSeconds( int maxAgeSeconds ) {
    this.maxAgeSeconds = maxAgeSeconds;
  }

  public ArrayList<String> getAllowedHeaders( ) {
    return allowedHeaders;
  }

  public void setAllowedHeaders( ArrayList<String> allowedHeaders ) {
    this.allowedHeaders = allowedHeaders;
  }

  public ArrayList<String> getExposeHeaders( ) {
    return exposeHeaders;
  }

  public void setExposeHeaders( ArrayList<String> exposeHeaders ) {
    this.exposeHeaders = exposeHeaders;
  }
}
