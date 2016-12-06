/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.simplequeue.bootstrap;

import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.util.SystemAccountProvider;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class SimpleQueueSystemAccountProvider implements SystemAccountProvider {

  @Override
  public String getAlias( ) {
    return AccountIdentifiers.SIMPLEQUEUE_SYSTEM_ACCOUNT;
  }

  @Override
  public boolean isCreateAdminAccessKey( ) {
    return false;
  }

  @Override
  public List<SystemAccountRole> getRoles( ) {
    return Collections.emptyList();
  }
}
