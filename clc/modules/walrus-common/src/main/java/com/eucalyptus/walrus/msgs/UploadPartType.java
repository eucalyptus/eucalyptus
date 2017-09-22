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
package com.eucalyptus.walrus.msgs;

public class UploadPartType extends WalrusDataRequestType {

  private String contentLength;
  private String contentMD5;
  private String contentType;
  private String expect;
  private String uploadId;
  private String partNumber;

  public String getContentLength( ) {
    return contentLength;
  }

  public void setContentLength( String contentLength ) {
    this.contentLength = contentLength;
  }

  public String getContentMD5( ) {
    return contentMD5;
  }

  public void setContentMD5( String contentMD5 ) {
    this.contentMD5 = contentMD5;
  }

  public String getContentType( ) {
    return contentType;
  }

  public void setContentType( String contentType ) {
    this.contentType = contentType;
  }

  public String getExpect( ) {
    return expect;
  }

  public void setExpect( String expect ) {
    this.expect = expect;
  }

  public String getUploadId( ) {
    return uploadId;
  }

  public void setUploadId( String uploadId ) {
    this.uploadId = uploadId;
  }

  public String getPartNumber( ) {
    return partNumber;
  }

  public void setPartNumber( String partNumber ) {
    this.partNumber = partNumber;
  }
}
