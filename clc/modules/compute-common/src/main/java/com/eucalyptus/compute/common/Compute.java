/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
