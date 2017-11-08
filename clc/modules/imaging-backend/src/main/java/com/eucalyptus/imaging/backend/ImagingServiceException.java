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
package com.eucalyptus.imaging.backend;

import java.lang.reflect.InvocationTargetException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.Role;

/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
public class ImagingServiceException extends EucalyptusWebServiceException {
  public static final Role DEFAULT_ROLE = Role.Sender;
  public static final String DEFAULT_CODE = "400"; 
  
  public static final String INTERNAL_SERVER_ERROR = "500";
  
  protected ImagingServiceException( 
                               final String code, 
                               final Role role, 
                               final String message ) {
    super( code, role, message );
  }

  public ImagingServiceException(final String code, final String message){
    this( code, DEFAULT_ROLE, message);
  }
  
  public ImagingServiceException(final String code, final String message, final Throwable inner){
    this(code, DEFAULT_ROLE, message);
    this.initCause(inner);
  }
  
  public ImagingServiceException(final String message){
    this(DEFAULT_CODE, DEFAULT_ROLE, message);
  }
  public ImagingServiceException(final String message, Throwable inner){
    this(DEFAULT_CODE, DEFAULT_ROLE, message);
    this.initCause(inner);
  }
  public static <T extends ImagingServiceException> T rethrow( Class<T> type, String message ) {
    try {
      return type.getConstructor( new Class[] { String.class } ).newInstance( new Object[] { message } );
    } catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException ex ) {
      throw Exceptions.toUndeclared( ex );
    }
  }
}