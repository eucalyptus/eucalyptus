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

package com.eucalyptus.crypto;

import java.security.MessageDigest;

public enum Digest {
  GOST3411,
  Tiger,
  Whirlpool,
  MD2,
  MD4,
  MD5,
  RipeMD128,
  RipeMD160,
  RipeMD256,
  RipeMD320,
  SHA1,
  SHA224,
  SHA256,
  SHA384,
  SHA512;

  public MessageDigest get( ) {
    try {
      return MessageDigest.getInstance( this.name( ) );
    } catch ( Exception e ) {
      e.printStackTrace( );
      return null;
    }
  }
}
