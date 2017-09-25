/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cloudformation.entity;

import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.cloudformation.common.CloudFormationMetadata;

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
