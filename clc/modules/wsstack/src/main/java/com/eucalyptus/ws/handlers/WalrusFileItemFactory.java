package com.eucalyptus.ws.handlers;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

public class WalrusFileItemFactory extends DiskFileItemFactory {
    public WalrusFileItemFactory() {
        super();
    }

    public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
        return new WalrusFileItem(fieldName, contentType,
                isFormField, fileName, getSizeThreshold(), getRepository());

    }
}