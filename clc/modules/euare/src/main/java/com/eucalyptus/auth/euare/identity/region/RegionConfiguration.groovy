/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.euare.identity.region

import com.eucalyptus.util.Cidr
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
import java.util.regex.Pattern

@CompileStatic
@Canonical
class RegionConfiguration implements Iterable<Region> {
  List<Region> regions
  List<String> remoteCidrs
  List<String> forwardedForCidrs

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
  String certificateFingerprintDigest
  String certificateFingerprint
  String sslCertificateFingerprintDigest
  String sslCertificateFingerprint
  List<Integer> identifierPartitions
  List<Service> services
  List<String> remoteCidrs
  List<String> forwardedForCidrs
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
    validateAll( configuration.&getRemoteCidrs, new CidrValidator(errors) )
    validateAll( configuration.&getForwardedForCidrs, new CidrValidator(errors) )
  }
}

@CompileStatic
@Canonical
@PackageScope
class RegionValidator extends TypedValidator<Region> {
  public static final Pattern REGION_NAME_PATTERN = Pattern.compile( '^(?!-)[a-z0-9-]{1,63}(?<!-)' )

  Errors errors

  @Override
  void validate( final Region region ) {
    require( region.&getName )
    require( region.&getCertificateFingerprint )
    require( region.&getIdentifierPartitions )
    require( region.&getServices )
    validate( region.&getName, new RegexValidator( errors, REGION_NAME_PATTERN, 'Invalid region name ([a-z0-9-]*) "{0}": "{1}"' ) )
    validate( region.&getCertificateFingerprint, new CertificateFingerprintValidator( errors ) )
    validate( region.&getCertificateFingerprintDigest, new CertificateFingerprintDigestValidator( errors ) )
    validate( region.&getSslCertificateFingerprint, new CertificateFingerprintValidator( errors ) )
    validate( region.&getSslCertificateFingerprintDigest, new CertificateFingerprintDigestValidator( errors ) )
    validateAll( region.&getIdentifierPartitions, new IdentifierPartitionValidator( errors ) )
    validateAll( region.&getServices, new ServiceValidator( errors ) )
    validateAll( region.&getRemoteCidrs, new CidrValidator( errors ) )
    validateAll( region.&getForwardedForCidrs, new CidrValidator( errors ) )

    if ( region?.identifierPartitions?.empty ) {
      errors.reject( "property.invalid.identifier", [pathTranslate(errors.nestedPath,'identifierPartitions')] as Object[], 'No values given for \"{0}\"' )
    }
    if ( region?.services?.empty ) {
      errors.reject( "property.invalid.services", [pathTranslate(errors.nestedPath,'services')] as Object[], 'No values given for \"{0}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class CidrValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String cidr ) {
    if ( !Cidr.parse( ).apply( cidr ).isPresent( ) ) {
      errors.reject( "property.invalid.cidr", [pathTranslate( errors.getNestedPath( ) ), cidr ] as Object[], 'Invalid CIDR for \"{0}\": \"{1}\"' )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class RegexValidator extends TypedValidator<String> {
  Errors errors
  Pattern pattern
  String errorMessage

  @Override
  void validate( final String value ) {
    if ( value && !pattern.matcher( value ).matches( ) ) {
      errors.reject( "property.invalid.regex", [pathTranslate( errors.getNestedPath( ) ), value ] as Object[], errorMessage )
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class CertificateFingerprintValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String fingerprint ) {
    if ( fingerprint ) {
      if ( !fingerprint.matches( '[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){9,255}' ) ) {
        errors.reject( "property.invalid.certificateFingerprint", [pathTranslate( errors.getNestedPath( ) )] as Object[], 'Invalid certificate fingerprint (e.g. EC:E7:...): \"{0}\"' )
      }
    }
  }
}

@CompileStatic
@Canonical
@PackageScope
class CertificateFingerprintDigestValidator extends TypedValidator<String> {
  Errors errors

  @Override
  void validate( final String digest ) {
    if ( digest ) {
      if ( ![ 'SHA-1', 'SHA-224', 'SHA-256', 'SHA-384', 'SHA-512' ].contains( digest ) ) {
        errors.reject( "property.invalid.certificateFingerprintDigest", [digest, pathTranslate( errors.getNestedPath( ) ), "SHA-256, SHA-1"] as Object[], 'Invalid certificate fingerprint digest: \"{0}\" for field: \"{1}\". Typical digests: \"{2}\"' )
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
  public static final Pattern SERVICE_TYPE_PATTERN = Pattern.compile( '[a-z][0-9a-z-]{0,62}' );
  Errors errors

  @Override
  void validate( final Service service ) {
    require( service.&getType )
    require( service.&getEndpoints )
    validate( service.&getType, new RegexValidator( errors, SERVICE_TYPE_PATTERN, 'Invalid service type "{0}": "{1}"' ) )
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
      URI endpointUri = new URI( endpoint )
      if ( !endpointUri.absolute ||
          ( !endpointUri.scheme.equalsIgnoreCase( 'https' ) && !endpointUri.scheme.equalsIgnoreCase( 'http' ) ) ) {
        errors.reject( "property.invalid.endpoint", [pathTranslate(errors.nestedPath), endpoint] as Object[], 'Invalid service endpoint (e.g. https://...)\"{0}\": \"{1}\"' )
      }
    } catch ( e ) {
      errors.reject( "property.invalid.endpoint", [pathTranslate(errors.nestedPath), endpoint] as Object[], 'Invalid service endpoint (e.g. https://...)\"{0}\": \"{1}\"' )
    }
  }
}
