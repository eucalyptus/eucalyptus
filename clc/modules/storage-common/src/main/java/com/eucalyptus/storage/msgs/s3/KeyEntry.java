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

public abstract class KeyEntry extends EucalyptusData {

  private String key;
  private String versionId;
  private Boolean isLatest;
  private String lastModified;
  private CanonicalUser owner;

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public String getVersionId( ) {
    return versionId;
  }

  public void setVersionId( String versionId ) {
    this.versionId = versionId;
  }

  public Boolean getIsLatest( ) {
    return isLatest;
  }

  public void setIsLatest( Boolean isLatest ) {
    this.isLatest = isLatest;
  }

  public String getLastModified( ) {
    return lastModified;
  }

  public void setLastModified( String lastModified ) {
    this.lastModified = lastModified;
  }

  public CanonicalUser getOwner( ) {
    return owner;
  }

  public void setOwner( CanonicalUser owner ) {
    this.owner = owner;
  }
}
