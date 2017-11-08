/*************************************************************************
 * Copyright 2009-2016 Ent. Services Development Corporation LP
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

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

/**
 *
 */
@CompileStatic
class PublicAddresses {
  private static final Map<String,String> dirtyAddresses = Maps.newConcurrentMap( )

  static void markDirty( String address, String partition ) {
    dirtyAddresses.put( address, partition )
  }

  static boolean clearDirty( Collection<String> inUse, String partition ) {
    boolean cleared = false;
    dirtyAddresses.each{ String address, String addressPartition ->
      if ( partition == addressPartition && !inUse.contains( address )) {
        cleared = dirtyAddresses.remove( address ) || cleared
      }
    }
    cleared
  }

  static boolean clearDirty( String address ) {
    dirtyAddresses.remove( address )
  }

  static Set<String> dirtySnapshot( ) {
    Sets.newHashSet( dirtyAddresses.keySet( ) )
  }
}
