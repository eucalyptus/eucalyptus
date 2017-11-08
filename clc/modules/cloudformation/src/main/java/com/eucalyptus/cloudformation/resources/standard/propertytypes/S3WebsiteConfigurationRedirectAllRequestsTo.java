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
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

public class S3WebsiteConfigurationRedirectAllRequestsTo {

  @Required
  @Property
  private String hostName;

  @Property
  private String protocol;

  public String getHostName( ) {
    return hostName;
  }

  public void setHostName( String hostName ) {
    this.hostName = hostName;
  }

  public String getProtocol( ) {
    return protocol;
  }

  public void setProtocol( String protocol ) {
    this.protocol = protocol;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3WebsiteConfigurationRedirectAllRequestsTo that = (S3WebsiteConfigurationRedirectAllRequestsTo) o;
    return Objects.equals( getHostName( ), that.getHostName( ) ) &&
        Objects.equals( getProtocol( ), that.getProtocol( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getHostName( ), getProtocol( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "hostName", hostName )
        .add( "protocol", protocol )
        .toString( );
  }
}
