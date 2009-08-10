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

public class VmInstances extends AbstractNamedRegistry<VmInstance> {

  private static VmInstances singleton = getInstance();

  public static VmInstances getInstance() {
    synchronized ( VmInstances.class ) {
      if ( singleton == null )
        singleton = new VmInstances();
    }
    return singleton;
  }

  public static String getId( Long rsvId, int launchIndex ) {
    String vmId = null;
    do {
      MessageDigest digest = Hashes.Digest.MD5.get();
      digest.reset();
      digest.update( Long.toString( rsvId + launchIndex + System.currentTimeMillis() ).getBytes() );

      Adler32 hash = new Adler32();
      hash.reset();
      hash.update( digest.digest() );
      vmId = String.format( "i-%08X", hash.getValue() );
    } while ( VmInstances.getInstance().contains( vmId ) );
    return vmId;
  }

  public static String getAsMAC( String instanceId ) {
    String mac = String.format( "d0:0d:%s:%s:%s:%s",
                                instanceId.substring( 2, 4 ),
                                instanceId.substring( 4, 6 ),
                                instanceId.substring( 6, 8 ),
                                instanceId.substring( 8, 10 ) );
    return mac;
  }

}
