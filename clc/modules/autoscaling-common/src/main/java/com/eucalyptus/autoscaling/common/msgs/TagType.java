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
package com.eucalyptus.autoscaling.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.autoscaling.common.AutoScalingMessageValidation;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class TagType extends EucalyptusData {

  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.NAME )
  private String resourceId;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.TAG_RESOURCE )
  private String resourceType;
  @Nonnull
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.STRING_128 )
  private String key;
  @AutoScalingMessageValidation.FieldRegex( AutoScalingMessageValidation.FieldRegexValue.ESTRING_256)
  private String value;
  private Boolean propagateAtLaunch;

  public String getResourceId( ) {
    return resourceId;
  }

  public void setResourceId( String resourceId ) {
    this.resourceId = resourceId;
  }

  public String getResourceType( ) {
    return resourceType;
  }

  public void setResourceType( String resourceType ) {
    this.resourceType = resourceType;
  }

  public String getKey( ) {
    return key;
  }

  public void setKey( String key ) {
    this.key = key;
  }

  public String getValue( ) {
    return value;
  }

  public void setValue( String value ) {
    this.value = value;
  }

  public Boolean getPropagateAtLaunch( ) {
    return propagateAtLaunch;
  }

  public void setPropagateAtLaunch( Boolean propagateAtLaunch ) {
    this.propagateAtLaunch = propagateAtLaunch;
  }
}
