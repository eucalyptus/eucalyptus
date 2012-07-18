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

package com.eucalyptus.auth.principal;

import java.io.Serializable;
import java.security.Principal;

public interface BasePrincipal extends Principal, Serializable {
  /**
   * Returns the name of this principal.
   * 
   * @return the name of this principal.
   */
  public abstract String getName( );
  
  /**
   * Compares this principal to the specified object. Returns true if the object passed in matches the principal represented by the implementation of this
   * interface.
   * 
   * @param another
   *          principal to compare with.
   * @return true if the principal passed in is the same as that encapsulated by this principal, and false otherwise.
   */
  public abstract boolean equals( Object another );
  
  /**
   * Returns a string representation of this principal.
   * 
   * @return a string representation of this principal.
   */
  public abstract String toString( );
  
  /**
   * Returns a hashcode for this principal.
   * 
   * @return a hashcode for this principal.
   */
  public abstract int hashCode( );
}
