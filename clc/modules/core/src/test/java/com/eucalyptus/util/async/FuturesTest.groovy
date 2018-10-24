/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
