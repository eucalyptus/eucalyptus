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
import com.eucalyptus.storage.msgs.s3.KeyEntry;

public class ListVersionsResponseType extends ObjectStorageResponseType {

  private String name;
  private String prefix;
  private String keyMarker;
  private String versionIdMarker;
  private String nextKeyMarker;
  private String nextVersionIdMarker;
  private int maxKeys;
  private String delimiter;
  private boolean isTruncated;
  private ArrayList<KeyEntry> keyEntries = new ArrayList<KeyEntry>( );
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

  public String getKeyMarker( ) {
    return keyMarker;
  }

  public void setKeyMarker( String keyMarker ) {
    this.keyMarker = keyMarker;
  }

  public String getVersionIdMarker( ) {
    return versionIdMarker;
  }

  public void setVersionIdMarker( String versionIdMarker ) {
    this.versionIdMarker = versionIdMarker;
  }

  public String getNextKeyMarker( ) {
    return nextKeyMarker;
  }

  public void setNextKeyMarker( String nextKeyMarker ) {
    this.nextKeyMarker = nextKeyMarker;
  }

  public String getNextVersionIdMarker( ) {
    return nextVersionIdMarker;
  }

  public void setNextVersionIdMarker( String nextVersionIdMarker ) {
    this.nextVersionIdMarker = nextVersionIdMarker;
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

  public ArrayList<KeyEntry> getKeyEntries( ) {
    return keyEntries;
  }

  public void setKeyEntries( ArrayList<KeyEntry> keyEntries ) {
    this.keyEntries = keyEntries;
  }

  public ArrayList<CommonPrefixesEntry> getCommonPrefixesList( ) {
    return commonPrefixesList;
  }

  public void setCommonPrefixesList( ArrayList<CommonPrefixesEntry> commonPrefixesList ) {
    this.commonPrefixesList = commonPrefixesList;
  }
}
