/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.util;

import com.google.common.base.Predicate;

public interface Callback<R> {
  /**
   * The operation completed. Guaranteed to be called only once per corresponding dispatch.
   * 
   * @param t
   */
  public void fire( R input );
  
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
    public boolean apply( R input ) {
      try {
        this.fire( input );
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
     * @see com.eucalyptus.util.Callback#fire(java.lang.Object)
     * @param response
     */
    @Override
    public final void fire( R response ) {}
    
    /**
     * @see com.eucalyptus.util.Callback.Checked#fireException(java.lang.Throwable)
     * @param t
     */
    public abstract void fireException( Throwable t );
  }
  
  public abstract class Completion<R> implements Checked<R>, Predicate<R> {
    /**
     * @see com.eucalyptus.util.Callback#fire(java.lang.Object)
     * @param r
     */
    @Override
    public final void fire( R input ) {
      this.fire( );
    }
    
    /**
     * @see com.google.common.base.Predicate#apply(java.lang.Object)
     * @param arg0
     * @return
     */
    @Override
    public boolean apply( R input ) {
      try {
        this.fire( );
        return true;
      } catch ( Exception ex ) {
        return false;
      }
    }

    public abstract void fire( );

    public abstract boolean isDone( );
  }
}
