package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.event.shared.GwtEvent;


public class ClearErrorEvent<T> extends GwtEvent<ClearErrorHandler<T>> {
    private static final Type<ClearErrorHandler<?>> TYPE = new Type<ClearErrorHandler<?>>();
    private T target;

    ClearErrorEvent(T target) {
        this.target = target;
    }

    public T getTarget() {
        return target;
    }

    public static Type<ClearErrorHandler<?>> getType() {
        return (Type) TYPE;
    }

    @Override
    public Type<ClearErrorHandler<T>> getAssociatedType() {
        return (Type) TYPE;
    }

    @Override
    protected void dispatch(ClearErrorHandler<T> handler) {
        handler.onClearError(this);
    }
}
