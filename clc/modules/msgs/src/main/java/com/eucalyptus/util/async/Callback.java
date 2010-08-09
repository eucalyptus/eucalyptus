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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.util.async;

import com.google.common.base.Predicate;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public interface Callback<R> {
  /**
   * The operation completed. Guaranteed to be called only once per corresponding dispatch.
   * 
   * @param t
   */
  public void fire( R t );
  
  /**
   * Allows for handling exceptions which occur during the asynchronous operation.
   * 
   * @author decker
   * @param <R>
   */
  public interface Checked<R> extends Callback<R> {
    public void fireException( Throwable t );
  }
  
  public interface TwiceChecked<Q, R> extends Checked<R> {
    public void initialize( Q request ) throws Exception;
  }
  
  /**
   * Only invoked when the requested operation succeeds.
   * 
   * @author decker
   * @param <R>
   */
  public abstract class Success<R> implements Callback<R>, Predicate<R> {

    /**
     * @see com.google.common.base.Predicate#apply(java.lang.Object)
     * @param arg0
     * @return
     */
    @Override
    public boolean apply( R arg0 ) {
      try {
        this.fire( arg0 );
        return true;
      } catch ( Exception ex ) {
        return false;
      }
    }
    
  }
  
  /**
   * Invoked only when the associated operation fails. The method {@link Callback.Checked#fireException(Throwable)} will be called with the cause of the
   * failure.
   * 
   * @author decker
   */
  public abstract class Failure<R> implements Checked<R> {
    /**
     * @see com.eucalyptus.util.async.Callback#fire(java.lang.Object)
     * @param response
     */
    @Override
    public final void fire( R response ) {}
    
    /**
     * @see com.eucalyptus.util.async.Callback.Checked#fireException(java.lang.Throwable)
     * @param t
     */
    public abstract void fireException( Throwable t );
  }
  
  public abstract class Completion<R> implements Checked<R>, Predicate<R> {
    /**
     * @see com.eucalyptus.util.async.Callback#fire(java.lang.Object)
     * @param r
     */
    @Override
    public final void fire( R r ) {
      this.fire( );
    }
    
    /**
     * @see com.google.common.base.Predicate#apply(java.lang.Object)
     * @param arg0
     * @return
     */
    @Override
    public boolean apply( R arg0 ) {
      try {
        this.fire( );
        return true;
      } catch ( Exception ex ) {
        return false;
      }
    }

    public abstract void fire( );
    
    public abstract void fireException( Throwable t );
  }
}
