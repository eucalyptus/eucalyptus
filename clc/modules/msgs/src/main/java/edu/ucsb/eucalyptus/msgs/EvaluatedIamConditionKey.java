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
