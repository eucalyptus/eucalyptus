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
import com.google.common.base.MoreObjects;

public class S3WebsiteConfigurationRoutingRulesRedirectRule {

  @Property
  private String hostName;

  @Property
  private String httpRedirectCode;

  @Property
  private String protocol;

  @Property
  private String replaceKeyPrefixWith;

  @Property
  private String replaceKeyWith;

  public String getHostName( ) {
    return hostName;
  }

  public void setHostName( String hostName ) {
    this.hostName = hostName;
  }

  public String getHttpRedirectCode( ) {
    return httpRedirectCode;
  }

  public void setHttpRedirectCode( String httpRedirectCode ) {
    this.httpRedirectCode = httpRedirectCode;
  }

  public String getProtocol( ) {
    return protocol;
  }

  public void setProtocol( String protocol ) {
    this.protocol = protocol;
  }

  public String getReplaceKeyPrefixWith( ) {
    return replaceKeyPrefixWith;
  }

  public void setReplaceKeyPrefixWith( String replaceKeyPrefixWith ) {
    this.replaceKeyPrefixWith = replaceKeyPrefixWith;
  }

  public String getReplaceKeyWith( ) {
    return replaceKeyWith;
  }

  public void setReplaceKeyWith( String replaceKeyWith ) {
    this.replaceKeyWith = replaceKeyWith;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3WebsiteConfigurationRoutingRulesRedirectRule that = (S3WebsiteConfigurationRoutingRulesRedirectRule) o;
    return Objects.equals( getHostName( ), that.getHostName( ) ) &&
        Objects.equals( getHttpRedirectCode( ), that.getHttpRedirectCode( ) ) &&
        Objects.equals( getProtocol( ), that.getProtocol( ) ) &&
        Objects.equals( getReplaceKeyPrefixWith( ), that.getReplaceKeyPrefixWith( ) ) &&
        Objects.equals( getReplaceKeyWith( ), that.getReplaceKeyWith( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getHostName( ), getHttpRedirectCode( ), getProtocol( ), getReplaceKeyPrefixWith( ), getReplaceKeyWith( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "hostName", hostName )
        .add( "httpRedirectCode", httpRedirectCode )
        .add( "protocol", protocol )
        .add( "replaceKeyPrefixWith", replaceKeyPrefixWith )
        .add( "replaceKeyWith", replaceKeyWith )
        .toString( );
  }
}
