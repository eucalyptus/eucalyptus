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

import com.eucalyptus.component.id.Dns;
import com.eucalyptus.resources.EucalyptusActivityException;
import com.eucalyptus.util.DispatchingClient;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Callback.Checked;
import com.eucalyptus.util.async.CheckedListenableFuture;

import edu.ucsb.eucalyptus.msgs.DnsMessage;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiANameType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordResponseType;
import edu.ucsb.eucalyptus.msgs.RemoveMultiARecordType;

/**
 * @author Sang-Min Park
 *
 */
public class DnsClient {

  private static DnsClient _instance = null;
  private DnsClient(){ }
  public static DnsClient getInstance(){
    if(_instance == null)
      _instance = new DnsClient();
    return _instance;
  }
  
  private class DnsContext extends AbstractClientContext<DnsMessage, Dns> {
    private DnsContext(final String userId){
      super(userId, Dns.class);
    }
  }
  
  
  // / delete one name-address mapping from existing {name - {addr1, addr2, etc
  // } } map
  private class DnsRemoveARecordTask extends
      EucalyptusClientTask<DnsMessage, Dns> {
    private String zone = null;
    private String name = null;
    private String address = null;

    private DnsRemoveARecordTask(final String zone, final String name,
        final String address) {
      this.zone = zone;
      this.name = name;
      this.address = address;
    }

    private RemoveMultiARecordType removeARecord() {
      final RemoveMultiARecordType req = new RemoveMultiARecordType();
      req.setZone(this.zone);
      req.setName(this.name);
      req.setAddress(this.address);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<DnsMessage, Dns> context,
        Checked<DnsMessage> callback) {

      final DispatchingClient<DnsMessage, Dns> client = context.getClient();
      client.dispatch(removeARecord(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<DnsMessage, Dns> context,
        DnsMessage response) {
      // TODO Auto-generated method stub
      final RemoveMultiARecordResponseType resp = (RemoveMultiARecordResponseType) response;
    }
  }

  // / delete name - {addr1, addr2, addr3, etc} mapping entirely
  private class DnsRemoveMultiARecordTask extends
      EucalyptusClientTask<DnsMessage, Dns> {
    private String zone = null;
    private String name = null;

    private DnsRemoveMultiARecordTask(final String zone, final String name) {
      this.zone = zone;
      this.name = name;
    }

    private RemoveMultiANameType removeARecord() {
      final RemoveMultiANameType req = new RemoveMultiANameType();
      req.setZone(this.zone);
      req.setName(this.name);
      return req;
    }

    @Override
    void dispatchInternal(ClientContext<DnsMessage, Dns> context,
        Checked<DnsMessage> callback) {

      final DispatchingClient<DnsMessage, Dns> client = context.getClient();
      client.dispatch(removeARecord(), callback);
    }

    @Override
    void dispatchSuccess(ClientContext<DnsMessage, Dns> context,
        DnsMessage response) {
      // TODO Auto-generated method stub
      final RemoveMultiANameResponseType resp = (RemoveMultiANameResponseType) response;
    }
  }

  public void removeARecord(String zone, String name, String address) {
    final DnsRemoveARecordTask task = new DnsRemoveARecordTask(zone, name,
        address);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new DnsContext(null));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException("failed to remove A record ");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }

  public void removeMultiARecord(String zone, String name) {
    final DnsRemoveMultiARecordTask task = new DnsRemoveMultiARecordTask(zone,
        name);
    final CheckedListenableFuture<Boolean> result = task
        .dispatch(new DnsContext(null));
    try {
      if (result.get()) {
        return;
      } else
        throw new EucalyptusActivityException(
            "failed to remove multi A records ");
    } catch (Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
}
