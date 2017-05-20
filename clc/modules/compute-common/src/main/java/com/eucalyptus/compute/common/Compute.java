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
package com.eucalyptus.compute.common;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.AwsServiceName;
import com.eucalyptus.component.annotation.Description;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.auth.policy.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicComponentAccounts;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.component.annotation.ServiceNames;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;

/**
 *
 */
@PublicService
@AwsServiceName( "ec2" )
@ServiceNames( { "ec2", "eucalyptus" } )
@PolicyVendor( "ec2" )
@Partition( value = Compute.class, manyToOne = true )
@Description( "the Eucalyptus EC2 API service" )
@PublicComponentAccounts(AccountIdentifiers.AWS_EXEC_READ_SYSTEM_ACCOUNT)
public class Compute extends ComponentId {
  private static final long serialVersionUID = 1L;

  private final String CERT_USAGE_BUNDLING = "image-bundling";

  @Override
  public String getServicePath( final String... pathParts ) {
    return "/services/compute";
  }

  @Override
  public Set<String> getCertificateUsages( ) {
    return Collections.singleton( CERT_USAGE_BUNDLING );
  }

  @Override
  public X509Certificate getCertificate( final String usage ) {
    return CERT_USAGE_BUNDLING.equals( usage ) ?
        SystemCredentials.lookup( Eucalyptus.class ).getCertificate( ) :
        super.getCertificate( usage );
  }
}
