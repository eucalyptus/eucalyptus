/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.portal;

import com.eucalyptus.ws.protocol.QueryBindingInfo;

@QueryBindingInfo( statusCode = 401 )
public class Ec2ReportsServiceUnauthorizedException extends Ec2ReportsServiceException {
  private static final long serialVersionUID = 1L;

  public Ec2ReportsServiceUnauthorizedException(final String code, final String message) { super(code, message); }
}
