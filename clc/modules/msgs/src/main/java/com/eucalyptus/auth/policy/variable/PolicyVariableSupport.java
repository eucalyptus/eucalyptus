/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.policy.variable;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.util.Parameters;

/**
 *
 */
public abstract class PolicyVariableSupport implements PolicyVariable {
  
  private final String vendor;
  private final String name;
  private final String qName;
  
  protected PolicyVariableSupport( final String vendor, final String name ) {
    Parameters.checkParam( "vendor", vendor, not( isEmptyOrNullString( ) ) );  
    Parameters.checkParam( "name", name, not( isEmptyOrNullString( ) ) );  
    this.vendor = vendor;
    this.name = name;
    this.qName = PolicySpec.qualifiedName( vendor, name );
  }

  @Override
  public String getVendor( ) {
    return vendor;
  }

  @Override
  public String getName( ) {
    return name;
  }

  public String getQName( ) {
    return qName;
  }
}
