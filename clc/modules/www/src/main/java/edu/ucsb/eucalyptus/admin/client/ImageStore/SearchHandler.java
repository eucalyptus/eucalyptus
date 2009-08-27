package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.event.shared.EventHandler;


public interface SearchHandler<T> extends EventHandler {
    void onSearch(SearchEvent<T> event);
}
