package edu.ucsb.eucalyptus.admin.client.extensions.store;

import com.google.gwt.event.shared.EventHandler;


public interface InstallHandler<T> extends EventHandler {
    void onInstall(InstallEvent<T> event);
}
