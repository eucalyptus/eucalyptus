/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
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

import com.eucalyptus.simpleworkflow.common.stateful.NotifyClientUtils;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.util.Consumer;
import com.google.common.base.Joiner;

/**
 *
 */
public class NotifyClient {

  private static final Logger logger = Logger.getLogger( NotifyClient.class );

  public static final class NotifyTaskList implements NotifyClientUtils.ChannelWrapper {
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

    public static NotifyTaskList of( final AccountFullName accountFullName,
                                     final String domain,
                                     final String type,
                                     final String name ) {
      return new NotifyTaskList( accountFullName, domain, type, name );
    }

    public static NotifyTaskList of( final String accountNumber,
                                     final String domain,
                                     final String type,
                                     final String name ) {
      return new NotifyTaskList( accountNumber, domain, type, name );
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
    notifyTaskList(new NotifyTaskList(accountFullName, domain, type, taskList));
  }

  public static void notifyTaskList(final NotifyTaskList taskList) {
    NotifyClientUtils.notifyChannel(taskList);
  }

  public static Consumer<Boolean> pollTaskList(
      final AccountFullName accountFullName,
      final String domain,
      final String type,
      final String taskList,
      final long timeout,
      final Consumer<Boolean> resultConsumer
  ) throws Exception {
    return pollTaskList(new NotifyTaskList(accountFullName, domain, type, taskList), timeout, resultConsumer);
  }

  public static Consumer<Boolean> pollTaskList(
      final NotifyTaskList taskList,
      final long timeout,
      final Consumer<Boolean> resultConsumer
  ) throws Exception {
    return NotifyClientUtils.pollChannel(taskList, timeout, resultConsumer);
  }

}
