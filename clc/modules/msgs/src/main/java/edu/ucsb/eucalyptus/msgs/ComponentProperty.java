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
package edu.ucsb.eucalyptus.msgs;

public class ComponentProperty extends EucalyptusData {

  private String type;
  private String displayName;
  private String value;
  private String qualifiedName;

  public ComponentProperty( ) {
  }

  public ComponentProperty( String type, String displayName, String value, String qualifiedName ) {
    this.type = type;
    this.displayName = displayName;
    this.value = value;
    this.qualifiedName = qualifiedName;
  }

  public String getType( ) {
    return type;
  }

  public void setType( String type ) {
    this.type = type;
  }

  public String getQualifiedName( ) {
    return qualifiedName;
  }

  public void setQualifiedName( String qualifiedName ) {
    this.qualifiedName = qualifiedName;
  }

  public String getDisplayName( ) {
    return displayName;
  }

  public void setDisplayName( String displayName ) {
    this.displayName = displayName;
  }

  public String getValue( ) {
    return value;
  }

  public void setValue( String value ) {
    this.value = value;
  }
}
