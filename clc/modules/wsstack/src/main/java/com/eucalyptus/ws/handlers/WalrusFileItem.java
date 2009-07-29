package com.eucalyptus.ws.handlers;

import org.apache.commons.fileupload.disk.DiskFileItem;

import java.io.File;

public class WalrusFileItem extends DiskFileItem {
    public WalrusFileItem(String fieldName, String contentType,
                          boolean isFormField, String fileName, int sizeThreshold,
                          File repository) {
        super(fieldName, contentType, isFormField, fileName, sizeThreshold,
                repository);
    }
}
