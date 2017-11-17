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
