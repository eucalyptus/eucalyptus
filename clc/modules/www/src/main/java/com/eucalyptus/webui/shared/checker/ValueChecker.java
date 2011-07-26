package com.eucalyptus.webui.shared.checker;

public interface ValueChecker {

  public static final String WEAK = "weak";
  public static final String MEDIUM = "medium";
  public static final String STRONG = "strong";
  public static final String STRONGER = "stronger";
  
  /**
   * Check a value's validity.
   * 
   * @param value
   * @return the original value
   * @throws InvalidValueException
   */
  String check( String value ) throws InvalidValueException;
  
}
