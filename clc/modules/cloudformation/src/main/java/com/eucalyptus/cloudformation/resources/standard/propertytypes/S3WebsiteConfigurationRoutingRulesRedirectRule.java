/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
