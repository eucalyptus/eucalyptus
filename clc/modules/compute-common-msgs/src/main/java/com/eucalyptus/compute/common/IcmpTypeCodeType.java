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

public class IcmpTypeCodeType extends EucalyptusData {

  @ComputeMessageValidation.FieldRange( min = -1l, max = 255l )
  private Integer code;
  @ComputeMessageValidation.FieldRange( min = -1l, max = 255l )
  private Integer type;

  public IcmpTypeCodeType( ) {
  }

  public IcmpTypeCodeType( final Integer code, final Integer type ) {
    this.code = code;
    this.type = type;
  }

  public Integer getCode( ) {
    return code;
  }

  public void setCode( Integer code ) {
    this.code = code;
  }

  public Integer getType( ) {
    return type;
  }

  public void setType( Integer type ) {
    this.type = type;
  }
}
