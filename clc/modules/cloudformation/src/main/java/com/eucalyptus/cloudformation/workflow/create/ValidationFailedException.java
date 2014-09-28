package com.eucalyptus.cloudformation.workflow.create;

/**
 * Created by ethomas on 9/28/14.
 */
public class ValidationFailedException extends NotAResourceFailureException {
  public ValidationFailedException() {
  }

  public ValidationFailedException(String message) {
    super(message);
  }
}
