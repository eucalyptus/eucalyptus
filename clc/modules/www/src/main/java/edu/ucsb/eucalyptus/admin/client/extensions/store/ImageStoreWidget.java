/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
package edu.ucsb.eucalyptus.admin.client.extensions.store;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.Timer;

import com.google.gwt.user.client.rpc.AsyncCallback;  

import com.google.gwt.core.client.GWT;


public class ImageStoreWidget extends Composite {

    private final ImageStoreClient client;

    private SimplePanel imageSectionsPanelContainer = new SimplePanel();
    private VerticalPanel imageSectionsPanel = new VerticalPanel();
    private StatusWidget statusWidget = new StatusWidget();
    private SearchWidget searchWidget = new SearchWidget();
    private Label errorLabel = new Label();
    private boolean errorOnUserAction = false;

    private ResponseHandler responseHandler = new ResponseHandler();
    private ResponseHandler searchResponseHandler = new SearchResponseHandler();
    private ResponseHandler userRequestResponseHandler = new UserRequestResponseHandler();
    private RunActionInstallHandler runActionInstallHandler = new RunActionInstallHandler();
    private RunActionCancelHandler runActionCancelHandler = new RunActionCancelHandler();
    private RunActionClearErrorHandler runActionClearErrorHandler = new RunActionClearErrorHandler();

    private ReloadTimer reloadTimer = new ReloadTimer();

    private static class ImageData {
        ImageInfo info = null;
        ImageState state = null;
        List<ImageWidget> widgets = null;

        ImageWidget createImageWidget() {
            ImageWidget imageWidget = null;
            if (info != null && state != null) {
                if (widgets == null) {
                    widgets = new ArrayList<ImageWidget>();
                }
                imageWidget = new ImageWidget(info, state);
                widgets.add(imageWidget);
            }
            return imageWidget;
        }
    }

    private Map<String,ImageData> imageMap = new HashMap<String,ImageData>();

    public ImageStoreWidget(ImageStoreClient client) {
        this.client = client;

        final VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.add(searchWidget);
        verticalPanel.add(errorLabel);
        verticalPanel.add(statusWidget);
        verticalPanel.add(imageSectionsPanelContainer);
        imageSectionsPanelContainer.setWidget(imageSectionsPanel);
        imageSectionsPanelContainer.setStyleName("istore-sections-panel");
        initWidget(verticalPanel);
        setStyleName("istore-image-store-widget");
        errorLabel.setStyleName("istore-global-error-message");

        reloadTimer.scheduleRepeating(5000);

        client.getDashboard(responseHandler);

        searchWidget.addSearchHandler(new SearchHandler<SearchWidget>() {
            public void onSearch(SearchEvent<SearchWidget> searchEvent) {
                String searchText = searchEvent.getSearchText();
                // The searchResponseHandler will remove the "in progress"
                // status which is set automatically by the widget itself.
                if (searchText.length() == 0) {
                    ImageStoreWidget.this.client.getDashboard(searchResponseHandler);
                } else {
                    ImageStoreWidget.this.client.search(searchText, searchResponseHandler);
                }
            }
        });

        statusWidget.addClearErrorHandler(runActionClearErrorHandler);
    }

    private void updateDisplay(ImageStoreResponse response,
                               boolean updateRequestedByUser) {
        /* The following logic ensures that errors returned as a result
         * of a user action won't be overriden by an error generated by
         * an automatic update.  Also, automatic updates which succeed
         * won't erase an error which was caused by a user action, so
         * an error which resulted from a user action will only be
         * cleared by another successful user action, or replaced by
         * another user action error. */
        String errorMessage = response.getErrorMessage();
        if (!errorOnUserAction || updateRequestedByUser) {
            if (errorMessage != null) {
                errorOnUserAction = updateRequestedByUser;
                if (errorMessage.equalsIgnoreCase("Proxy error: Connection refused")) {
                    errorMessage = "Error: failed to connect to local store proxy.  Is it installed?";
                }
                errorLabel.setText(errorMessage);
                errorLabel.setVisible(true);
            } else {
                errorOnUserAction = false;
                errorLabel.setVisible(false);
            }
        }
        if (response.hasImageSections()) {
            // If we got a full new payload, reset the in progress listing,
            // otherwise just update it.
            statusWidget.clear();
        }
        for (ImageInfo imageInfo : response.getImageInfos()) {
            putImageInfo(imageInfo);
        }
        for (ImageState imageState : response.getImageStates()) {
            putImageState(imageState);
        }
        if (response.hasImageSections()) {
            imageSectionsPanel = new VerticalPanel();
            for (ImageSection imageSection : response.getImageSections()) {
                addImageSection(imageSection);
            }
            imageSectionsPanelContainer.setWidget(imageSectionsPanel);
        }
    }

    private void putImageInfo(ImageInfo imageInfo) {
        statusWidget.putImageInfo(imageInfo);
        ImageData imageData = imageMap.get(imageInfo.getUri());
        if (imageData == null) {
            imageData = new ImageData();
            imageMap.put(imageInfo.getUri(), imageData);
        }
        imageData.info = imageInfo;
    }

    private void putImageState(ImageState imageState) {
        statusWidget.putImageState(imageState);
        ImageData imageData = imageMap.get(imageState.getImageUri());
        if (imageData == null) {
            imageData = new ImageData();
            imageData.state = imageState;
            imageMap.put(imageState.getImageUri(), imageData);
        } else {
            imageData.state = imageState;
            if (imageData.widgets != null) {
                for (ImageWidget imageWidget : imageData.widgets) {
                    imageWidget.setImageState(imageState);
                }
            }
        }
    }

    private ImageWidget createImageWidget(String imageUri) {
        ImageData imageData = imageMap.get(imageUri);
        if (imageData != null) {
            ImageWidget imageWidget = imageData.createImageWidget();
            if(imageWidget != null) {
              imageWidget.addInstallHandler(runActionInstallHandler);
              imageWidget.addCancelHandler(runActionCancelHandler);
              imageWidget.addClearErrorHandler(runActionClearErrorHandler);
              return imageWidget;
            }
        }
        return null;
    }

    private void addImageSection(ImageSection imageSection) {
        ImageSectionWidget imageSectionWidget = new ImageSectionWidget(imageSection);
        for (String uri : imageSection.getImageUris()) {
            ImageWidget imageWidget = createImageWidget(uri);
            if (imageWidget != null) {
                imageSectionWidget.addImageWidget(imageWidget);
            }
        }
        imageSectionsPanel.add(imageSectionWidget);
    }

    private void reloadStates() {
        List<ImageInfo> imageInfos = new ArrayList<ImageInfo>();
        for (ImageData imageData : imageMap.values()) {
            if (imageData.info != null) {
                imageInfos.add(imageData.info);
            }
        }
        // Must ask even if there are no infos currently known,
        // since it is possible that the proxy will return other
        // details about other on going changes.
        client.getImageStates(imageInfos, responseHandler);
    }

    private class ReloadTimer extends Timer {
        public void run() {
            if (isVisible()) {
                reloadStates();
            }
        }
    }

    private class ResponseHandler implements AsyncCallback<ImageStoreResponse> {
        protected boolean wasRequestedByUser = false;

        public void responseReceived() {}

        public void onSuccess(ImageStoreResponse response) {
            responseReceived();
            updateDisplay(response, wasRequestedByUser);
        }

        public void onFailure(Throwable caught) {
            responseReceived();
            updateDisplay(JSONImageStoreResponse.fromString("{}"),
                          wasRequestedByUser);
            errorLabel.setText("Error: " + caught.getMessage());
            errorLabel.setVisible(true);
            errorOnUserAction = errorOnUserAction || wasRequestedByUser;
        }

    }

    private class UserRequestResponseHandler extends ResponseHandler { 
        { wasRequestedByUser = true; }
    }

    private class SearchResponseHandler extends UserRequestResponseHandler { 
        public void responseReceived() {
            searchWidget.setInProgress(false);
        }
    }

    private class ActionResponseHandler extends UserRequestResponseHandler { 
        private ImageWidget imageWidget;
        public ActionResponseHandler(ImageWidget imageWidget) {
            this.imageWidget = imageWidget;
        }
        public void responseReceived() {
            super.responseReceived();
            imageWidget.setInProgress(false);
        }
    }

    private class RunActionInstallHandler implements InstallHandler<ImageWidget> {
        public void onInstall(InstallEvent<ImageWidget> event) {
            ImageWidget targetWidget = event.getTarget();
            ImageState imageState = targetWidget.getImageState();
            client.runAction(imageState, ImageState.Action.INSTALL,
                             new ActionResponseHandler(targetWidget));
        }
    }

    private class RunActionCancelHandler implements CancelHandler<ImageWidget> {
        public void onCancel(CancelEvent<ImageWidget> event) {
            ImageWidget targetWidget = event.getTarget();
            ImageState imageState = targetWidget.getImageState();
            client.runAction(imageState, ImageState.Action.CANCEL,
                             new ActionResponseHandler(targetWidget));
        }
    }

    private class RunActionClearErrorHandler implements ClearErrorHandler<ImageState> {
        public void onClearError(ClearErrorEvent<ImageState> event) {
            client.runAction(event.getTarget(), ImageState.Action.CLEAR_ERROR,
                             userRequestResponseHandler);
        }
    }

}
