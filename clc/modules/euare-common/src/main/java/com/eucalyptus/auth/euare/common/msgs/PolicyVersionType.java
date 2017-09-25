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
package com.eucalyptus.auth.euare.common.msgs;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class PolicyVersionType extends EucalyptusData {

  private Date createDate;
  private String document;
  private Boolean isDefaultVersion;
  private String versionId;

  public Date getCreateDate( ) {
    return createDate;
  }

  public void setCreateDate( Date createDate ) {
    this.createDate = createDate;
  }

  public String getDocument( ) {
    return document;
  }

  public void setDocument( String document ) {
    this.document = document;
  }

  public Boolean getIsDefaultVersion( ) {
    return isDefaultVersion;
  }

  public void setIsDefaultVersion( Boolean isDefaultVersion ) {
    this.isDefaultVersion = isDefaultVersion;
  }

  public String getVersionId( ) {
    return versionId;
  }

  public void setVersionId( String versionId ) {
    this.versionId = versionId;
  }
}
