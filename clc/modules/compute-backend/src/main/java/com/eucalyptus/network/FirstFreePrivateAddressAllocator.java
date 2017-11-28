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
package com.eucalyptus.network;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import org.apache.log4j.Logger;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;

/**
 * Private address allocator that uses the first free address.
 */
public class FirstFreePrivateAddressAllocator extends PrivateAddressAllocatorSupport {

  private static final Logger logger = Logger.getLogger( FirstFreePrivateAddressAllocator.class );

  public FirstFreePrivateAddressAllocator( ) {
    this( new DatabasePrivateAddressPersistence( ) );
  }

  protected FirstFreePrivateAddressAllocator( final PrivateAddressPersistence persistence ) {
    super( logger, persistence );
  }

  @Override
  protected String allocate(
      final Iterable<Integer> addresses,
      final int addressCount,
      final int allocatedCount,
      final Function<Integer,String> allocator,
      final Supplier<Set<Integer>> lister
  ) {
    final Iterator<Integer> iterator = addresses.iterator( );
    while ( iterator.hasNext( ) ) {
      final String value = allocator.apply( iterator.next( ) );
      if ( !Strings.isNullOrEmpty( value ) ) {
        return value;
      }
    }
    return null;
  }
}
