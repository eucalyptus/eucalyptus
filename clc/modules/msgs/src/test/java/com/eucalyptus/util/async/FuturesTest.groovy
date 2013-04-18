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
package com.eucalyptus.util.async

import static org.junit.Assert.*
import org.junit.Test

/**
 * 
 */
class FuturesTest {
  
  @Test
  void testAllAsListSuccess() {
    CheckedListenableFuture<Boolean> future1 = Futures.newGenericeFuture()
    CheckedListenableFuture<Boolean> future2 = Futures.newGenericeFuture()
    CheckedListenableFuture<Boolean> future3 = Futures.newGenericeFuture()
    CheckedListenableFuture<List<Boolean>> futures = Futures.allAsList( [ future1, future2, future3 ] )
    
    boolean complete = false
    futures.addListener( { complete = true } as Runnable )
    
    assertFalse( "Futures complete 1", complete )
    
    future3.set( false )
    
    assertFalse( "Futures complete 2", complete )
    
    future1.set( false )
    
    assertFalse( "Futures complete 3", complete )

    future2.set( true )

    assertTrue( "Futures complete 4", complete )
    
    assertEquals( "Futures values", [ false, true, false ], futures.get() )
  }

  @Test
  void testAllAsListFailure() {
    CheckedListenableFuture<Boolean> future1 = Futures.newGenericeFuture()
    CheckedListenableFuture<Boolean> future2 = Futures.newGenericeFuture()
    CheckedListenableFuture<Boolean> future3 = Futures.newGenericeFuture()
    CheckedListenableFuture<List<Boolean>> futures = Futures.allAsList( [ future1, future2, future3 ] )

    boolean complete = false
    futures.addListener( { complete = true } as Runnable )

    assertFalse( "Futures complete 1", complete )

    future3.set( false )

    assertFalse( "Futures complete 2", complete )

    future1.set( false )

    assertFalse( "Futures complete 3", complete )

    future2.setException( new IllegalArgumentException() )

    assertTrue( "Futures complete 4", complete )

    try {
      futures.get()
      fail( "Error expected" )
    } catch ( Exception e ) {
      // expected
    }
  }

  @Test
  void testAllAsListEarlyFailure() {
    CheckedListenableFuture<Boolean> future1 = Futures.newGenericeFuture()
    CheckedListenableFuture<Boolean> future2 = Futures.newGenericeFuture()
    CheckedListenableFuture<Boolean> future3 = Futures.newGenericeFuture()
    CheckedListenableFuture<List<Boolean>> futures = Futures.allAsList( [ future1, future2, future3 ] )

    boolean complete = false
    futures.addListener( { complete = true } as Runnable )

    assertFalse( "Futures complete 1", complete )

    future2.setException( new IllegalArgumentException() )

    assertTrue( "Futures complete 2", complete )

    future1.set( false )

    assertTrue( "Futures complete 3", complete )

    future3.set( false )

    assertTrue( "Futures complete 4", complete )

    try {
      futures.get()
      fail( "Error expected" )
    } catch ( Exception e ) {
      // expected
    }
  }
}
