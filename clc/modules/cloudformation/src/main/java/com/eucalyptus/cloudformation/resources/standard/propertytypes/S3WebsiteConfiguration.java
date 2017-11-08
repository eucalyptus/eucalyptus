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

import java.util.ArrayList;
import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class S3WebsiteConfiguration {

  @Property
  private String errorDocument;

  @Property
  private String indexDocument;

  @Property
  private S3WebsiteConfigurationRedirectAllRequestsTo redirectAllRequestsTo;

  @Property
  private ArrayList<S3WebsiteConfigurationRoutingRule> routingRules = Lists.newArrayList( );

  public String getErrorDocument( ) {
    return errorDocument;
  }

  public void setErrorDocument( String errorDocument ) {
    this.errorDocument = errorDocument;
  }

  public String getIndexDocument( ) {
    return indexDocument;
  }

  public void setIndexDocument( String indexDocument ) {
    this.indexDocument = indexDocument;
  }

  public S3WebsiteConfigurationRedirectAllRequestsTo getRedirectAllRequestsTo( ) {
    return redirectAllRequestsTo;
  }

  public void setRedirectAllRequestsTo( S3WebsiteConfigurationRedirectAllRequestsTo redirectAllRequestsTo ) {
    this.redirectAllRequestsTo = redirectAllRequestsTo;
  }

  public ArrayList<S3WebsiteConfigurationRoutingRule> getRoutingRules( ) {
    return routingRules;
  }

  public void setRoutingRules( ArrayList<S3WebsiteConfigurationRoutingRule> routingRules ) {
    this.routingRules = routingRules;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3WebsiteConfiguration that = (S3WebsiteConfiguration) o;
    return Objects.equals( getErrorDocument( ), that.getErrorDocument( ) ) &&
        Objects.equals( getIndexDocument( ), that.getIndexDocument( ) ) &&
        Objects.equals( getRedirectAllRequestsTo( ), that.getRedirectAllRequestsTo( ) ) &&
        Objects.equals( getRoutingRules( ), that.getRoutingRules( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getErrorDocument( ), getIndexDocument( ), getRedirectAllRequestsTo( ), getRoutingRules( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "errorDocument", errorDocument )
        .add( "indexDocument", indexDocument )
        .add( "redirectAllRequestsTo", redirectAllRequestsTo )
        .add( "routingRules", routingRules )
        .toString( );
  }
}
