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
package com.eucalyptus.auth.euare.persist;

import java.util.Date;
import com.eucalyptus.auth.euare.persist.entities.ManagedPolicyVersionEntity;
import com.eucalyptus.auth.euare.principal.EuareManagedPolicyVersion;

/**
 *
 */
public class DatabaseManagedPolicyVersionProxy implements EuareManagedPolicyVersion {

  private final ManagedPolicyVersionEntity delegate;

  public DatabaseManagedPolicyVersionProxy( final ManagedPolicyVersionEntity delegate ) {
    this.delegate = delegate;
  }

  @Override
  public Integer getPolicyVersion( ) {
    return delegate.getPolicyVersion( );
  }

  @Override
  public Boolean isDefaultVersion( ) {
    return delegate.getDefaultPolicy( );
  }

  @Override
  public String getText( ) {
    return delegate.getText( );
  }

  @Override
  public Date getCreateDate( ) {
    return delegate.getCreationTimestamp( );
  }
}
