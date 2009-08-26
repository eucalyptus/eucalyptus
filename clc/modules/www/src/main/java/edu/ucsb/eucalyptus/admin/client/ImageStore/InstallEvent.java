package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.event.shared.GwtEvent;


public class InstallEvent<T> extends GwtEvent<InstallHandler<T>> {
    private static final Type<InstallHandler<?>> TYPE = new Type<InstallHandler<?>>();
    private T target;

    InstallEvent(T target) {
        this.target = target;
    }

    public T getTarget() {
        return target;
    }

    public static Type<InstallHandler<?>> getType() {
        return (Type) TYPE;
    }

    @Override
    public Type<InstallHandler<T>> getAssociatedType() {
        return (Type) TYPE;
    }

    @Override
    protected void dispatch(InstallHandler<T> handler) {
        handler.onInstall(this);
    }
}
