/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.cloudformation.CloudFormationMetadata;

import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

/**
 * Created by ethomas on 1/6/16.
 */
@Entity
@PersistenceContext( name = "eucalyptus_cloudformation" )
@Table( name = "stacks" )
public class StackEntity extends VersionedStackEntity implements CloudFormationMetadata.StackMetadata {
  public StackEntity() {
  }

  /**
   * Display name is the part of the ARN (stackId) following the type.
   *
   * $StackName/$NaturalId
   *
   * @return The name.
   */
  @Override
  public String getDisplayName() {
    return String.format( "%s/%s", getStackName( ), getNaturalId( ) );
  }

  @Override
  public OwnerFullName getOwner() {
    return AccountFullName.getInstance(accountId);
  }

  public static StackEntity exampleUndeletedWithAccount(String accountId) {
    StackEntity stackEntity = new StackEntity();
    stackEntity.setAccountId(accountId);
    stackEntity.setRecordDeleted(false);
    return stackEntity;
  }

  public static class Output {
    String description;
    String key;
    String stringValue;
    String jsonValue;
    String condition;
    boolean ready = false;
    boolean allowedByCondition = true;

    public Output() {
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getCondition() {
      return condition;
    }

    public void setCondition(String condition) {
      this.condition = condition;
    }

    public boolean isReady() {
      return ready;
    }

    public void setReady(boolean ready) {
      this.ready = ready;
    }

    public boolean isAllowedByCondition() {
      return allowedByCondition;
    }

    public void setAllowedByCondition(boolean allowedByCondition) {
      this.allowedByCondition = allowedByCondition;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public String getJsonValue() {
      return jsonValue;
    }

    public void setJsonValue(String jsonValue) {
      this.jsonValue = jsonValue;
    }
  }

  public static class Parameter {
    String key;
    String stringValue;
    String jsonValue;
    boolean noEcho = false;

    public Parameter() {
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    public String getJsonValue() {
      return jsonValue;
    }

    public void setJsonValue(String jsonValue) {
      this.jsonValue = jsonValue;
    }

    public boolean isNoEcho() {
      return noEcho;
    }

    public void setNoEcho(boolean noEcho) {
      this.noEcho = noEcho;
    }
  }
}
