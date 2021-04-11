/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service.ws;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.rds.common.msgs.RdsMessage;
import com.eucalyptus.rds.common.policy.RdsPolicySpec;
import com.eucalyptus.rds.service.RdsAuthorizationException;

/**
 *
 */
@ComponentNamed
public class RdsServiceAuthAdvice extends ServiceAdvice {

  @Override
  protected void beforeService( @Nonnull final Object requestObject ) throws Exception {
    if ( requestObject instanceof RdsMessage) {
      final RdsMessage request = (RdsMessage) requestObject;
      final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );

      // Authorization check
      if ( !Permissions.perhapsAuthorized( RdsPolicySpec.VENDOR_RDS, getIamActionByMessageType( request ), user ) ) {
        throw new RdsAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
      }
    } else {
      throw new RdsAuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }
  }
}
