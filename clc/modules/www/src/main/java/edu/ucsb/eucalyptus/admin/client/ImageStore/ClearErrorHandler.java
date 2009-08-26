package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.event.shared.EventHandler;


public interface ClearErrorHandler<T> extends EventHandler {
    void onClearError(ClearErrorEvent<T> event);
}
