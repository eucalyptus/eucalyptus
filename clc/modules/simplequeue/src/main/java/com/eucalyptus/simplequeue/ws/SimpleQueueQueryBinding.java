/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.simplequeue.ws;

import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import org.apache.log4j.Logger;

public class SimpleQueueQueryBinding extends BaseQueryBinding<OperationParameter> {
  private static final Logger LOG = Logger.getLogger(SimpleQueueQueryBinding.class);
  static final String SIMPLEQUEUE_NAMESPACE_PATTERN = "http://queue.amazonaws.com/doc/2012-11-05/";  //TODO:GEN2OOLS: replace version with pattern : %s
  static final String SIMPLEQUEUE_DEFAULT_VERSION = "2012-11-05";              //TODO:GEN2OOLS: replace with correct default API version
  static final String SIMPLEQUEUE_DEFAULT_NAMESPACE = String.format(SIMPLEQUEUE_NAMESPACE_PATTERN, SIMPLEQUEUE_DEFAULT_VERSION);

  public SimpleQueueQueryBinding() {
    super(SIMPLEQUEUE_NAMESPACE_PATTERN, SIMPLEQUEUE_DEFAULT_VERSION, OperationParameter.Action, OperationParameter.Operation );
  }

}
