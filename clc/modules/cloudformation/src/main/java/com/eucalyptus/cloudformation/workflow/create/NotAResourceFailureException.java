package com.eucalyptus.cloudformation.workflow.create;

/**
 * Created by ethomas on 9/28/14.
 */
public class NotAResourceFailureException extends Exception {
  public NotAResourceFailureException() {
  }

  public NotAResourceFailureException(String message) {
    super(message);
  }
}
