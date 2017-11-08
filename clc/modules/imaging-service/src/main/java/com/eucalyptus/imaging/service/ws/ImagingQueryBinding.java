/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.imaging.service.ws;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.imaging.common.Imaging;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;

/**
 * @author Sang-Min Park
 *
 */
@ComponentPart(Imaging.class)
public class ImagingQueryBinding extends BaseQueryBinding<OperationParameter> {
  static final String IMAGING_NAMESPACE_PATTERN = "http://www.eucalyptus.com/ns/imaging/%s/";
  static final String IMAGING_DEFAULT_VERSION = "2014-02-14";
  static final String IMAGING_DEFAULT_NAMESPACE = String.format( IMAGING_NAMESPACE_PATTERN, IMAGING_DEFAULT_VERSION );

  
  public ImagingQueryBinding( ) {
    super( IMAGING_NAMESPACE_PATTERN, IMAGING_DEFAULT_VERSION, UnknownParameterStrategy.ERROR, OperationParameter.Action, OperationParameter.Operation );
  }
}
