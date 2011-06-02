/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.cluster.callback;

import com.eucalyptus.util.async.FailedRequestException;
import com.eucalyptus.util.async.SubjectMessageCallback;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import com.eucalyptus.util.async.RemoteCallback;

/**
 * Parent class for messages which have no preconditions or outstanding side-effects which need to
 * be rolled back in the case of a failure. This class
 * introduces a narrower signature for {@link #fireException(FailedRequestException)} which is only
 * invoked when a <b>negative</b> response is recieved for a
 * request. That is, descendants will not see connection-related exceptions. A consequence is that a
 * given execution of the underlying mechanism may never
 * invoke the callback. This differs from {@link RemoteCallback} where either
 * {@link RemoteCallback#fire(BaseMessage)} <b>or</b>
 * {@link RemoteCallback#fireException(Throwable)} is guaranteed to be called exactly one time.
 */
public abstract class StateUpdateMessageCallback<P, Q extends BaseMessage, R extends BaseMessage> extends SubjectMessageCallback<P, Q, R> {
  
  public StateUpdateMessageCallback( ) {
    super( );
  }

  public StateUpdateMessageCallback( P subject, Q request ) {
    super( subject, request );
  }

  public StateUpdateMessageCallback( P subject ) {
    super( subject );
  }

  public StateUpdateMessageCallback( Q request ) {
    super( request );
  }

  /**
   * NOTE: Empty implementation as there are no preconditions for this type of
   * message: suppress the warning.
   * 
   * @see com.eucalyptus.util.async.MessageCallback#initialize(edu.ucsb.eucalyptus.msgs.BaseMessage)
   * @param request
   * @throws Exception
   */
  @Override
  public final void initialize( Q request ) throws Exception {}
  
  /**
   * NOTE: Empty implementation as there are no outstanding side-effects which
   * need to be rolled back: suppress the warning.
   * 
   * @see com.eucalyptus.util.async.MessageCallback#fireException(java.lang.Throwable)
   * @param t
   */
  @Override
  public final void fireException( Throwable t ) {
    this.fireException( new FailedRequestException( t, getRequest() ) );
  }
  
  /**
   * TODO: DOCUMENT StateUpdateMessageCallback.java
   * 
   * @param t
   */
  public abstract void fireException( FailedRequestException t );
  
}
