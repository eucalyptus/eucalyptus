/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions.ErrorMessages;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class Classes {
  /**
   * Returns a {@link Predicate} which determines if the class or interface represented by
   * {@link Predicate#apply(Class input)} is either the same as, or is a
   * superclass or superinterface of, the class or interface represented by
   * {@link #assignableTo(Class type)}. It
   * returns true if so; otherwise it returns false. If this Class object represents a primitive
   * type, this
   * method returns true if the specified Class parameter is exactly this Class object; otherwise it
   * returns
   * false.
   *
   * @param type
   * @return
   */
  public static Predicate<Class> assignableTo( final Class<?> type ) {
    return new Predicate<Class>( ) {

      @Override
      public boolean apply( Class input ) {
        return input.isAssignableFrom( type );
      }
    };
  }

  enum ClassNameToName implements Function<Object, String> {
    INSTANCE;
    @Override
    public String apply( final Object arg0 ) {
      return WhateverAsClass.INSTANCE.apply( arg0 ).getName( );
    }
  }

  enum ClassNameToSimpleName implements Function<Object, String> {
    INSTANCE;
    @Override
    public String apply( final Object arg0 ) {
      return WhateverAsClass.INSTANCE.apply( arg0 ).getSimpleName( );
    }
  }

  enum ClassNameToCanonicalName implements Function<Object, String> {
    INSTANCE;
    @Override
    public String apply( final Object arg0 ) {
      return WhateverAsClass.INSTANCE.apply( arg0 ).getCanonicalName( );
    }
  }

  public static Function<Object, String> nameFunction( ) {
    return ClassNameToName.INSTANCE;
  }

  public static String simpleName( final Object object ) {
    return simpleName( object, null );
  }

  public static String simpleName( final Object object, final String nullDefault ) {
    return object == nullDefault ? null : simpleNameFunction().apply( object );
  }

  public static Function<Object, String> simpleNameFunction( ) {
    return ClassNameToSimpleName.INSTANCE;
  }

  public static Function<Object, String> canonicalNameFunction( ) {
    return ClassNameToCanonicalName.INSTANCE;
  }

  public static Predicate<Class<?>> subclassOf( final Class<?> target ) {
    return new Predicate<Class<?>>() {
      @Override
      public boolean apply( final Class<?> value ) {
        return value != null && target.isAssignableFrom( value );
      }
    };
  }

  @ErrorMessages( Classes.class )
  enum ErrorMessageMap implements Function<Class, String> {
    INSTANCE;
    private static final//
    Map<Class, String> errorMessages = new ImmutableMap.Builder<Class, String>( ) {
                                       {
                                         this.put(
                                           IllegalAccessException.class,
                                                   "Either: "
                                                       + "the number of actual and formal parameters differ;"
                                                       + "an unwrapping conversion for primitive arguments fails; "
                                                       + "this constructor pertains to an enum type;"
                                                       + "a parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion." );
                                         this.put( NoSuchMethodException.class, "" );
                                         this.put( SecurityException.class, "" );
                                         this.put( ExceptionInInitializerError.class, "" );
                                         this.put( InvocationTargetException.class, "" );
                                         this.put( InstantiationException.class, "" );
                                         this.put( IllegalArgumentException.class, "" );
                                       }
                                     }.build( );

    @Override
    public String apply( Class input ) {
      return errorMessages.get( input );
    }

  }

  public static class InstanceBuilder<T> {
    private final Class<T>     type;
    private final List<Class>  argTypes = Lists.newArrayList( );
    private final List<Object> args     = Lists.newArrayList( );

    public InstanceBuilder( Class<T> type ) {
      this.type = type;
    }

    public InstanceBuilder<T> arg( Object arg ) {
      if ( arg == null ) {
        throw new IllegalArgumentException( "Cannot supply a null value argument w/o specifying the type." );
      } else {
        return arg( arg, arg.getClass( ) );
      }
    }

    public InstanceBuilder<T> arg( Object arg, Class type ) {
      this.argTypes.add( type );
      this.args.add( arg );
      return this;
    }

    /**
     * @return
     * @throws UndeclaredThrowableException if the called constructor throws either:
     *           {@link InvocationTargetException} the value of
     *           {@link InvocationTargetException#getCause()} is rethrown as
     *           {@link UndeclaredThrowableException#UndeclaredThrowableException(Throwable)}.
     *           {@link NoSuchMethodException} is rethrown as
     *           {@link UndeclaredThrowableException#UndeclaredThrowableException(Throwable)}.
     */
    public T newInstance( ) throws UndeclaredThrowableException {
      if ( this.type.isEnum( ) ) {
        return this.type.getEnumConstants( )[0];
      } else {
        try {
          Constructor<T> constructor = this.type.getConstructor( this.argTypes.toArray( new Class<?>[] {} ) );
          if ( !constructor.isAccessible( ) ) {
            constructor.setAccessible( true );
          }
          return ( T ) constructor.newInstance( this.args.toArray( ) );
        } catch ( final InvocationTargetException ex ) {
          throw new UndeclaredThrowableException( ex.getCause( ), errorMessage( ex ) );
        } catch ( final NoSuchMethodException ex ) {
          throw new UndeclaredThrowableException( ex );
        } catch ( Exception ex ) {
          String message = errorMessage( ex, InstanceBuilder.errFstring,
                                         this.type.getClass( ), this.args, this.argTypes );
          throw new InstantiationError( Exceptions.string( message, ex ) );
        }
      }
    }

    private String errorMessage( final InvocationTargetException ex ) {
      return errorMessage( ex.getCause( ), errFstring, this.type.getClass( ), this.args, this.argTypes );
    }

    private static final String errFstring = "Failed to create new instance of type %s with arguments %s (of types: %s) because of: ";

    public static String errorMessage( Throwable ex, String message, Object... formatArgs ) {
      return Exceptions.builder( Classes.class ).exception( ex ).unknownException( "An unexpected error: " ).context( errFstring, formatArgs ).append(
        " because of: " ).build( );
    }

  }

  public static <T> InstanceBuilder<T> builder( Class<T> type ) {
    return new InstanceBuilder<T>( type );
  }

  public static <T> T newInstance( final Class<T> type, final Object... args ) {
    try {
      return new InstanceBuilder<T>( type ) {
        {
          for ( Object o : args ) {
            this.arg( o );
          }
        }
      }.newInstance( );
    } catch ( UndeclaredThrowableException ex ) {
      throw ex;
    }
  }

  enum WhateverAsClass implements Function<Object, Class> {
    INSTANCE;
    @Override
    public Class apply( final Object o ) {
      return ( o instanceof Class
                                 ? ( Class ) o
                                 : ( o != null
                                              ? o.getClass( )
                                              : null ) );
    }
  }

  enum ParentClass implements Function<Class, Class> {
    INSTANCE;
    @Override
    public Class apply( final Class type ) {
      return type.getSuperclass( );
    }
  }

  enum TransitiveClosureImplementedInterfaces implements Function<Class[], List<Class>> {
    INSTANCE;
    @Override
    public List<Class> apply( final Class[] types ) {
      final List<Class> ret = Lists.newArrayList( );
      if ( types.length == 0 ) {
        return ret;
      } else {
        for ( final Class t : types ) {
          if ( ( t == null ) || ( t == Object.class ) ) {
            continue;
          } else if ( t.isInterface( ) ) {
            ret.add( t );
          }
          if ( t.getInterfaces( ).length == 0 ) {
            continue;
          } else {
            final List<Class> next = TransitiveClosureImplementedInterfaces.INSTANCE.apply( t.getInterfaces( ) );
            ret.addAll( next );
          }
        }
        return ret;
      }
    }
  }

  enum BreadthFirstTransitiveClosure implements Function<Object, List<Class>> {
    INSTANCE;

    @Override
    public List<Class> apply( final Object input ) {
      final List<Class> ret = Lists.newArrayList( );
      final Class type = WhateverAsClass.INSTANCE.apply( input );
      if ( ( type == Object.class ) || ( type == null ) ) {
        return ret;
      } else if ( type.isInterface( ) ) {
        ret.add( type );
        final List<Class> superInterfaces = TransitiveClosureImplementedInterfaces.INSTANCE.apply( new Class[] { type } );
        ret.addAll( superInterfaces );
        return ret;
      } else {
        ret.add( type );
        final List<Class> superInterfaces = TransitiveClosureImplementedInterfaces.INSTANCE.apply( new Class[] { type } );
        ret.addAll( superInterfaces );
        final List<Class> next = this.apply( type.getSuperclass( ) );
        ret.addAll( next );
        return ret;
      }
    }

  }

  /**
   * Function for geting a linearized breadth-first list of classes which belong to the
   * transitive-closure of classes and interfaces implemented by {@code Object o}.
   *
   * @param o
   * @return
   */
  public static Function<Object, List<Class>> ancestors( ) {
    return BreadthFirstTransitiveClosure.INSTANCE;
  }

  /**
   * Get a linearized breadth-first list of classes which belong to the transitive-closure of
   * classes and interfaces implemented by {@code Object o}.
   *
   * @param o
   * @return
   */
  public static List<Class> ancestors( final Object o ) {
    return ancestors( ).apply( o );
  }

  enum ClassBreadthFirstTransitiveClosure implements Function<Object, List<Class>> {
    INSTANCE;

    @Override
    public List<Class> apply( final Object input ) {
      final List<Class> ret = Lists.newArrayList( );
      final Class type = WhateverAsClass.INSTANCE.apply( input );
      if ( ( type == Object.class ) || ( type == null ) ) {
        return ret;
      } else if ( type.isInterface( ) ) {
        return ret;
      } else {
        ret.add( type );
        final List<Class> next = this.apply( type.getSuperclass( ) );
        ret.addAll( next );
        return ret;
      }
    }

  }

  /**
   * Function for geting a linearized breadth-first list of classes which belong to the
   * transitive-closure of
   * classes implemented by {@code Object o}.
   *
   * @param o
   * @return
   */
  public static Function<Object, List<Class>> classAncestors( ) {
    return ClassBreadthFirstTransitiveClosure.INSTANCE;
  }

  /**
   * Get a linearized breadth-first list of classes which belong to the transitive-closure of
   * classes implemented by {@code Object o}.
   *
   * @param o
   * @return
   */
  public static List<Class> classAncestors( final Object o ) {
    return ClassBreadthFirstTransitiveClosure.INSTANCE.apply( o );
  }

  enum InterfaceBreadthFirstTransitiveClosure implements Function<Object, List<Class>> {
    INSTANCE;

    @Override
    public List<Class> apply( final Object input ) {
      final List<Class> ret = Lists.newArrayList( );
      final Class type = WhateverAsClass.INSTANCE.apply( input );
      if ( ( type == Object.class ) || ( type == null ) ) {
        return ret;
      } else {
        final List<Class> superInterfaces = TransitiveClosureImplementedInterfaces.INSTANCE.apply( new Class[] { type } );
        ret.addAll( superInterfaces );
        final List<Class> next = this.apply( type.getSuperclass( ) );
        ret.addAll( next );
        return ret;
      }
    }

  }

  @SuppressWarnings( "unchecked" )
  public static <T> Class<T> typeOf( final Object obj ) {
    return ( Class<T> ) WhateverAsClass.INSTANCE.apply( obj );
  }

  /**
   * Function for getting a linearized breadth-first list of classes which belong to the
   * transitive-closure of
   * interfaces implemented by {@code Object o}.
   *
   * @param o
   * @return
   */
  public static Function<Object, List<Class>> interfaceAncestors( ) {
    return InterfaceBreadthFirstTransitiveClosure.INSTANCE;
  }

  /**
   * Get a linearized breadth-first list of classes which belong to the transitive-closure of
   * interfaces implemented by {@code Object o}.
   *
   * @param o
   * @return
   */
  public static List<Class> interfaceAncestors( final Object o ) {
    return interfaceAncestors( ).apply( o );
  }

  enum GenericsBreadthFirstTransitiveClosure implements Function<Object, List<Class>> {
    INSTANCE;

    @Override
    public List<Class> apply( final Object input ) {
      final List<Class> ret = Lists.newArrayList( );
      final Class inputClass = WhateverAsClass.INSTANCE.apply( input );
      if ( !inputClass.isEnum( ) ) {
        ret.addAll( processTypeForGenerics( inputClass.getGenericSuperclass( ) ) );
      }
      ret.addAll( processTypeForGenerics( inputClass.getGenericInterfaces( ) ) );
      return ret;
    }

    private static List<Class> processTypeForGenerics( final Type... types ) {
      final List<Class> ret = Lists.newArrayList( );
      for ( final Type t : types ) {
        if ( t instanceof ParameterizedType ) {
          final ParameterizedType pt = ( ParameterizedType ) t;
          for ( final Type ptType : pt.getActualTypeArguments( ) ) {
            if ( ptType instanceof Class ) {
              ret.add( ( Class ) ptType );
            }
          }
        }
        if ( t instanceof Class ) {
          ret.addAll( processTypeForGenerics( ( ( Class ) t ).getGenericSuperclass( ) ) );
          ret.addAll( processTypeForGenerics( ( ( Class ) t ).getGenericInterfaces( ) ) );
        }
      }
      return ret;
    }

  }

  /**
   * Get a list of the classes corresponding to the actual generic parameters for the given
   * {@code Object o}.
   *
   * @param o
   * @return
   */
  public static List<Class> genericsToClasses( final Object o ) {
    return genericsToClasses( ).apply( o );
  }

  /**
   * Function for getting a list of the classes corresponding to the actual generic parameters for
   * the given {@code Object o}.
   *
   * @param o
   * @return
   */
  private static Function<Object, List<Class>> genericsToClasses( ) {
    return GenericsBreadthFirstTransitiveClosure.INSTANCE;
  }

}
