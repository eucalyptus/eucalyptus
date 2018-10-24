/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
