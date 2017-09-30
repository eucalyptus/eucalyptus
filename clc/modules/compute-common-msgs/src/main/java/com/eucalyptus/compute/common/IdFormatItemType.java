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

public class IdFormatItemType extends EucalyptusData {

  private String resource;
  private Boolean useLongIds;
  private Date deadline;

  public IdFormatItemType( ) {
  }

  public IdFormatItemType( final String resource, final Boolean useLongIds ) {
    this.resource = resource;
    this.useLongIds = useLongIds;
  }

  public String getResource( ) {
    return resource;
  }

  public void setResource( String resource ) {
    this.resource = resource;
  }

  public Boolean getUseLongIds( ) {
    return useLongIds;
  }

  public void setUseLongIds( Boolean useLongIds ) {
    this.useLongIds = useLongIds;
  }

  public Date getDeadline( ) {
    return deadline;
  }

  public void setDeadline( Date deadline ) {
    this.deadline = deadline;
  }
}
