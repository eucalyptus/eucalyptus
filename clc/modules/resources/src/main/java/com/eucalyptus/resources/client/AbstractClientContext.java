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
package com.eucalyptus.resources.client;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;

import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * @author Sang-Min Park
 *
 */
public abstract class AbstractClientContext<TM extends BaseMessage, TC extends ComponentId> 
  implements ClientContext<TM, TC> {
    String userId = null;
    Class<TC> cls = null;
    protected AbstractClientContext(final String userId, Class<TC> cls) {
      this.userId = userId;
      this.cls = cls;
    }

    @Override
    public String getUserId() {
      try {
        if (this.userId == null)
          return Accounts.lookupSystemAdmin().getUserId();
        else
          return this.userId;
      } catch (AuthException ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }

    @Override
    public DispatchingClient<TM, TC> getClient() {
      try {
        final DispatchingClient<TM, TC> client = new DispatchingClient<>(
            this.getUserId(), this.cls);
        client.init();
        return client;
      } catch (Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }
}
