package com.eucalyptus.auth.euare;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import com.eucalyptus.util.EucalyptusCloudException;

public class EuareException extends EucalyptusCloudException {

  private static final long serialVersionUID = 1L;

  public static final String ENTITY_ALREADY_EXISTS = "EntityAlreadyExists";
  public static final String LIMIT_EXCEEDED = "LimitExceeded";
  public static final String NO_SUCH_ENTITY = "NoSuchEntity";
  public static final String INTERNAL_FAILURE = "InternalFailure";
  public static final String NOT_AUTHORIZED = "NotAuthorized";
  public static final String DELETE_CONFLICT = "DeleteConflict";
  public static final String NOT_IMPLEMENTED = "NotImplemented";
  public static final String MALFORMED_POLICY_DOCUMENT = "MalformedPolicyDocument";
  public static final String DUPLICATE_CERTIFICATE = "DuplicateCertificate";
  public static final String INVALID_CERTIFICATE = "InvalidCertificate";
  public static final String MALFORMED_CERTIFICATE = "MalformedCertificate";
  public static final String INVALID_NAME = "InvalidName";
  public static final String INVALID_ID = "InvalidId";
  public static final String INVALID_PATH = "InvalidPath";
  public static final String INVALID_VALUE = "InvalidValue";
  
  private HttpResponseStatus status;
  private String error;
  
  public EuareException( HttpResponseStatus status, String error ) {
    super( );
    this.status = status;
    this.error = error;
  }
  
  public EuareException( HttpResponseStatus status, String error, String message, Throwable cause ) {
    super( message, cause );
    this.status = status;
    this.error = error;
  }
  
  public EuareException( HttpResponseStatus status, String error, String message ) {
    super( message );
    this.status = status;
    this.error = error;
  }
  
  public EuareException( HttpResponseStatus status, String error, Throwable cause ) {
    super( cause );
    this.status = status;
    this.error = error;
  }
  
  public HttpResponseStatus getStatus( ) {
    return this.status;
  }
  
  public String getError( ) {
    return this.error;
  }
  
}
