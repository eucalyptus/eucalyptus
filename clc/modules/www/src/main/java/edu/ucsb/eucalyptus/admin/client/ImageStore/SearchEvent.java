package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.event.shared.GwtEvent;


public class SearchEvent<T> extends GwtEvent<SearchHandler<T>> {
    private static final Type<SearchHandler<?>> TYPE = new Type<SearchHandler<?>>();
    private T target;
    private String searchText;

    SearchEvent(T target, String searchText) {
        this.searchText = searchText;
    }

    public T getTarget() {
        return target;
    }

    public String getSearchText() {
        return searchText;
    }

    public static Type<SearchHandler<?>> getType() {
        return (Type) TYPE;
    }

    @Override
    public Type<SearchHandler<T>> getAssociatedType() {
        return (Type) TYPE;
    }

    @Override
    protected void dispatch(SearchHandler<T> handler) {
        handler.onSearch(this);
    }
}
