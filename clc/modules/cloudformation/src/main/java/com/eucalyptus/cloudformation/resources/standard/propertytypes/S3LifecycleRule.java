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
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class S3LifecycleRule {

  @Property
  private String expirationDate;

  @Property
  private Integer expirationInDays;

  @Property
  private String id;

  @Property
  private String prefix;

  @Required
  @Property
  private String status;

  @Property
  private S3LifecycleRuleTransition transition;

  public String getExpirationDate( ) {
    return expirationDate;
  }

  public void setExpirationDate( String expirationDate ) {
    this.expirationDate = expirationDate;
  }

  public Integer getExpirationInDays( ) {
    return expirationInDays;
  }

  public void setExpirationInDays( Integer expirationInDays ) {
    this.expirationInDays = expirationInDays;
  }

  public String getId( ) {
    return id;
  }

  public void setId( String id ) {
    this.id = id;
  }

  public String getPrefix( ) {
    return prefix;
  }

  public void setPrefix( String prefix ) {
    this.prefix = prefix;
  }

  public String getStatus( ) {
    return status;
  }

  public void setStatus( String status ) {
    this.status = status;
  }

  public S3LifecycleRuleTransition getTransition( ) {
    return transition;
  }

  public void setTransition( S3LifecycleRuleTransition transition ) {
    this.transition = transition;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3LifecycleRule that = (S3LifecycleRule) o;
    return Objects.equals( getExpirationDate( ), that.getExpirationDate( ) ) &&
        Objects.equals( getExpirationInDays( ), that.getExpirationInDays( ) ) &&
        Objects.equals( getId( ), that.getId( ) ) &&
        Objects.equals( getPrefix( ), that.getPrefix( ) ) &&
        Objects.equals( getStatus( ), that.getStatus( ) ) &&
        Objects.equals( getTransition( ), that.getTransition( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getExpirationDate( ), getExpirationInDays( ), getId( ), getPrefix( ), getStatus( ), getTransition( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "expirationDate", expirationDate )
        .add( "expirationInDays", expirationInDays )
        .add( "id", id )
        .add( "prefix", prefix )
        .add( "status", status )
        .add( "transition", transition )
        .toString( );
  }
}
