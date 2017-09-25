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

public class Part extends EucalyptusData {

  private Integer partNumber;
  private String etag;
  private Date lastModified;
  private Long size;

  public Part( ) {
  }

  public Part( Integer partNumber, String etag ) {
    this.partNumber = partNumber;
    this.etag = etag;
  }

  public Part( Integer partNumber, String etag, Date lastModified, Long size ) {
    this.partNumber = partNumber;
    this.etag = etag;
    this.lastModified = lastModified;
    this.size = size;
  }

  public Integer getPartNumber( ) {
    return partNumber;
  }

  public void setPartNumber( Integer partNumber ) {
    this.partNumber = partNumber;
  }

  public String getEtag( ) {
    return etag;
  }

  public void setEtag( String etag ) {
    this.etag = etag;
  }

  public Date getLastModified( ) {
    return lastModified;
  }

  public void setLastModified( Date lastModified ) {
    this.lastModified = lastModified;
  }

  public Long getSize( ) {
    return size;
  }

  public void setSize( Long size ) {
    this.size = size;
  }
}
