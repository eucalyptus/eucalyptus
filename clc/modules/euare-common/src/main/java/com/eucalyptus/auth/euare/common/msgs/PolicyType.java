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

public class PolicyType extends EucalyptusData {

  private String arn;
  private Integer attachmentCount;
  private Date createDate;
  private String defaultVersionId;
  private String description;
  private Boolean isAttachable;
  private String path;
  private String policyId;
  private String policyName;
  private Date updateDate;

  public String getArn( ) {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }

  public Integer getAttachmentCount( ) {
    return attachmentCount;
  }

  public void setAttachmentCount( Integer attachmentCount ) {
    this.attachmentCount = attachmentCount;
  }

  public Date getCreateDate( ) {
    return createDate;
  }

  public void setCreateDate( Date createDate ) {
    this.createDate = createDate;
  }

  public String getDefaultVersionId( ) {
    return defaultVersionId;
  }

  public void setDefaultVersionId( String defaultVersionId ) {
    this.defaultVersionId = defaultVersionId;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Boolean getIsAttachable( ) {
    return isAttachable;
  }

  public void setIsAttachable( Boolean isAttachable ) {
    this.isAttachable = isAttachable;
  }

  public String getPath( ) {
    return path;
  }

  public void setPath( String path ) {
    this.path = path;
  }

  public String getPolicyId( ) {
    return policyId;
  }

  public void setPolicyId( String policyId ) {
    this.policyId = policyId;
  }

  public String getPolicyName( ) {
    return policyName;
  }

  public void setPolicyName( String policyName ) {
    this.policyName = policyName;
  }

  public Date getUpdateDate( ) {
    return updateDate;
  }

  public void setUpdateDate( Date updateDate ) {
    this.updateDate = updateDate;
  }
}
