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
package com.eucalyptus.auth.euare.common.identity.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class Policy extends EucalyptusData {

  private String versionId;
  private String name;
  private String scope;
  private String policy;
  private String hash;

  public String getVersionId( ) {
    return versionId;
  }

  public void setVersionId( String versionId ) {
    this.versionId = versionId;
  }

  public String getName( ) {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getScope( ) {
    return scope;
  }

  public void setScope( String scope ) {
    this.scope = scope;
  }

  public String getPolicy( ) {
    return policy;
  }

  public void setPolicy( String policy ) {
    this.policy = policy;
  }

  public String getHash( ) {
    return hash;
  }

  public void setHash( String hash ) {
    this.hash = hash;
  }
}
