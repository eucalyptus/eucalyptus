/*
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.compute.common.internal.vm;

import java.util.NoSuchElementException;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthContextSupplier;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.ern.Ern;
import com.eucalyptus.auth.policy.ern.EuareResourceName;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.InstanceProfile;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.google.common.base.Strings;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import net.sf.json.JSONException;

/**
 *
 */
public class VmIamInstanceProfileHelper {

  public static Tuple2<String,String> profileAccountAndName(
      final String defaultAccountNumber,
      final String arn,
      final String name
  ) {
    final String profileAccount;
    final String profileName;
    if ( !Strings.isNullOrEmpty( arn ) ) try {
      final Ern resourceName = Ern.parse( arn );
      if ( !( resourceName instanceof EuareResourceName ) ) {
        throw new IllegalArgumentException( "Invalid IAM instance profile ARN: " + arn );
      }
      profileAccount = resourceName.getAccount( );
      profileName = ((EuareResourceName) resourceName).getName( );
    } catch ( JSONException e ) {
      throw new IllegalArgumentException( "Invalid IAM instance profile ARN: " + arn );
    } else {
      profileAccount = defaultAccountNumber;
      profileName = name;
    }
    return Tuple.of( profileAccount, profileName );
  }

  public static InstanceProfile lookup( final Tuple2<String,String> profileAccountAndName ) throws AuthException, NoSuchElementException {
    return lookup( profileAccountAndName._1( ), profileAccountAndName._2( ) );
  }

  public static InstanceProfile lookup( final String profileAccount, final String profileName ) throws AuthException, NoSuchElementException {
    try {
      return Accounts.lookupInstanceProfileByName( profileAccount, profileName );
    } catch ( AuthException e ) {
      if ( AuthException.NO_SUCH_INSTANCE_PROFILE.equals( e.getMessage( ) ) ) {
        throw new NoSuchElementException( "Invalid IAM instance profile: " + profileAccount + "/" + profileName );
      } else {
        throw e;
      }
    }
  }

  public static VmInstance lookupVmInstanceByIamInstanceProfileAssociationId(
      final OwnerFullName ownerFullName,
      final String associationId
  ) {
    final EntityRestriction<VmInstance> accountRestriction = ownerFullName==null ?
        Entities.restriction( VmInstance.class ).all( ).build( ) :
        Entities.restriction( VmInstance.class )
            .equal( VmInstance_.ownerAccountNumber, ownerFullName.getAccountNumber( ) )
            .build( );
    return Entities.criteriaQuery( VmInstance.class )
        .where( accountRestriction )
        .join( VmInstance_.bootRecord )
        .whereEqual( VmBootRecord_.iamInstanceProfileAssociationId, associationId )
        .uniqueResult( );
  }

  public static String checkAuthorized( final InstanceProfile profile, final AuthContextSupplier user, final OwnerFullName ownerFullName ) {
    if ( !Permissions.isAuthorized(
        PolicySpec.VENDOR_IAM,
        PolicySpec.IAM_RESOURCE_INSTANCE_PROFILE,
        Accounts.getInstanceProfileFullName( profile ),
        AccountFullName.getInstance( profile.getAccountNumber( ) ),
        "listinstanceprofiles",
        user ) ) {
      throw new IllegalStateException( String.format(
          "Not authorized to access instance profile with ARN %s for %s",
          profile.getInstanceProfileArn( ),
          ownerFullName ) );
    }

    final Role role = profile.getRole( );
    if ( role == null ) {
      throw new IllegalArgumentException( "Invalid IAM instance profile ARN: " + profile.getInstanceProfileArn() + ", role not found" );
    }
    if ( !Permissions.isAuthorized(
        PolicySpec.VENDOR_IAM,
        PolicySpec.IAM_RESOURCE_ROLE,
        Accounts.getRoleFullName( role ),
        AccountFullName.getInstance( role.getAccountNumber( ) ),
        "passrole",
        user ) ) {
      throw new IllegalStateException( String.format(
          "Not authorized to pass role with ARN %s for %s",
          role.getRoleArn( ),
          ownerFullName ) );
    }
    return role.getRoleArn( );
  }
}
