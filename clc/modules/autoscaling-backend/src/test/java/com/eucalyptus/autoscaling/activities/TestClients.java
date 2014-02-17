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
package com.eucalyptus.autoscaling.activities;

import javax.annotation.Nullable;
import com.eucalyptus.cloudwatch.common.msgs.CloudWatchMessage;
import com.eucalyptus.loadbalancing.common.msgs.LoadBalancingMessage;
import com.eucalyptus.util.Callback;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

/**
 * Groovy can't handle the generics so using Java + Callback
 */
public class TestClients {
  public interface RequestHandler {
    BaseMessage handle( BaseMessage request );
  }

  static class TestEucalyptusClient extends EucalyptusClient {
    private final RequestHandler handler;

    TestEucalyptusClient( String userId, RequestHandler handler ) {
      super(userId);
      this.handler = handler;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <REQ extends EucalyptusMessage, RES extends EucalyptusMessage> void dispatch( REQ request,
                                                                                  Callback.Checked<RES> callback,
                                                                                  @Nullable Runnable then ) {
      try {
        callback.fire( (RES)handler.handle( request ) );
      } catch ( Exception e ) {
        callback.fireException( e );
      } finally {
        if ( then != null ) then.run();
      }
    }
  }

  static class TestElbClient extends ElbClient {
    private final RequestHandler handler;

    TestElbClient( String userId, RequestHandler handler ) {
      super(userId);
      this.handler = handler;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <REQ extends LoadBalancingMessage, RES extends LoadBalancingMessage> void dispatch( REQ request,
                                                                                               Callback.Checked<RES> callback,
                                                                                               @Nullable Runnable then ) {
      try {
        callback.fire( (RES)handler.handle( request ) );
      } catch ( Exception e ) {
        callback.fireException( e );
      } finally {
        if ( then != null ) then.run();
      }
    }
  }

  public static class TesCloudWatchClient extends CloudWatchClient {
    private final RequestHandler handler;

    TesCloudWatchClient( String userId, RequestHandler handler ) {
      super(userId);
      this.handler = handler;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <REQ extends CloudWatchMessage, RES extends CloudWatchMessage> void dispatch( REQ request,
                                                                                         Callback.Checked<RES> callback,
                                                                                         @Nullable Runnable then ) {
      try {
        callback.fire( (RES)handler.handle( request ) );
      } catch ( Exception e ) {
        callback.fireException( e );
      } finally {
        if ( then != null ) then.run();
      }
    }
  }
}
