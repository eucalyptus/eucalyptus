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
package com.eucalyptus.simpleworkflow;

import java.util.concurrent.ExecutionException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.component.Topology;
import com.eucalyptus.simpleworkflow.stateful.NotifyResponseType;
import com.eucalyptus.simpleworkflow.stateful.NotifyType;
import com.eucalyptus.simpleworkflow.stateful.PollForNotificationResponseType;
import com.eucalyptus.simpleworkflow.stateful.PollForNotificationType;
import com.eucalyptus.simpleworkflow.stateful.PolledNotifications;
import com.eucalyptus.util.Consumer;
import com.eucalyptus.util.Consumers;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.util.concurrent.ListenableFuture;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;

/**
 *
 */
public class NotifyClient {

  private static final Logger logger = Logger.getLogger( NotifyClient.class );

  public static final class NotifyTaskList {
    private final String accountNumber;
    private final String domain;
    private final String type;
    private final String name;

    public NotifyTaskList( final AccountFullName accountFullName,
                           final String domain,
                           final String type,
                           final String name ) {
      this( accountFullName.getAccountNumber( ), domain, type, name );
    }

    public NotifyTaskList( final String accountNumber,
                           final String domain,
                           final String type,
                           final String name ) {
      this.accountNumber = accountNumber;
      this.domain = domain;
      this.type = type;
      this.name = name;
    }

    public String getChannelName( ) {
      return Joiner.on( ':' ).join( accountNumber, type, domain, name );
    }

    @SuppressWarnings( "RedundantIfStatement" )
    @Override
    public boolean equals( final Object o ) {
      if ( this == o ) return true;
      if ( o == null || getClass() != o.getClass() ) return false;

      final NotifyTaskList taskList = (NotifyTaskList) o;

      if ( !accountNumber.equals( taskList.accountNumber ) ) return false;
      if ( !domain.equals( taskList.domain ) ) return false;
      if ( !name.equals( taskList.name ) ) return false;
      if ( !type.equals( taskList.type ) ) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = accountNumber.hashCode();
      result = 31 * result + domain.hashCode();
      result = 31 * result + type.hashCode();
      result = 31 * result + name.hashCode();
      return result;
    }
  }

  public static void notifyTaskList( final AccountFullName accountFullName,
                                     final String domain,
                                     final String type,
                                     final String taskList ) {
    notifyTaskList( new NotifyTaskList( accountFullName, domain, type, taskList ) );
  }

  public static void notifyTaskList( final NotifyTaskList taskList ) {
    final NotifyType notify = new NotifyType( );
    notify.setChannel( taskList.getChannelName( ) );
    try {
      final ListenableFuture<NotifyResponseType> dispatchFuture =
          AsyncRequests.dispatch( Topology.lookup( PolledNotifications.class ), notify );
      dispatchFuture.addListener( new Runnable( ) {
        @Override
        public void run() {
          try {
            dispatchFuture.get( );
          } catch ( final InterruptedException e ) {
            logger.info( "Interrupted while sending notification for " + notify.getChannel(), e );
          } catch ( final ExecutionException e ) {
            logger.error( "Error sending notification for " + notify.getChannel( ), e );
            //TODO:STEVE: should retry notification?
          }
        }
      } );
    } catch ( final Exception e ) {
      logger.error( "Error sending notification for " + notify.getChannel( ), e );
    }
  }

  public static void pollTaskList( final AccountFullName accountFullName,
                                   final String domain,
                                   final String type,
                                   final String taskList,
                                   final Consumer<Boolean> resultConsumer ) throws Exception {
    pollTaskList( new NotifyTaskList( accountFullName, domain, type, taskList ), resultConsumer );
  }

  public static void pollTaskList( final NotifyTaskList taskList,
                                   final Consumer<Boolean> resultConsumer ) throws Exception {
    final Consumer<Boolean> consumer = Consumers.once( resultConsumer );
    final PollForNotificationType poll = new PollForNotificationType( );
    poll.setChannel( taskList.getChannelName( ) );
    final ListenableFuture<PollForNotificationResponseType> dispatchFuture =
        AsyncRequests.dispatch( Topology.lookup( PolledNotifications.class ), poll );
    dispatchFuture.addListener( new Runnable( ) {
      @Override
      public void run( ) {
        try {
          final PollForNotificationResponseType response = dispatchFuture.get( );
          consumer.accept( Objects.firstNonNull( response.getNotified(), false ) );
        } catch ( final InterruptedException e ) {
          logger.info( "Interrupted while polling for task " + poll.getChannel( ), e );
        } catch ( final Exception e ) {
          logger.error( "Error polling for task " + poll.getChannel( ), e );
        } finally {
          consumer.accept( false );
        }
      }
    } );
  }
}
