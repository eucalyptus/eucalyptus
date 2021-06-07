/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.auth.euare.common;

import com.eucalyptus.auth.euare.common.msgs.GetServerCertificateResponseType;
import com.eucalyptus.auth.euare.common.msgs.GetServerCertificateType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListRolesType;
import com.eucalyptus.auth.euare.common.msgs.ListServerCertificatesResponseType;
import com.eucalyptus.auth.euare.common.msgs.ListServerCertificatesType;
import com.eucalyptus.auth.euare.common.msgs.PutRolePolicyResponseType;
import com.eucalyptus.auth.euare.common.msgs.PutRolePolicyType;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.component.id.Euare;

@ComponentPart(Euare.class)
public interface EuareApi {

  // API

  GetServerCertificateResponseType getServerCertificate( GetServerCertificateType request );

  ListRolesResponseType listRoles( ListRolesType request );

  ListServerCertificatesResponseType listServerCertificates( ListServerCertificatesType request );

  PutRolePolicyResponseType putRolePolicy( PutRolePolicyType request );

  // Helpers

  default GetServerCertificateResponseType getServerCertificate(final String name) {
    final GetServerCertificateType request = new GetServerCertificateType();
    request.setServerCertificateName(name);
    return getServerCertificate(request);
  }

  default ListRolesResponseType listRoles() {
    return listRoles(new ListRolesType());
  }

  default ListRolesResponseType listRoles(final String pathPrefix) {
    final ListRolesType request = new ListRolesType();
    request.setPathPrefix(pathPrefix);
    return listRoles(request);
  }

  default ListServerCertificatesResponseType listServerCertificates() {
    return listServerCertificates(new ListServerCertificatesType());
  }

  default PutRolePolicyResponseType putRolePolicy(
      final String roleName,
      final String policyName,
      final String policyDocument
  ) {
    final PutRolePolicyType request = new PutRolePolicyType();
    request.setRoleName(roleName);
    request.setPolicyName(policyName);
    request.setPolicyDocument(policyDocument);
    return putRolePolicy(request);
  }

}
