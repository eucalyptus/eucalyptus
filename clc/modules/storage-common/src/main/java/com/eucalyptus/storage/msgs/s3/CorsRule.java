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

public class CorsRule extends EucalyptusData {

  private String id;
  private int sequence;
  private ArrayList<String> allowedOrigins;
  private ArrayList<String> allowedMethods;
  private ArrayList<String> allowedHeaders;
  private int maxAgeSeconds;
  private ArrayList<String> exposeHeaders;

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public int getSequence( ) {
    return sequence;
  }

  public void setSequence( int sequence ) {
    this.sequence = sequence;
  }

  public ArrayList<String> getAllowedOrigins( ) {
    return allowedOrigins;
  }

  public void setAllowedOrigins( ArrayList<String> allowedOrigins ) {
    this.allowedOrigins = allowedOrigins;
  }

  public ArrayList<String> getAllowedMethods( ) {
    return allowedMethods;
  }

  public void setAllowedMethods( ArrayList<String> allowedMethods ) {
    this.allowedMethods = allowedMethods;
  }

  public ArrayList<String> getAllowedHeaders( ) {
    return allowedHeaders;
  }

  public void setAllowedHeaders( ArrayList<String> allowedHeaders ) {
    this.allowedHeaders = allowedHeaders;
  }

  public int getMaxAgeSeconds( ) {
    return maxAgeSeconds;
  }

  public void setMaxAgeSeconds( int maxAgeSeconds ) {
    this.maxAgeSeconds = maxAgeSeconds;
  }

  public ArrayList<String> getExposeHeaders( ) {
    return exposeHeaders;
  }

  public void setExposeHeaders( ArrayList<String> exposeHeaders ) {
    this.exposeHeaders = exposeHeaders;
  }
}
