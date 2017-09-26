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
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry;
import com.eucalyptus.storage.msgs.s3.ListEntry;

public class ListBucketResponseType extends ObjectStorageResponseType {

  private String name;
  private String prefix;
  private String marker;
  private String nextMarker;
  private int maxKeys;
  private String delimiter;
  private boolean isTruncated;
  private ArrayList<ListEntry> contents;
  private ArrayList<CommonPrefixesEntry> commonPrefixesList = new ArrayList<CommonPrefixesEntry>( );

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public String getMarker( ) {
    return marker;
  }

  public void setMarker( String marker ) {
    this.marker = marker;
  }

  public String getNextMarker( ) {
    return nextMarker;
  }

  public void setNextMarker( String nextMarker ) {
    this.nextMarker = nextMarker;
  }

  public int getMaxKeys( ) {
    return maxKeys;
  }

  public void setMaxKeys( int maxKeys ) {
    this.maxKeys = maxKeys;
  }

  public String getDelimiter( ) {
    return delimiter;
  }

  public void setDelimiter( String delimiter ) {
    this.delimiter = delimiter;
  }

  public boolean getIsTruncated( ) {
    return isTruncated;
  }

  public boolean isIsTruncated( ) {
    return isTruncated;
  }

  public void setIsTruncated( boolean isTruncated ) {
    this.isTruncated = isTruncated;
  }

  public ArrayList<ListEntry> getContents( ) {
    return contents;
  }

  public void setContents( ArrayList<ListEntry> contents ) {
    this.contents = contents;
  }

  public ArrayList<CommonPrefixesEntry> getCommonPrefixesList( ) {
    return commonPrefixesList;
  }

  public void setCommonPrefixesList( ArrayList<CommonPrefixesEntry> commonPrefixesList ) {
    this.commonPrefixesList = commonPrefixesList;
  }
}
