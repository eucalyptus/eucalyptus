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

public class SigningCertificateType extends EucalyptusData {

  private String userName;
  private String certificateId;
  private String certificateBody;
  private String privateKey;
  private String status;
  private Date uploadDate;

  public String getUserName( ) {
    return userName;
  }

  public void setUserName( String userName ) {
    this.userName = userName;
  }

  public String getCertificateId( ) {
    return certificateId;
  }

  public void setCertificateId( String certificateId ) {
    this.certificateId = certificateId;
  }

  public String getCertificateBody( ) {
    return certificateBody;
  }

  public void setCertificateBody( String certificateBody ) {
    this.certificateBody = certificateBody;
  }

  public String getPrivateKey( ) {
    return privateKey;
  }

  public void setPrivateKey( String privateKey ) {
    this.privateKey = privateKey;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public Date getUploadDate( ) {
    return uploadDate;
  }

  public void setUploadDate( Date uploadDate ) {
    this.uploadDate = uploadDate;
  }
}
