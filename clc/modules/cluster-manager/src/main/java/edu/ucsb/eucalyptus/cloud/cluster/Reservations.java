/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.cloud.cluster;

import edu.ucsb.eucalyptus.cloud.AbstractNamedRegistry;

import java.security.MessageDigest;
import java.util.zip.Adler32;

import com.eucalyptus.auth.Hashes;

public class Reservations extends AbstractNamedRegistry<Reservation> {

  private static Reservations singleton = getInstance();

  public static Reservations getInstance()
  {
    synchronized ( Reservations.class )
    {
      if ( singleton == null )
        singleton = new Reservations();
    }
    return singleton;
  }

  public static String makeReservationId( Long idNumber )
  {
    MessageDigest digest = Hashes.Digest.MD2.get();
    digest.reset();
    digest.update( idNumber.toString().getBytes(  ));

    Adler32 hash = new Adler32();
    hash.reset();
    hash.update( digest.digest() );

    return String.format( "r-%08X", hash.getValue() );
  }

  public static String getMac( int mac )
  {
    return String.format( "%02X:%02X:%02X:%02X", ( mac >> 24 ) & 0xff, ( mac >> 16 ) & 0xff, ( mac >> 8 ) & 0xff, mac & 0xff );
  }
}
