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
package com.eucalyptus.compute.common;

/** *******************************************************************************/
public class CreateKeyPairResponseType extends VmKeyPairMessage {

  private String keyName;
  private String keyFingerprint;
  private String keyMaterial;

  public String getKeyName( ) {
    return keyName;
  }

  public void setKeyName( String keyName ) {
    this.keyName = keyName;
  }

  public String getKeyFingerprint( ) {
    return keyFingerprint;
  }

  public void setKeyFingerprint( String keyFingerprint ) {
    this.keyFingerprint = keyFingerprint;
  }

  public String getKeyMaterial( ) {
    return keyMaterial;
  }

  public void setKeyMaterial( String keyMaterial ) {
    this.keyMaterial = keyMaterial;
  }
}
