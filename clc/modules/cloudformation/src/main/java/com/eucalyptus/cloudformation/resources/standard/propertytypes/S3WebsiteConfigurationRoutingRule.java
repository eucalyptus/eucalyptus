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

public class S3WebsiteConfigurationRoutingRule {

  @Required
  @Property
  private S3WebsiteConfigurationRoutingRulesRedirectRule redirectRule;

  @Property
  private S3WebsiteConfigurationRoutingRulesRoutingRuleCondition routingRuleCondition;

  public S3WebsiteConfigurationRoutingRulesRedirectRule getRedirectRule( ) {
    return redirectRule;
  }

  public void setRedirectRule( S3WebsiteConfigurationRoutingRulesRedirectRule redirectRule ) {
    this.redirectRule = redirectRule;
  }

  public S3WebsiteConfigurationRoutingRulesRoutingRuleCondition getRoutingRuleCondition( ) {
    return routingRuleCondition;
  }

  public void setRoutingRuleCondition( S3WebsiteConfigurationRoutingRulesRoutingRuleCondition routingRuleCondition ) {
    this.routingRuleCondition = routingRuleCondition;
  }

  @Override
  public boolean equals( final Object o ) {
    if ( this == o ) return true;
    if ( o == null || getClass( ) != o.getClass( ) ) return false;
    final S3WebsiteConfigurationRoutingRule that = (S3WebsiteConfigurationRoutingRule) o;
    return Objects.equals( getRedirectRule( ), that.getRedirectRule( ) ) &&
        Objects.equals( getRoutingRuleCondition( ), that.getRoutingRuleCondition( ) );
  }

  @Override
  public int hashCode( ) {
    return Objects.hash( getRedirectRule( ), getRoutingRuleCondition( ) );
  }

  @Override
  public String toString( ) {
    return MoreObjects.toStringHelper( this )
        .add( "redirectRule", redirectRule )
        .add( "routingRuleCondition", routingRuleCondition )
        .toString( );
  }
}
