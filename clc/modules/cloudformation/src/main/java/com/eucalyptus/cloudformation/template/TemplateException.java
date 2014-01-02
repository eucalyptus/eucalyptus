package com.eucalyptus.cloudformation.template;

/**
 * Created by ethomas on 12/10/13.
 */
public class TemplateException extends Exception {
  public TemplateException() {
    super();
  }

  public TemplateException(String message) {
    super(message);
  }

  public TemplateException(String message, Throwable cause) {
    super(message, cause);
  }

  public TemplateException(Throwable cause) {
    super(cause);
  }

}
