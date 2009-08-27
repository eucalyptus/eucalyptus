package edu.ucsb.eucalyptus.admin.client.extensions.store;

import com.google.gwt.event.shared.GwtEvent;


public class CancelEvent<T> extends GwtEvent<CancelHandler<T>> {
    private static final Type<CancelHandler<?>> TYPE = new Type<CancelHandler<?>>();
    private T target;

    CancelEvent(T target) {
        this.target = target;
    }

    public T getTarget() {
        return target;
    }

    public static Type<CancelHandler<?>> getType() {
        return (Type) TYPE;
    }

    @Override
    public Type<CancelHandler<T>> getAssociatedType() {
        return (Type) TYPE;
    }

    @Override
    protected void dispatch(CancelHandler<T> handler) {
        handler.onCancel(this);
    }
}
