package com.eucalyptus.system;

import java.lang.annotation.Annotation;

public class Ats {
  private final Class c;
  private Annotation a;
  public Ats( Class c ) {
    this.c = c;
  }
  
  public <A extends Annotation> boolean has( Class<A> annotation ) {
    return c.isAnnotationPresent( annotation );
  }
  
  public <A extends Annotation> A get( Class<A> annotation ) {
    return ( A ) (a=c.getAnnotation( annotation ));
  }
  
  public static Ats From( Object o ) {
    return from( o );
  }
  public static Ats from( Object o ) {
    return o instanceof Class ? new Ats( ( Class ) o ) : new Ats( o.getClass( ) );
  }
}
