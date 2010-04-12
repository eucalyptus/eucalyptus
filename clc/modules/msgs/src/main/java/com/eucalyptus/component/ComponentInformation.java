package com.eucalyptus.component;

import java.io.Serializable;

/**
 * Marker interface for all classes which contain component information.
 * @author decker
 */
public interface ComponentInformation extends Serializable {
  public String getName( );
}
