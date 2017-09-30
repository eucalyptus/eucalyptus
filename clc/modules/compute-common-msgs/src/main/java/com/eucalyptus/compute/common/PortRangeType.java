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

import edu.ucsb.eucalyptus.msgs.ComputeMessageValidation;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class PortRangeType extends EucalyptusData {

  @ComputeMessageValidation.FieldRange( max = 65535l )
  private Integer from;
  @ComputeMessageValidation.FieldRange( max = 65535l )
  private Integer to;

  public PortRangeType( ) {
  }

  public PortRangeType( final Integer from, final Integer to ) {
    this.from = from;
    this.to = to;
  }

  public Integer getFrom( ) {
    return from;
  }

  public void setFrom( Integer from ) {
    this.from = from;
  }

  public Integer getTo( ) {
    return to;
  }

  public void setTo( Integer to ) {
    this.to = to;
  }
}
