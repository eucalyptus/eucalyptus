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

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class IamInstanceProfile extends EucalyptusData {

  private String arn;
  private String id;

  public IamInstanceProfile( ) {
  }

  public IamInstanceProfile( String arn, String id ) {
    this.arn = arn;
    this.id = id;
  }

  public String getArn( ) {
    return arn;
  }

  public void setArn( String arn ) {
    this.arn = arn;
  }

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }
}
