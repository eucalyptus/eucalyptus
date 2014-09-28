package com.eucalyptus.cloudformation.workflow.create;

/**
 * Created by ethomas on 9/28/14.
 */
public class ResourceFailureException extends Exception {
  public ResourceFailureException() {
  }

  public ResourceFailureException(String message) {
    super(message);
  }
}
