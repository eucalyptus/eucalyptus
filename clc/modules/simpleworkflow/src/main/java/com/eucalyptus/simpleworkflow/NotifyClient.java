/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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
