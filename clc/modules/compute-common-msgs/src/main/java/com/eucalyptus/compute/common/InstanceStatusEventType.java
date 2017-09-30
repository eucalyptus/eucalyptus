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
package com.eucalyptus.compute.common;

import java.util.Date;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class InstanceStatusEventType extends EucalyptusData {

  private String code;
  private String description;
  private Date notBefore;
  private Date notAfter;

  public String getCode( ) {
    return code;
  }

  public void setCode( String code ) {
    this.code = code;
  }

  public String getDescription( ) {
    return description;
  }

  public void setDescription( String description ) {
    this.description = description;
  }

  public Date getNotBefore( ) {
    return notBefore;
  }

  public void setNotBefore( Date notBefore ) {
    this.notBefore = notBefore;
  }

  public Date getNotAfter( ) {
    return notAfter;
  }

  public void setNotAfter( Date notAfter ) {
    this.notAfter = notAfter;
  }
}
