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
package com.eucalyptus.cluster.service.migration;

import java.security.SecureRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import com.eucalyptus.cluster.common.msgs.InstanceType;
import com.eucalyptus.cluster.common.msgs.VirtualMachineType;
import com.eucalyptus.cluster.service.vm.ClusterVm;
import com.eucalyptus.crypto.Crypto;

/**
 *
 */
public class Migrations {

  private static final Supplier<SecureRandom> secureRandomSupplier = Crypto.getSecureRandomSupplier( );
  private static final String credentialChars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int credentialLength = 16;

  public static String generateCredential( ) {
    return secureRandomSupplier.get( ).ints( credentialLength )
        .mapToObj( rnd -> String.valueOf( credentialChars.charAt( Math.abs( rnd ) % credentialChars.length( ) ) ) )
        .collect( Collectors.joining( ) );
  }

}
