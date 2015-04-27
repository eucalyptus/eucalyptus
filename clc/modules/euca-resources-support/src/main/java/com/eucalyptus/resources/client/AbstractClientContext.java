/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
