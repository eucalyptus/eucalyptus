/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.service;

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.util.ClassPathSystemAccountProvider;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class RdsSystemAccountProvider  extends ClassPathSystemAccountProvider {

  public static final String RDS_SYSTEM_ACCOUNT = AccountIdentifiers.SYSTEM_ACCOUNT_PREFIX + "rds";;

  public RdsSystemAccountProvider( ) {
    super(
        RDS_SYSTEM_ACCOUNT,
        false,
        ImmutableList.<SystemAccountRole>of(
            newSystemAccountRole(
                "RdsServiceAdministrator",
                "/rds",
                ImmutableList.<AttachedPolicy>of(
                    newAttachedPolicy( "RdsServiceAdministrator" )
                )
            )
        )
    );
  }
}

