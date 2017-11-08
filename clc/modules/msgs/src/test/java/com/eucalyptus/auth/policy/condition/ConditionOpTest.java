/*************************************************************************
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.auth.policy.condition;

import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Test;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class ConditionOpTest {

  private static final List<Class<? extends ConditionOp>> conditionOps = ImmutableList.<Class<? extends ConditionOp>>builder()
      .add(IpAddress.class)
      .add(NotIpAddress.class)
      .add(Bool.class)
      .add(DateEquals.class)
      .add(DateGreaterThan.class)
      .add(DateGreaterThanEquals.class)
      .add(DateLessThan.class)
      .add(DateLessThanEquals.class)
      .add(DateNotEquals.class)
      .add(NumericEquals.class)
      .add(NumericGreaterThan.class)
      .add(NumericGreaterThanEquals.class)
      .add(NumericLessThan.class)
      .add(NumericLessThanEquals.class)
      .add(NumericNotEquals.class)
      .add(StringEquals.class)
      .add(StringEqualsIgnoreCase.class)
      .add(StringLike.class)
      .add(StringNotEquals.class)
      .add(StringNotEqualsIgnoreCase.class)
      .add( StringNotLike.class )
      .build();

  /**
   * Verifies no duplicate condition names (see PolicyCondition annotation)
   */
  @Test
  public void testPolicyConditionAnnotations() {
    final ImmutableList<String> conditions =
        ImmutableList.copyOf(Iterables.concat(Iterables.transform(conditionOps, ToConditions.INSTANCE)));
    assertTrue( "No duplicate conditions", Iterables.all( conditions, Predicates.not( duplicatedIn( conditions ) ) ) );
  }

  @Test
  public void instantiationTest() {
    Iterables.transform( conditionOps, ToInstance.INSTANCE );
  }

  /**
   * Verifies conditions evaluate to false for empty arguments
   */
  @Test
  public void emptyArgumentsTest() {
    final Iterable<ConditionOp> conditionOperations = Iterables.transform( Iterables.filter( conditionOps,
        Predicates.not(Predicates.or(
            Predicates.<Class<? extends ConditionOp>>equalTo(Bool.class),
            Predicates.<Class<? extends ConditionOp>>equalTo(StringEquals.class),
            Predicates.<Class<? extends ConditionOp>>equalTo(StringEqualsIgnoreCase.class),
            Predicates.<Class<? extends ConditionOp>>equalTo(StringLike.class))
    )
    ), ToInstance.INSTANCE);
    assertTrue( "Empty is false", Iterables.all( conditionOperations, apply( "", "" ) ) );
  }

  private static Predicate<Object> duplicatedIn( final List<?> objects ) {
    return new Predicate<Object>(  ) {
      @Override
      public boolean apply( final Object o ) {
        return Iterables.filter( objects, Predicates.and( Predicates.equalTo(o), Predicates.not(isSameObject(o)) )).iterator().hasNext();
      }
    };
  }

  private static Predicate<ConditionOp> apply( final String key, final String value ) {
    return new Predicate<ConditionOp>() {
      @Override
      public boolean apply( final ConditionOp conditionOp ) {
        System.out.println( conditionOp );
        return !conditionOp.check( key, value );
      }
    };
  }

  private static Predicate<Object> isSameObject( final Object o1 ) {
    return new Predicate<Object>() {
      @Override
      public boolean apply( final Object o2 ) {
        return o1 == o2;
      }
    };
  }

  enum ToConditions implements Function<Class<?>,Iterable<String>> {
    INSTANCE;

    @Override
    public Iterable<String> apply(final Class<?> aClass) {
      final PolicyCondition condition = Ats.from(aClass).get( PolicyCondition.class );
      return condition == null ?
          Collections.<String>emptyList() :
          Arrays.asList( condition.value() );
    }
  }

  enum ToInstance implements Function<Class<? extends ConditionOp>,ConditionOp> {
    INSTANCE;

    @Override
    public ConditionOp apply( final Class<? extends ConditionOp> aClass) {
      try {
        return aClass.newInstance();
      } catch (Exception e) {
        throw Exceptions.toUndeclared(e);
      }
    }
  }
}
