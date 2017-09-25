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

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Upload extends EucalyptusData {

  private String key;
  private String uploadId;
  private Initiator initiator;
  private CanonicalUser owner;
  private String storageClass;
  private Date initiated;

  public Upload( String key, String uploadId, Initiator initiator, CanonicalUser owner, String storageClass, Date initiated ) {
    this.key = key;
    this.uploadId = uploadId;
    this.initiator = initiator;
    this.owner = owner;
    this.storageClass = storageClass;
    this.initiated = initiated;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public String getUploadId( ) {
    return uploadId;
  }

  public void setUploadId( String uploadId ) {
    this.uploadId = uploadId;
  }

  public Initiator getInitiator( ) {
    return initiator;
  }

  public void setInitiator( Initiator initiator ) {
    this.initiator = initiator;
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

  public Date getInitiated( ) {
    return initiated;
  }

  public void setInitiated( Date initiated ) {
    this.initiated = initiated;
  }
}
