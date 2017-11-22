/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.network

import com.google.common.base.Supplier
import groovy.transform.CompileStatic
import org.apache.log4j.Logger

/**
 * Private address allocator that uses the first free address.
 */
@CompileStatic
class FirstFreePrivateAddressAllocator extends PrivateAddressAllocatorSupport {

  private static final Logger logger = Logger.getLogger( FirstFreePrivateAddressAllocator )

  FirstFreePrivateAddressAllocator( ) {
    this( new DatabasePrivateAddressPersistence( ) )
  }

  protected FirstFreePrivateAddressAllocator( final PrivateAddressPersistence persistence ) {
    super( logger, persistence )
  }

  @Override
  protected String allocate(
      final Iterable<Integer> addresses,
      final int addressCount,
      final int allocatedCount,
      final Closure<String> allocator,
      final Supplier<Set<Integer>> lister
  ) {
    Iterator<Integer> iterator = addresses.iterator( )
    while ( iterator.hasNext( ) ) {
      String value = allocator.call( iterator.next( ) )
      if ( value ) return value;
    }
    null
  }
}
