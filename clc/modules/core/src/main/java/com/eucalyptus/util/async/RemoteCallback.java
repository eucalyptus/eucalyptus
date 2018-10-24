/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util.async;

import com.eucalyptus.util.Callback;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

/**
 * An asynchronous callback which involves remote communication (read: possibly long lasting). Implementors of this interface are guaranteed that:
 * 
 * <ul>
 * <li>{@link #initialize(BaseMessage)} will be invoked before communication begins.</li>
 * <li>Should the invokation of {@link #initialize(BaseMessage)} terminate prematurely the callback will be disposed of -- preconditions
 * caused a failure.</li>
 * <li>Otherwise, either {@link #fire(BaseMessage)} <b>OR</b> {@link #fireException(Throwable)} will be called exactly
 * one time.</li>
 * <li>The argument to {@link #fire(BaseMessage)}/{@link #fireException(Throwable)} will be the outcome of the remote
 * operation.</li>
 * </ul>
 * @see Callback
 * @see Callback.Checked
 * @see Callback.TwiceChecked
 * @see RemoteCallback#fire(BaseMessage)
 * @see RemoteCallback#fireException(Throwable)
 */
public interface RemoteCallback<Q extends BaseMessage, R extends BaseMessage> extends Callback.TwiceChecked<Q, R> {
  
  /**
   * Get the request being made by this callback.
   */
  Q getRequest( );
  
  /**
   * Initialize (e.g., check preconditions) before proceeding with the
   * connection and sending of <tt>request</tt>. Guaranteed to be called only
   * once per corresponding dispatch.
   */
  @Override
  void initialize( Q request ) throws Exception;
  
  /**
   * The operation completed with a response: <tt>msg</tt>.
   */
  @Override
  void fire( R msg );
  
  /**
   * The operation completed with an exception.
   */
  @Override
  void fireException( Throwable t );
  
}
