package com.eucalyptus.webui.shared.checker;

public interface ValueChecker {

  /**
   * Check a value's validity.
   * 
   * @param value
   * @return the original value
   * @throws InvalidValueException
   */
  String check( String value ) throws InvalidValueException;
  
}
