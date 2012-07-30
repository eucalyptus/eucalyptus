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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.cloud.util;

import com.eucalyptus.util.HasNaturalId;

public interface Reference<T extends Reference<T, R>, R extends HasNaturalId> {
  
  enum State {
    UNKNOWN, FREE, PENDING, EXTANT, RELEASING
  }
  
  /**
   * Request is in-flight on the network (not just in memory) and state updates should be
   * disregarded until the in-flight request completes.
   * 
   * Calling {@link Reference#set(Object)} completes the allocation, while calling
   * {@link Reference#teardown()} resets the state to that before the reference change.
   */
  public T set( R referer ) throws ResourceAllocationException;
  
  /**
   * Request is in-flight on the network (not just in memory) and state updates should be
   * disregarded until the in-flight request completes.
   * 
   * Calling {@link Reference#set(Object)} completes the allocation, while calling
   * {@link Reference#teardown()} resets the state to that before the reference change.
   */
  public T allocate( ) throws ResourceAllocationException;
  
  /**
   * The procedure for gracefully releasing the resource is pending a submitted in-flight request.
   * Potential references to stale state may exist and should be disregarded until in-flight
   * requests complete.
   * 
   * Calling {@link Reference#set(Object)} completes releasing the allocation, while calling
   * {@link Reference#clear()} resets the state to that before the reference change.
   */
  public T release( ) throws ResourceAllocationException;
  
  /**
   * Dependent external resource state has been cleared and the resource is ready for re-use.
   * @return 
   * 
   * @throws ResourceAllocationException
   */
  public boolean teardown( ) throws ResourceAllocationException;
  
  /**
   * Attempt to recover a resource allocation -- e.g., after a system restart. Constraints must be
   * enforced on valid initial state.
   * 
   * @throws ResourceAllocationException
   */
  public T reclaim( R referer ) throws ResourceAllocationException;
  
}
