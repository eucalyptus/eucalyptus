/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.ws;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static com.eucalyptus.util.Parameters.checkParam;
import javax.annotation.Nonnull;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

/**
 * Represents an expected error condition from a web service.
 * 
 * <p>The codes used will be service specific, the {@link Role} should be
 * interpreted at binding time to a valid meaningful to the service.</p>
 * 
 * <p>Annotations for binding specific defaults should be placed on this
 * class and overridden as appropriate in the exception class hierarchy.</p>
 */
@QueryBindingInfo( statusCode = 500 )
public class EucalyptusWebServiceException extends EucalyptusCloudException {
  private static final long serialVersionUID = 1L;

  private final String code;
  private final Role role;
  
  public EucalyptusWebServiceException( final String code,
                                        final Role role,
                                        final String message ) {
    super( message );
    checkParam( code, not( isEmptyOrNullString() ) );
    checkParam( role, notNullValue() );
    checkParam( message, not( isEmptyOrNullString() ) );
    this.code = code;
    this.role = role;
  }

  @Nonnull
  public String getCode() {
    return code;
  }

  @Nonnull
  public Role getRole() {
    return role;
  }
}
