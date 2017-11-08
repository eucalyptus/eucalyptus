/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.bootstrap;

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.crypto.Digest;

import com.eucalyptus.crypto.Signatures;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Joiner;

public class SystemIds {

  public static String databasePassword( ) {
    try {
      return Digest.SHA256.digestHex( Signatures.SHA256withRSA.signBinary( Eucalyptus.class, "eucalyptus".getBytes() ) );
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( "Error getting database password", e );
    }
  }
  
  public static String tunnelPassword( ) {
    return Signatures.SHA256withRSA.trySign( Eucalyptus.class, "vtunpass".getBytes( ) );
  }

  public static String securityTokenPassword( ) {
    return Signatures.SHA256withRSA.trySign( Eucalyptus.class, "tokens-service".getBytes( ) );
  }

  public static String createCloudUniqueName( String subName ) {
    return Joiner.on( "." ).join(
        Eucalyptus.class.getSimpleName( ),
        BillOfMaterials.getVersion( ),
        subName,
        Signatures.SHA256withRSA.trySign( Eucalyptus.class, subName.getBytes( ) ) );
  }
  
  public static String createShortCloudUniqueName( String subName ) {
    try {
      return Joiner.on( "." ).join(
          Eucalyptus.class.getSimpleName( ),
          BillOfMaterials.getVersion( ),
          subName,
          Digest.SHA256.digestHex( Signatures.SHA256withRSA.signBinary( Eucalyptus.class, subName.getBytes( ) ) ) );
    } catch ( Exception e ) {
      throw Exceptions.toUndeclared( "Error getting short unique name for " + subName, e );
    }
  }
  
  public static String cloudName( ) {
    return createCloudUniqueName( "cloud" );
  }
  
  public static String cacheName( ) {
    return createShortCloudUniqueName( "cache" );
  }
  
  public static String membershipGroupName( ) {
    return createShortCloudUniqueName( "membership" );
  }
  
  public static String membershipUdpMcastTransportName( ) {
    return createShortCloudUniqueName( "membership-udp-mcast-transport" );
  }
}
