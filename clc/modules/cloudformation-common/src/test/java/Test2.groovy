import com.eucalyptus.scripting.Groovyness
import com.eucalyptus.util.Strings
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.introspect.AnnotatedField
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector

import java.beans.Introspector
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 *
 */
class Test {

  @org.junit.Test
  void foo() {
    println new ObjectMapper(  ).setAnnotationIntrospector(
        new JacksonAnnotationIntrospector( ) {
          private static final long serialVersionUID = 1L;
          @Override
          public boolean hasIgnoreMarker( final AnnotatedMember m ) {
            return isMethodBackedByTransientField(m) || super.hasIgnoreMarker( m );
          }
        }
    ).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
     .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
     .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL)
     .writeValueAsString( new Doop( foo: 'baz', bar: 'bazzer' ) )
  }

  private static boolean isMethodBackedByTransientField( final AnnotatedMember m ) {
    boolean isMethodBackedByTransientField = false;
    if (m instanceof AnnotatedMethod) {
      final String fieldName = Introspector.decapitalize(Strings.trimPrefix("get", m.getName()));
      for (final Field field : m.getMember().getDeclaringClass().getDeclaredFields()) {
        if (fieldName.equals(field.getName())) {
          isMethodBackedByTransientField = Modifier.isTransient(field.getModifiers());
          break;
        }
      }
    }
    return isMethodBackedByTransientField;
  }


  static final class Doop {
    private transient String foo
    private String bar

    String getFoo() {
      return foo
    }

    void setFoo(final String foo) {
      this.foo = foo
    }

    String getBar() {
      return bar
    }

    void setBar(final String bar) {
      this.bar = bar
    }
  }
}
