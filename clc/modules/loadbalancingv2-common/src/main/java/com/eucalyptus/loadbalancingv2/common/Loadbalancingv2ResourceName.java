/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.common;

import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.util.CompatFunction;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import io.vavr.control.Option;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;

public class Loadbalancingv2ResourceName {

  private static final String prefix = "arn:aws:";
  private static final Splitter nameSpliter = Splitter.on(':');
  private static final Splitter resourceSpliter = Splitter.on('/');

  private static final int PART_SERVICE = 2;
  private static final int PART_NAMESPACE = 4;
  private static final int PART_RESOURCE = 5;

  private static final int RESOURCE_PART_TYPE = 0;

  private final String resourceName;
  private final String service;
  private final String namespace; // account number if you don't speak ARN
  private final String type;
  private final String subType;
  private final String name1;
  private final String id1;
  @Nullable
  private final String id2;
  @Nullable
  private final String id3;

  public enum Type {
    listener( "listener", true, true ),
    listener_rule( "listener rule", true, true ),
    loadbalancer( "loadbalancer", true, false ),
    targetgroup( "target group", false, false ),
    ;

    private final String description;
    private final boolean subTyped;   // has net/app subtypes
    private final boolean scoped;     // owned by another resource type

    Type(
        final String description,
        final boolean subTyped,
        final boolean scoped
    ) {
      this.description = description;
      this.subTyped = subTyped;
      this.scoped = scoped;
    }

    public static Option<Type> forResourceType(final String resourceType) {
      try {
        return Option.some(Type.valueOf(resourceType.replace('-', '_')));
      } catch (final IllegalArgumentException ex) {
        return Option.none();
      }
    }

    public String getResourceType( ) {
      return name().replace('_', '-');
    }

    public String describe() {
      return description;
    }

    public boolean isScoped() {
      return scoped;
    }

    public boolean isSubTyped() {
      return subTyped;
    }

    public CompatFunction<Loadbalancingv2ResourceName,String> id() {
      return name -> name.getId(this);
    }
  }

  public static String generateId() {
    return Crypto.generateLongId("").substring(2).toLowerCase();
  }

  public Loadbalancingv2ResourceName(
      final String resourceName,
      final String service,
      final String namespace,
      final String type,
      @Nullable final String subType,
      final String name1,
      final String id1,
      @Nullable final String id2,
      @Nullable final String id3
  ) {
    this.resourceName = resourceName;
    this.service = service;
    this.namespace = namespace;
    this.type = type;
    this.subType = subType;
    this.name1 = name1;
    this.id1 = id1;
    this.id2 = id2;
    this.id3 = id3;
  }

  public static Loadbalancingv2ResourceName parse( final String resourceName ) throws InvalidResourceNameException {
    return parse( resourceName, null );
  }

  public static Loadbalancingv2ResourceName parse(
      final String resourceName,
      @Nullable final Type type
  ) throws InvalidResourceNameException {
    if(resourceName == null || !resourceName.startsWith(prefix)) {
      throw new InvalidResourceNameException( resourceName );
    }

    final Iterable<String> nameParts = nameSpliter.split(resourceName);
    final int namePartCount = Iterables.size(nameParts);
    if (namePartCount != 6) {
      throw new InvalidResourceNameException(resourceName);
    }

    if (!"elasticloadbalancing".equals(Iterables.get(nameParts, PART_SERVICE))) {
      throw new InvalidResourceNameException( resourceName );
    }

    final String resourcePath = Iterables.get(nameParts, PART_RESOURCE);
    final Iterable<String> resourceParts = resourceSpliter.split(resourcePath);
    final int resourcePartCount = Iterables.size(resourceParts);
    if (resourcePartCount < 3 || resourcePartCount > 6) {
      throw new InvalidResourceNameException(resourceName);
    }
    final Option<Type> resourceTypeOption = Option.of(type)
        .orElse( () -> Type.forResourceType(Iterables.get(resourceParts, RESOURCE_PART_TYPE)));
    if ( resourceTypeOption.isDefined() ) {
      if(!resourceTypeOption.get().getResourceType().equals(Iterables.get(resourceParts, RESOURCE_PART_TYPE))) {
        throw new InvalidResourceNameException(resourceName);
      }
    } else {
      throw new InvalidResourceNameException(resourceName);
    }
    final Type resourceType = resourceTypeOption.get();
    final Iterator<String> resourcePathIterator = resourceParts.iterator();
    final String typeStr = resourcePathIterator.next();
    final String subType;
    if (resourceType.isSubTyped()) {
      subType = resourcePathIterator.next();
    } else {
      subType = null;
    }
    final String name1 = resourcePathIterator.next();
    return new Loadbalancingv2ResourceName(
        resourceName,
        Iterables.get( nameParts, PART_SERVICE ),
        Iterables.get( nameParts, PART_NAMESPACE ),
        typeStr,
        subType,
        name1,
        Iterators.getNext(resourcePathIterator, null),
        Iterators.getNext(resourcePathIterator, null),
        Iterators.getNext(resourcePathIterator, null)
    );
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getService() {
    return service;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getType() {
    return type;
  }

  public String getSubType( final Type type ) {
    if ( !type.isSubTyped() ) throw new IllegalStateException( "Type not subtyped" );
    return subType;
  }

  public String getScope( final Type type ) {
    if ( !type.isScoped() ) throw new IllegalStateException( "Type not scoped" );
    return id2;
  }

  public String getName( ) {
    return name1;
  }

  public String getId() {
    return getId(
        Type.forResourceType(getType())
            .getOrElseThrow(() -> new IllegalStateException("Type error " + getType())));
  }

  public String getId(final Type requestedType) {
    final Type arnType = Type.forResourceType(getType())
        .getOrElseThrow(() -> new IllegalStateException("Invalid type for arn"));
    switch (requestedType) {
      case listener:
        switch (arnType) {
          case listener:
            return id2;
          case listener_rule:
            return id2;
        }
        break;
      case listener_rule:
        if (arnType == Type.listener_rule) {
          return id3;
        }
        break;
      case loadbalancer:
        switch (arnType) {
          case listener:
          case listener_rule:
          case loadbalancer:
            return id1;
        }
        break;
      case targetgroup:
        if (arnType == Type.targetgroup) {
          return id1;
        }
        break;
    }
    throw new IllegalStateException("Invalid type for arn");
  }

  public String toString() {
    return resourceName;
  }

  public static Set<String> simpleNames( final Iterable<String> namesAndArns ) {
    return Sets.newHashSet( Iterables.filter( namesAndArns, Predicates.not( isResourceName() ) ) );
  }

  public static Set<String> arns( final Iterable<String> namesAndArns ) {
    return Sets.newHashSet( Iterables.filter( namesAndArns, isResourceName() ) );
  }

  public static Predicate<String> isResourceName() {
    return ResourceNamePredicate.INSTANCE;
  }

  private enum ResourceNamePredicate implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply( final String value ) {
      return value != null && value.startsWith( prefix );
    }
  }

  public static final class InvalidResourceNameException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidResourceNameException( final String resourceName ) {
      super( "Invalid resource name: " + resourceName );
    }
  }
}
