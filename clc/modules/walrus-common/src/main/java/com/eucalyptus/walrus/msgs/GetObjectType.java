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

public class GetObjectType extends WalrusDataGetRequestType {

  private Boolean getMetaData;
  private Boolean getData;
  private Boolean inlineData;
  private Boolean deleteAfterGet;
  private Boolean getTorrent;
  private String versionId;

  public GetObjectType( ) { }

  public GetObjectType( final String bucketName, final String key, final Boolean getData, final Boolean getMetaData, final Boolean inlineData ) {
    super( bucketName, key );
    this.getData = getData;
    this.getMetaData = getMetaData;
    this.inlineData = inlineData;
  }

  public Boolean getGetMetaData( ) {
    return getMetaData;
  }

  public void setGetMetaData( Boolean getMetaData ) {
    this.getMetaData = getMetaData;
  }

  public Boolean getGetData( ) {
    return getData;
  }

  public void setGetData( Boolean getData ) {
    this.getData = getData;
  }

  public Boolean getInlineData( ) {
    return inlineData;
  }

  public void setInlineData( Boolean inlineData ) {
    this.inlineData = inlineData;
  }

  public Boolean getDeleteAfterGet( ) {
    return deleteAfterGet;
  }

  public void setDeleteAfterGet( Boolean deleteAfterGet ) {
    this.deleteAfterGet = deleteAfterGet;
  }

  public Boolean getGetTorrent( ) {
    return getTorrent;
  }

  public void setGetTorrent( Boolean getTorrent ) {
    this.getTorrent = getTorrent;
  }

  public String getVersionId( ) {
    return versionId;
  }

  public void setVersionId( String versionId ) {
    this.versionId = versionId;
  }
}
