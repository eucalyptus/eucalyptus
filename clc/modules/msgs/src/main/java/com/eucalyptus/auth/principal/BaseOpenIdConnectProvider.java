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
package com.eucalyptus.auth.principal;

import java.util.List;
import com.eucalyptus.auth.AuthException;

/**
 *
 */
public interface BaseOpenIdConnectProvider {

  String getAccountNumber( ) throws AuthException;

  String getArn( ) throws AuthException;

  /**
   * The provider URL as used in the ARN
   *
   * This value does not start with the scheme or include any port.
   *
   * @return The account unique url for the provider.
   */
  String getUrl( );

  /**
   * The host from the url.
   */
  String getHost( );

  /**
   * The port for the provider.
   */
  Integer getPort( );

  /**
   * The path from the url.
   */
  String getPath( );

  List<String> getClientIds( ) throws AuthException;

  List<String> getThumbprints( ) throws AuthException;
}
