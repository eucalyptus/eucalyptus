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
package com.eucalyptus.auth.euare.identity.region

import com.eucalyptus.crypto.util.PEMFiles
import com.google.common.base.CaseFormat
import com.google.common.base.Strings
import groovy.transform.Canonical
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.codehaus.groovy.runtime.MethodClosure
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils
import org.springframework.validation.Validator

import java.lang.reflect.ParameterizedType
import java.nio.charset.StandardCharsets

@CompileStatic
@Canonical
class RegionConfiguration implements Iterable<Region> {
  List<Region> regions

  @Override
  Iterator<Region> iterator( ) {
    regions == null ?
        Collections.emptyIterator( ) :
        regions.iterator( )
  }
}

@CompileStatic
@Canonical
class Region {
  String name
  String certificate
  List<Integer> identifierPartitions
  List<Service> services
}

@CompileStatic
@Canonical
class Service {
  String type
  List<String> endpoints
}

@CompileStatic
@Canonical
@PackageScope
abstract class TypedValidator<T> implements Validator {

  @Override
  boolean supports( final Class<?> aClass ) {
    aClass == getTargetClass( )
  }

  @Override
  void validate( final Object o, final Errors errors ) {
    validate( (T) o )
  }

  abstract Errors getErrors()

  Class<?> getTargetClass( ) {
    (Class<?>)((ParameterizedType)getClass().getGenericSuperclass()).actualTypeArguments[0]
  }

  void validate( T target ) {
  }

  String toFieldName( String methodName ) {
    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, methodName.substring( 3 ) )
  }

  String toPropertyName( String fieldName ) {
    CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldName )
  }

  String pathTranslate( String path, String name = null ) {
    String pathPrefix  = Strings.isNullOrEmpty( path ) ? '' : "${path}${path.endsWith('.')?'':'.'}"
    String fullPath = name ? "${pathPrefix}${name}" : path
    fullPath.split('\\.').collect{ String item -> toPropertyName(item) }.join('.')
  }

  void require( Closure<?> closure ) {
    MethodClosure methodClosure = (MethodClosure) closure
    String fieldName = toFieldName(methodClosure.method)
    ValidationUtils.rejectIfEmptyOrWhitespace( errors, fieldName, 'property.required', [pathTranslate(errors.nestedPath,fieldName)] as Object[], 'Missing required property \"{0}\"' );
  }

  void validate( Closure<?> closure, Validator validator ) {
    MethodClosure methodClosure = (MethodClosure) closure
    validate( closure.call(), toFieldName( methodClosure.method ), validator );
  }

  void validate( Object target, String path, Validator validator ) {
    try {
      errors.pushNestedPath( path );
      ValidationUtils.invokeValidator( validator, target, errors );
    } finally {
      errors.popNestedPath();
    }
  }

  void validateAll( Closure<List<?>> closure, Validator validator ) {
    MethodClosure methodClosure = (MethodClosure) closure
    String field = toFieldName(methodClosure.method)
    closure.call()?.eachWithIndex{ Object target, Integer index ->
      try {
        errors.pushNestedPath("${field}[${index}]");
        ValidationUtils.invokeValidator( validator, target, errors );
      } finally {
        errors.popNestedPath();
      }
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class RegionConfigurationValidator extends TypedValidator<RegionConfiguration> {
  Errors errors

  @Override
  void validate( final RegionConfiguration configuration ) {
    validateAll( configuration.&getRegions, new RegionValidator(errors) )
  }
}

@CompileStatic
@Canonical
@PackageScope
class RegionValidator extends TypedValidator<Region> {
  Errors errors

  @Override
  void validate( final Region region ) {
    require( region.&getName )
    require( region.&getCertificate )
    require( region.&getIdentifierPartitions )
    require( region.&getServices )
    validate( region.&getCertificate, new CertificateValidator( errors ) )
    validateAll( region.&getIdentifierPartitions, new IdentifierPartitionValidator( errors ) )
    validateAll( region.&getServices, new ServiceValidator( errors ) )
    if ( region.identifierPartitions.empty ) {
      errors.reject( "property.invalid.identifier", [pathTranslate(errors.nestedPath,'identifierPartitions')] as Object[], 'No values given for \"{0}\"' )
    }
    if ( region.services.empty ) {
      errors.reject( "property.invalid.services", [pathTranslate(errors.nestedPath,'services')] as Object[], 'No values given for \"{0}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class CertificateValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String pem ) {
    if ( pem ) {
      try {
        PEMFiles.getCert( pem.getBytes( StandardCharsets.UTF_8 ) )
      } catch ( e ) {
        errors.reject( "property.invalid.certificate", [pathTranslate( errors.getNestedPath( ) )] as Object[], 'Invalid certificate: \"{0}\"' )
      }
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class IdentifierPartitionValidator extends TypedValidator<Integer> {
  Errors errors

  @Override
  void validate( final Integer partition ) {
    boolean valid = false
    try {
      valid = partition > 0 && partition < 1000
    } catch( e ) {
    }
    if ( !valid ) {
      errors.reject( "property.invalid.identifier", [pathTranslate( errors.getNestedPath( ) )] as Object[], 'Invalid identifier partition (1-999) \"{0}\": \"{1}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class ServiceValidator extends TypedValidator<Service> {
  Errors errors

  @Override
  void validate( final Service service ) {
    require( service.&getType )
    require( service.&getEndpoints )
    validateAll( service.&getEndpoints, new EndpointValidator( errors ) )
    if ( service.endpoints.empty ) {
      errors.reject( "property.invalid.endpoints", [pathTranslate(errors.nestedPath,'endpoints')] as Object[], 'No values given for \"{0}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class EndpointValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String endpoint ) {
    try {
      new URI( endpoint )
    } catch ( e ) {
      errors.reject( "property.invalid.endpoint", [pathTranslate(errors.nestedPath)] as Object[], 'Invalid service endpoint \"{0}\": \"{1}\"' )
    }
  }
}
