package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.event.shared.EventHandler;


public interface CancelHandler<T> extends EventHandler {
    void onCancel(CancelEvent<T> event);
}
