/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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
package com.eucalyptus.entities;

import java.util.Collection;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

/**
 * Type-safe immutable entity restriction specification.
 */
public abstract class EntityRestriction<E> {

  /**
   * Get the entity class.
   *
   * @return The entity class being restricted.
   */
  @Nonnull
  public abstract Class<E> getEntityClass( );

  abstract Expression<Boolean> build( CriteriaBuilder builder, Path<E> path );

  static final class ConjunctionEntityRestriction<E> extends EntityRestriction<E> {
    private final Class<E> entityClass;
    private final Iterable<EntityRestriction<E>> restrictions;

    ConjunctionEntityRestriction(
        final Class<E> entityClass,
        final Iterable<EntityRestriction<E>> restrictions
    ) {
      this.entityClass = entityClass;
      this.restrictions = restrictions;
    }

    @Nonnull
    @Override
    public Class<E> getEntityClass( ) {
      return entityClass;
    }

    @Override
    public Expression<Boolean> build( final CriteriaBuilder builder, final Path<E> root ) {
      final javax.persistence.criteria.Predicate conjunction = builder.conjunction( );
      for ( final EntityRestriction<E> restriction : restrictions ) {
        conjunction.getExpressions( ).add( restriction.build( builder, root ) );
      }
      return conjunction;
    }
  }

  static final class DisjunctionEntityRestriction<E> extends EntityRestriction<E> {
    private final Class<E> entityClass;
    private final Iterable<EntityRestriction<E>> restrictions;

    DisjunctionEntityRestriction(
        final Class<E> entityClass,
        final Iterable<EntityRestriction<E>> restrictions
    ) {
      this.entityClass = entityClass;
      this.restrictions = restrictions;
    }

    @Nonnull
    @Override
    public Class<E> getEntityClass( ) {
      return entityClass;
    }

    @Override
    public Expression<Boolean> build( final CriteriaBuilder builder, final Path<E> root ) {
      final javax.persistence.criteria.Predicate disjunction = builder.disjunction( );
      for ( final EntityRestriction<E> restriction : restrictions ) {
        disjunction.getExpressions( ).add( restriction.build( builder, root ) );
      }
      return disjunction;
    }
  }

  static abstract class PropertyEntityRestrictionSupport<E,V> extends EntityRestriction<E> {
    private final Class<E> entityClass;
    private final SingularAttribute<? super E,V> attribute;

    PropertyEntityRestrictionSupport(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute
    ) {
      this.entityClass = entityClass;
      this.attribute = attribute;
    }

    @Nonnull
    @Override
    public Class<E> getEntityClass( ) {
      return entityClass;
    }

    public SingularAttribute<? super E, V> getAttribute( ) {
      return attribute;
    }
  }

  static abstract class PropertyEntityValueRestrictionSupport<E,V> extends PropertyEntityRestrictionSupport<E,V> {
    private final V value;

    PropertyEntityValueRestrictionSupport(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute,
        final V value
    ) {
      super( entityClass, attribute );
      this.value = value;
    }

    public V getValue( ) {
      return value;
    }
  }

  static final class EqualPropertyEntityValueRestriction<E,V> extends PropertyEntityValueRestrictionSupport<E,V> {
    EqualPropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute,
        final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.equal( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class EqualIgnoreCasePropertyEntityValueRestriction<E> extends PropertyEntityValueRestrictionSupport<E,String> {
    EqualIgnoreCasePropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, String> attribute,
        final String value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.equal( builder.lower( root.get( getAttribute( ) ) ), getValue( ).toLowerCase( ) );
    }
  }

  static final class NotEqualPropertyEntityValueRestriction<E,V> extends PropertyEntityValueRestrictionSupport<E,V> {
    NotEqualPropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute,
        final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.notEqual( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class LikePropertyEntityValueRestriction<E> extends PropertyEntityValueRestrictionSupport<E,String> {
    LikePropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, String> attribute,
        final String value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.like( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class NotLikePropertyEntityValueRestriction<E> extends PropertyEntityValueRestrictionSupport<E,String> {
    NotLikePropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, String> attribute,
        final String value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.notLike( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class GreaterThanPropertyEntityValueRestriction<E,V extends Number> extends PropertyEntityValueRestrictionSupport<E,V> {
    GreaterThanPropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute,
        final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.gt( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class GreaterThanOrEqualToPropertyEntityValueRestriction<E,V extends Number> extends PropertyEntityValueRestrictionSupport<E,V> {
    GreaterThanOrEqualToPropertyEntityValueRestriction(
      final Class<E> entityClass,
      final SingularAttribute<? super E, V> attribute,
      final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.ge( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class LessThanPropertyEntityValueRestriction<E,V extends Number> extends PropertyEntityValueRestrictionSupport<E,V> {
    LessThanPropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute,
        final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.lt( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class LessThanOrEqualToPropertyEntityValueRestriction<E,V extends Number> extends PropertyEntityValueRestrictionSupport<E,V> {
    LessThanOrEqualToPropertyEntityValueRestriction(
      final Class<E> entityClass,
      final SingularAttribute<? super E, V> attribute,
      final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.le( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class DateBeforePropertyEntityValueRestriction<E> extends PropertyEntityValueRestrictionSupport<E,Date> {
    DateBeforePropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, Date> attribute,
        final Date value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.lessThan( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class DateAfterPropertyEntityValueRestriction<E> extends PropertyEntityValueRestrictionSupport<E,Date> {
    DateAfterPropertyEntityValueRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, Date> attribute,
        final Date value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.greaterThan( root.get( getAttribute( ) ), getValue( ) );
    }
  }

  static final class NullPropertyEntityRestriction<E,V> extends PropertyEntityRestrictionSupport<E,V> {
    NullPropertyEntityRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute
    ) {
      super( entityClass, attribute );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isNull( root.get( getAttribute( ) ) );
    }
  }

  static final class NotNullPropertyEntityRestriction<E,V> extends PropertyEntityRestrictionSupport<E,V> {
    NotNullPropertyEntityRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, V> attribute
    ) {
      super( entityClass, attribute );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isNotNull( root.get( getAttribute( ) ) );
    }
  }

  static final class TruePropertyEntityRestriction<E> extends PropertyEntityRestrictionSupport<E,Boolean> {
    TruePropertyEntityRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, Boolean> attribute
    ) {
      super( entityClass, attribute );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isTrue( root.get( getAttribute( ) ) );
    }
  }

  static final class FalsePropertyEntityRestriction<E> extends PropertyEntityRestrictionSupport<E,Boolean> {
    FalsePropertyEntityRestriction(
        final Class<E> entityClass,
        final SingularAttribute<? super E, Boolean> attribute
    ) {
      super( entityClass, attribute );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isFalse( root.get( getAttribute( ) ) );
    }
  }


  static abstract class CollectionPropertyEntityRestrictionSupport<E,C extends Collection<V>,V> extends EntityRestriction<E> {
    private final Class<E> entityClass;
    private final PluralAttribute<E, C, V> attribute;

    CollectionPropertyEntityRestrictionSupport(
        final Class<E> entityClass,
        final PluralAttribute<E, C, V> attribute
    ) {
      this.entityClass = entityClass;
      this.attribute = attribute;
    }

    @Nonnull
    @Override
    public Class<E> getEntityClass( ) {
      return entityClass;
    }

    public PluralAttribute<E, C, V> getAttribute( ) {
      return attribute;
    }
  }

  static abstract class CollectionPropertyEntityValueRestrictionSupport<E,C extends Collection<V>,V> extends CollectionPropertyEntityRestrictionSupport<E,C,V> {
    private final V value;

    CollectionPropertyEntityValueRestrictionSupport(
        final Class<E> entityClass,
        final PluralAttribute<E, C, V> attribute,
        final V value
    ) {
      super( entityClass, attribute );
      this.value = value;
    }

    public V getValue( ) {
      return value;
    }
  }

  static final class EmptyPropertyEntityRestriction<E,C extends Collection<V>,V> extends CollectionPropertyEntityRestrictionSupport<E,C,V> {
    EmptyPropertyEntityRestriction(
        final Class<E> entityClass,
        final PluralAttribute<E, C, V> attribute
    ) {
      super( entityClass, attribute );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isEmpty( root.get( getAttribute( ) ) );
    }
  }

  static final class NotEmptyPropertyEntityRestriction<E,C extends Collection<V>,V> extends CollectionPropertyEntityRestrictionSupport<E,C,V> {
    NotEmptyPropertyEntityRestriction(
        final Class<E> entityClass,
        final PluralAttribute<E, C, V> attribute
    ) {
      super( entityClass, attribute );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isNotEmpty( root.get( getAttribute( ) ) );
    }
  }

  static final class MemberPropertyEntityValueRestriction<E,C extends Collection<V>,V> extends CollectionPropertyEntityValueRestrictionSupport<E,C,V> {
    MemberPropertyEntityValueRestriction(
        final Class<E> entityClass,
        final PluralAttribute<E, C, V> attribute,
        final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isMember( getValue( ), root.get( getAttribute( ) ) );
    }
  }

  static final class NotMemberPropertyEntityValueRestriction<E,C extends Collection<V>,V> extends CollectionPropertyEntityValueRestrictionSupport<E,C,V> {
    NotMemberPropertyEntityValueRestriction(
        final Class<E> entityClass,
        final PluralAttribute<E, C, V> attribute,
        final V value
    ) {
      super( entityClass, attribute, value );
    }

    public Expression<Boolean> build( CriteriaBuilder builder, Path<E> root ) {
      return builder.isNotMember( getValue( ), root.get( getAttribute( ) ) );
    }
  }
}
