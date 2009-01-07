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

package edu.ucsb.eucalyptus.cloud.entities;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table( name = "ssh_keypair" )
@Cache( usage = CacheConcurrencyStrategy.READ_WRITE )
public class SSHKeyPair {

  @Id
  @GeneratedValue
  @Column( name = "ssh_keypair_id" )
  private Long id = -1l;
  @Column( name = "ssh_keypair_name" )
  private String name;
  @Lob
  @Column( name = "ssh_keypair_public_key" )
  private String publicKey;
  @Column( name = "ssh_keypair_finger_print" )
  private String fingerPrint;
  @Transient
  public static String NO_KEY_NAME = "";
  @Transient
  public static SSHKeyPair NO_KEY = new SSHKeyPair( "", "", "" );

  public SSHKeyPair() {}

  public SSHKeyPair( final String name ) {
    this.name = name;
  }

  public SSHKeyPair( String name, String fingerPrint, String publicKey )
  {
    this.fingerPrint = fingerPrint;
    this.name = name;
    this.publicKey = publicKey;
  }

  public String getFingerPrint()
  {
    return fingerPrint;
  }

  public void setFingerPrint( String fingerPrint )
  {
    this.fingerPrint = fingerPrint;
  }

  public Long getId()
  {
    return id;
  }

  public void setId( Long id )
  {
    this.id = id;
  }

  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }

  public String getPublicKey()
  {
    return publicKey;
  }

  public void setPublicKey( String publicKey )
  {
    this.publicKey = publicKey;
  }

  public boolean equals( final Object o )
  {
    if ( this == o ) return true;
    if ( o == null || getClass() != o.getClass() ) return false;

    SSHKeyPair that = ( SSHKeyPair ) o;

    if ( !name.equals( that.name ) ) return false;

    return true;
  }

  public int hashCode()
  {
    return name.hashCode();
  }
}
