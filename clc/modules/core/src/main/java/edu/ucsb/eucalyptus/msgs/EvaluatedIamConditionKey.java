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
package edu.ucsb.eucalyptus.msgs;

import javax.annotation.Nullable;
import com.google.common.base.Function;

/**
*
*/
public class EvaluatedIamConditionKey extends EucalyptusData {
  private static final long serialVersionUID = 1L;
  private String key;
  private String value;

  public EvaluatedIamConditionKey() {
  }

  public EvaluatedIamConditionKey( final String key, final String value ) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey( final String key ) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue( final String value ) {
    this.value = value;
  }

  public static Function<EvaluatedIamConditionKey,String> key( ) {
    return PropertyGetters.KEY;
  }

  public static Function<EvaluatedIamConditionKey,String> value( ) {
    return PropertyGetters.VALUE;
  }

  private enum PropertyGetters implements Function<EvaluatedIamConditionKey,String> {
    KEY {
      @Nullable
      @Override
      public String apply( @Nullable final EvaluatedIamConditionKey evaluatedIamConditionKey ) {
        return evaluatedIamConditionKey == null ? null : evaluatedIamConditionKey.getKey();
      }
    },
    VALUE {
      @Nullable
      @Override
      public String apply( @Nullable final EvaluatedIamConditionKey evaluatedIamConditionKey ) {
        return evaluatedIamConditionKey == null ? null : evaluatedIamConditionKey.getValue();
      }
    }
  }
}
