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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class ListEntry extends EucalyptusData {

  private String key;
  private String lastModified;
  private String etag;
  private long size;
  private CanonicalUser owner;
  private String storageClass;

  public ListEntry( ) {
  }

  public ListEntry( String objKey, String modified, String eTag, long objSize, CanonicalUser objOwner, String objStorageClass ) {
    this.key = objKey;
    this.lastModified = modified;
    this.etag = eTag;
    this.size = objSize;
    this.owner = objOwner;
    this.storageClass = objStorageClass;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public String getLastModified( ) {
    return lastModified;
  }

  public void setLastModified( String lastModified ) {
    this.lastModified = lastModified;
  }

  public String getEtag( ) {
    return etag;
  }

  public void setEtag( String etag ) {
    this.etag = etag;
  }

  public long getSize( ) {
    return size;
  }

  public void setSize( long size ) {
    this.size = size;
  }

  public CanonicalUser getOwner( ) {
    return owner;
  }

  public void setOwner( CanonicalUser owner ) {
    this.owner = owner;
  }

  public String getStorageClass( ) {
    return storageClass;
  }

  public void setStorageClass( String storageClass ) {
    this.storageClass = storageClass;
  }
}
