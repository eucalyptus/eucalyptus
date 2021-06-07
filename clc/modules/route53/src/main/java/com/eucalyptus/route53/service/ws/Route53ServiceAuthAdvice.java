/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.ws;

import static com.eucalyptus.util.RestrictedTypes.getIamActionByMessageType;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.ServiceAdvice;
import com.eucalyptus.route53.common.msgs.Route53Message;
import com.eucalyptus.route53.common.policy.Route53PolicySpec;
import com.eucalyptus.route53.service.Route53AuthorizationException;

/**
 *
 */
@ComponentNamed
public class Route53ServiceAuthAdvice extends ServiceAdvice {

  @Override
  protected void beforeService( @Nonnull final Object requestObject ) throws Exception {
    if ( requestObject instanceof Route53Message) {
      final Route53Message request = (Route53Message) requestObject;
      final AuthContextSupplier user = Contexts.lookup( ).getAuthContext( );

      // Authorization check
      if ( !Permissions.perhapsAuthorized( Route53PolicySpec.VENDOR_ROUTE53, getIamActionByMessageType( request ), user ) ) {
        throw new Route53AuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
      }
    } else {
      throw new Route53AuthorizationException( "UnauthorizedOperation", "You are not authorized to perform this operation." );
    }
  }
}

