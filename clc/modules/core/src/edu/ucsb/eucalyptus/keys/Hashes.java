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

package edu.ucsb.eucalyptus.keys;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.encoders.UrlBase64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.*;
import java.util.zip.Adler32;

public class Hashes {
    private static Logger LOG = Logger.getLogger( Hashes.class );

  public static byte[] getPemBytes( final Object o ) throws IOException {
      PEMWriter pemOut;
      ByteArrayOutputStream pemByteOut = new ByteArrayOutputStream();
      pemOut = new PEMWriter( new OutputStreamWriter( pemByteOut ) );
      pemOut.writeObject( o );
      pemOut.close();
      return pemByteOut.toByteArray();
    }

  static
    {
        Security.addProvider( new BouncyCastleProvider() );
    }

    public enum Digest {
        GOST3411, Tiger, Whirlpool,
        MD2, MD4, MD5,
        RipeMD128, RipeMD160, RipeMD256, RipeMD320,
        SHA1, SHA224, SHA256, SHA384, SHA512;
        public MessageDigest get()
        {
            try
            {
                return MessageDigest.getInstance( this.name() );
            }
            catch ( Exception e )
            {
                LOG.error( e, e );
                System.exit( 3778 );
                return null;
            }
        }
    }

    public enum Mac {
        HmacSHA1, HmacSHA256
    }

    public static String hashPassword( String password ) throws NoSuchAlgorithmException
    {
        byte[] fp = Digest.MD5.get().digest( password.getBytes() );
        return getHexString( fp );
    }

    public static String getDigestBase64( String input, Digest hash, boolean randomize )
    {
        byte[] inputBytes = input.getBytes();
        byte[] digestBytes = null;
        MessageDigest digest = hash.get();
        digest.update( inputBytes );
        if ( randomize )
        {
            SecureRandom random = new SecureRandom();
            random.setSeed( System.currentTimeMillis() );
            byte[] randomBytes = random.generateSeed( inputBytes.length );
            digest.update( randomBytes );
        }
        digestBytes = digest.digest();
        return new String( UrlBase64.encode( digestBytes ) );
    }

    public static String base64encode( String input )
    {
        return new String( UrlBase64.encode( input.getBytes(  )) );
    }

    public static String base64decode( String input )
    {
        return new String( UrlBase64.decode( input.getBytes(  )) );
    }

    public static String getFingerPrint( Key privKey )
    {
        try
        {
            byte[] fp = Digest.SHA1.get().digest( privKey.getEncoded() );
            StringBuffer sb = new StringBuffer();
            for ( byte b : fp )
                sb.append( String.format( "%02X:", b ) );
            return sb.substring( 0, sb.length() - 1 ).toLowerCase();
        }
        catch ( Exception e )
        {
            LOG.error( e, e );
            return null;
        }
    }

    public static String getHexString( byte[] data )
    {
        StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < data.length; i++ )
        {
            int halfbyte = ( data[ i ] >>> 4 ) & 0x0F;
            int two_halfs = 0;
            do
            {
                if ( ( 0 <= halfbyte ) && ( halfbyte <= 9 ) )
                    buf.append( ( char ) ( '0' + halfbyte ) );
                else
                    buf.append( ( char ) ( 'a' + ( halfbyte - 10 ) ) );
                halfbyte = data[ i ] & 0x0F;
            } while ( two_halfs++ < 1 );
        }
        return buf.toString().toLowerCase();
    }

    public static String getRandom(int size) {
        SecureRandom random = new SecureRandom();
        random.setSeed( System.nanoTime() );
        byte[] randomBytes = new byte[size];
        random.nextBytes(randomBytes);
        return new String(UrlBase64.encode(randomBytes));
    }

  public static String generateId( final String userId, final String prefix ) {
    Adler32 hash = new Adler32();
    String key = userId + System.currentTimeMillis();
    hash.update( key.getBytes() );
    String imageId = String.format( "%s-%08X", prefix, hash.getValue() );
    return imageId;
  }


  // borrowing from neil for the time being
  public static byte[] hexToBytes(String data) {
      int k = 0;
      byte[] results = new byte[data.length() / 2];
      for (int i = 0; i < data.length();) {
          results[k] = (byte) (Character.digit(data.charAt(i++), 16) << 4);
          results[k] += (byte) (Character.digit(data.charAt(i++), 16));
          k++;
      }

      return results;
  }

}
