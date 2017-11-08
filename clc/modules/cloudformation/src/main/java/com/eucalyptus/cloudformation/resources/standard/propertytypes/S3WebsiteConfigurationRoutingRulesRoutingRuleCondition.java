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

public class S3WebsiteConfigurationRoutingRulesRoutingRuleCondition {

  @Property
  private String httpErrorCodeReturnedEquals;

  @Property
  private String keyPrefixEquals;

  public String getHttpErrorCodeReturnedEquals( ) {
    return httpErrorCodeReturnedEquals;
  }

  public void setHttpErrorCodeReturnedEquals( String httpErrorCodeReturnedEquals ) {
    this.httpErrorCodeReturnedEquals = httpErrorCodeReturnedEquals;
  }

  public String getKeyPrefixEquals( ) {
    return keyPrefixEquals;
  }

  public void setKeyPrefixEquals( String keyPrefixEquals ) {
    this.keyPrefixEquals = keyPrefixEquals;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3WebsiteConfigurationRoutingRulesRoutingRuleCondition that = (S3WebsiteConfigurationRoutingRulesRoutingRuleCondition) o;
    return Objects.equals( getHttpErrorCodeReturnedEquals( ), that.getHttpErrorCodeReturnedEquals( ) ) &&
        Objects.equals( getKeyPrefixEquals( ), that.getKeyPrefixEquals( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getHttpErrorCodeReturnedEquals( ), getKeyPrefixEquals( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "httpErrorCodeReturnedEquals", httpErrorCodeReturnedEquals )
        .add( "keyPrefixEquals", keyPrefixEquals )
        .toString( );
  }
}
