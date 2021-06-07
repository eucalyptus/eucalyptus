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
package com.eucalyptus.compute.common.network;

import java.util.Objects;
import com.google.common.base.MoreObjects;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public abstract class NetworkResource extends EucalyptusData {
  private static final long serialVersionUID = 1L;

  private String ownerId;
  private String value;

  protected NetworkResource( ) {
  }

  protected NetworkResource( final String ownerId, final String value ) {
    this.ownerId = ownerId;
    this.value = value;
  }

  public abstract String getType( );

  public String getOwnerId( ) {
    return ownerId;
  }

  public void setOwnerId( String ownerId ) {
    this.ownerId = ownerId;
  }

  public String getValue( ) {
    return value;
  }

  public void setValue( String value ) {
    this.value = value;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( !( o instanceof NetworkResource ) ) return false;
    final NetworkResource that = (NetworkResource) o;
    return Objects.equals( getOwnerId( ), that.getOwnerId( ) ) &&
        Objects.equals( getValue( ), that.getValue( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getOwnerId( ), getValue( ) );
  }

  protected MoreObjects.ToStringHelper toStringHelper( Object self ) {
    return MoreObjects.toStringHelper( self )
        .add( "value", getValue( ) )
        .add( "ownerId", getOwnerId( ) );
  }
}
