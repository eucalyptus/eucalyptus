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
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;

public class CopyObjectResponseType extends ObjectStorageResponseType {

  private String etag;
  private String lastModified;
  private Long size;
  private ArrayList<MetaDataEntry> metaData = new ArrayList<>( );
  private Integer errorCode;
  private String contentType;
  private String contentDisposition;
  private String versionId;
  private String copySourceVersionId;

  public String getEtag( ) {
    return etag;
  }

  public void setEtag( String etag ) {
    this.etag = etag;
  }

  public String getLastModified( ) {
    return lastModified;
  }

  public void setLastModified( String lastModified ) {
    this.lastModified = lastModified;
  }

  public Long getSize( ) {
    return size;
  }

  public void setSize( Long size ) {
    this.size = size;
  }

  public ArrayList<MetaDataEntry> getMetaData( ) {
    return metaData;
  }

  public void setMetaData( ArrayList<MetaDataEntry> metaData ) {
    this.metaData = metaData;
  }

  public Integer getErrorCode( ) {
    return errorCode;
  }

  public void setErrorCode( Integer errorCode ) {
    this.errorCode = errorCode;
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

  public String getVersionId( ) {
    return versionId;
  }

  public void setVersionId( String versionId ) {
    this.versionId = versionId;
  }

  public String getCopySourceVersionId( ) {
    return copySourceVersionId;
  }

  public void setCopySourceVersionId( String copySourceVersionId ) {
    this.copySourceVersionId = copySourceVersionId;
  }
}
