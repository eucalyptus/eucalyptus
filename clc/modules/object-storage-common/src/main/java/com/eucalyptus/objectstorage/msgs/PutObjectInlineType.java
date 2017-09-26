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
import java.util.List;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;

public class PutObjectInlineType extends ObjectStorageDataRequestType {

  private String contentLength;
  private ArrayList<MetaDataEntry> metaData = new ArrayList<>( );
  private AccessControlList accessControlList = new AccessControlList( );
  private String storageClass;
  private String base64Data;
  private String contentType;
  private String contentDisposition;

  public String getContentLength( ) {
    return contentLength;
  }

  public void setContentLength( String contentLength ) {
    this.contentLength = contentLength;
  }

  public ArrayList<MetaDataEntry> getMetaData( ) {
    return metaData;
  }

  public void setMetaData( ArrayList<MetaDataEntry> metaData ) {
    this.metaData = metaData;
  }

  public AccessControlList getAccessControlList( ) {
    return accessControlList;
  }

  public void setAccessControlList( AccessControlList accessControlList ) {
    this.accessControlList = accessControlList;
  }

  public String getStorageClass( ) {
    return storageClass;
  }

  public void setStorageClass( String storageClass ) {
    this.storageClass = storageClass;
  }

  public String getBase64Data( ) {
    return base64Data;
  }

  public void setBase64Data( String base64Data ) {
    this.base64Data = base64Data;
  }

  public String getContentType( ) {
    return contentType;
  }

  public void setContentType( String contentType ) {
    this.contentType = contentType;
  }

  public String getContentDisposition( ) {
    return contentDisposition;
  }

  public void setContentDisposition( String contentDisposition ) {
    this.contentDisposition = contentDisposition;
  }
}
