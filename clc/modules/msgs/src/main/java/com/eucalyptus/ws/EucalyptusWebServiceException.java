/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
    checkParam( message, notNullValue() );
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
