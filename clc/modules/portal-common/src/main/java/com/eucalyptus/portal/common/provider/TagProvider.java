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
package com.eucalyptus.portal.common.provider;

import java.util.Set;
import javax.annotation.Nonnull;
import com.eucalyptus.auth.principal.User;

/**
 * Tag provider represents a source of tag information
 */
public interface TagProvider {

  /**
   * The vendor supported by this provider, e.g. "ec2"
   */
  @Nonnull
  String getVendor( );

  /**
   * Get the tag keys that the given user can access.
   *
   * @param user The user
   */
  @Nonnull
  Set<String> getTagKeys( @Nonnull User user );

}
