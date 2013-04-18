/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudwatch.domain;

public class DimensionEntity implements Comparable<DimensionEntity>{
  private String name;
  private String value;
  public DimensionEntity() {
    super();
  }
  public DimensionEntity(String name, String value) {
    this.name = name;
    this.value = value;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getValue() {
    return value;
  }
  public void setValue(String value) {
    this.value = value;
  }
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    DimensionEntity other = (DimensionEntity) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }
  @Override
  public String toString() {
    return "Dimension [name=" + name + ", value=" + value + "]";
  }
  @Override
  public int compareTo(DimensionEntity other) {
    if (other == null) {
      return -1;
    } else {
      int nameCompare = stringCompare(name, other.name);
      if (nameCompare != 0) {
        return nameCompare;
      } else {
        return stringCompare(value, other.value);
      }
    }
  }
  
  private int stringCompare(String a, String b) {
    if (a == null && b == null) {
      return 0;
    }
    if (b == null) return 1;
    if (a == null) return -1;
    return a.compareTo(b);
  }
}
