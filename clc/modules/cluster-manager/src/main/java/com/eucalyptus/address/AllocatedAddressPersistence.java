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
package com.eucalyptus.address;

import java.util.List;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.compute.common.CloudMetadata.AddressMetadata;
import com.eucalyptus.compute.common.internal.address.AllocatedAddressEntity;
import com.eucalyptus.util.Callback;
import com.google.common.base.Predicate;

/**
 *
 */
interface AllocatedAddressPersistence {

  AllocatedAddressEntity save(
      AllocatedAddressEntity address
  ) throws AllocatedAddressPersistenceException;

  AllocatedAddressEntity updateByExample(
      AllocatedAddressEntity example,
      OwnerFullName ownerFullName,
      String key,
      Callback<AllocatedAddressEntity> updateCallback
  ) throws AllocatedAddressPersistenceException;

  boolean delete(
      AddressMetadata address,
      Predicate<? super AllocatedAddressEntity> precondition
  ) throws AllocatedAddressPersistenceException;

  List<AllocatedAddressEntity> list(
      OwnerFullName ownerFullName
  ) throws AllocatedAddressPersistenceException;

}
