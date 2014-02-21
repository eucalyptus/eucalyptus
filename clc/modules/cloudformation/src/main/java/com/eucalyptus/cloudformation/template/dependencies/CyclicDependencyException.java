package com.eucalyptus.cloudformation.template.dependencies;

/**
 * Created by ethomas on 1/27/14.
 */
public class CyclicDependencyException extends Exception {
  public CyclicDependencyException() {
  }

  public CyclicDependencyException(String message) {
    super(message);
  }

  public CyclicDependencyException(String message, Throwable cause) {
    super(message, cause);
  }

  public CyclicDependencyException(Throwable cause) {
    super(cause);
  }

  public CyclicDependencyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
