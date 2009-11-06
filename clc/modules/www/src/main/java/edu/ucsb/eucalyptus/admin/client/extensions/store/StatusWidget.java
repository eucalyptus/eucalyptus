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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.Grid;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;


public class StatusWidget extends Composite {

    private DisclosurePanel disclosurePanel = new DisclosurePanel();
    private Label headerLabel = new Label();
    private Grid contentGrid = new Grid(1, 3); 

    private static class ImageData {
        public int rowIndex = -1;
        ImageInfo info = null;
        ImageState state = null;
    }

    private static class StatusCounts {
        public int downloading = 0;
        public int installing = 0;
        public int errorMessages = 0;

        public boolean allZeros() {
            return downloading == 0 && installing == 0 && errorMessages == 0;
        }
    }

    private Map<String,ImageData> imageDataMap = new HashMap<String,ImageData>();

    public void clear() {
        imageDataMap.clear();
        contentGrid.resize(1, 3);
    }

    public StatusWidget() {
        disclosurePanel.setHeader(headerLabel);
        disclosurePanel.setContent(new FrameWidget(contentGrid));

        contentGrid.setCellSpacing(0);
        contentGrid.setCellPadding(0);

        Label titleLabel = new Label("Image title");
        Label statusLabel = new Label("Status");

        titleLabel.addStyleName("istore-table-header");
        statusLabel.addStyleName("istore-table-header");

        contentGrid.setWidget(0, 0, titleLabel);
        contentGrid.setWidget(0, 1, statusLabel);

        contentGrid.getRowFormatter().addStyleName(0, "istore-odd");

        initWidget(disclosurePanel);

        setStyleName("istore-status-widget");

        headerLabel.setStyleName("istore-header-label");
        contentGrid.setStyleName("istore-table");
    }

    public void putImageInfo(ImageInfo imageInfo) {
        ImageData imageData = imageDataMap.get(imageInfo.getUri());
        if (imageData != null) {
            imageData.info = imageInfo;
            updateImageDisplay(imageData);
        } else {
            imageData = new ImageData();
            imageData.info = imageInfo;
            imageDataMap.put(imageInfo.getUri(), imageData);
        }
    }

    public void putImageState(ImageState imageState) {
        ImageData imageData = imageDataMap.get(imageState.getImageUri());
        if (imageData != null) {
            imageData.state = imageState;
            updateImageDisplay(imageData);
        } else {
            imageData = new ImageData();
            imageData.state = imageState;
            imageDataMap.put(imageState.getImageUri(), imageData);
        }
    }

    private void updateImageDisplay(ImageData imageData) {
        // Only insert or update this image's status if we have the needed
        // data.  Also, if we haven't yet displayed this image, only display
        // if its current status is transient or if it's an error.
        if (imageData.info != null && imageData.state != null &&
            (imageData.rowIndex != -1 ||
             imageData.state.getStatus().isTransient() ||
             imageData.state.getErrorMessage() != null)) {
            updateGrid(imageData);
            if (isAttached()) {
                updateDisplay();
            }
        }
    }

    private void updateDisplay() {
        StatusCounts statusCounts = getStatusCounts();
        if (statusCounts.allZeros()) {
            // Do not use setVisible() here to prevent scrolling the page
            // when the status changes.
            headerLabel.setText("");
            disclosurePanel.setOpen(false);
        } else {
            updateHeader(statusCounts);
        }
    }

    protected void onAttach() {
        updateDisplay();
        super.onAttach();
    }

    private StatusCounts getStatusCounts() {
        StatusCounts statusCounts = new StatusCounts();

        for (ImageData imageData : imageDataMap.values()) {
            if (imageData.info != null && imageData.state != null) {
                switch (imageData.state.getStatus()) {
                    case INSTALLING:
                        statusCounts.installing++;
                        break;
                    case DOWNLOADING:
                        statusCounts.downloading++;
                        break;
                }

                if (imageData.state.getErrorMessage() != null) {
                    statusCounts.errorMessages++;
                }
            }
        }

        return statusCounts;
    }

    private void updateGrid(ImageData imageData) {
        final ImageInfo imageInfo = imageData.info;
        final ImageState imageState = imageData.state;

        assert imageInfo != null && imageState != null;

        int rowIndex = imageData.rowIndex;

        if (rowIndex == -1) {
            // Image isn't yet being displayed.  Add a new row for it.
            imageData.rowIndex = rowIndex = contentGrid.getRowCount();
            contentGrid.resizeRows(rowIndex + 1);
            contentGrid.getRowFormatter().addStyleName(rowIndex,
                    rowIndex % 2 == 0 ?
                    "istore-odd" : "istore-even");
        }

        ImageState.Status status = imageState.getStatus();
        String statusRepr = status.toString().toLowerCase();
        if (status.isTransient()) {
            statusRepr += "...";
        }
        Label titleLabel = new Label(imageInfo.getTitle());
        Label statusLabel = new Label(statusRepr);

        titleLabel.addStyleName("istore-image-title");
        statusLabel.addStyleName("istore-image-status");

        contentGrid.setWidget(rowIndex, 0, titleLabel);
        contentGrid.setWidget(rowIndex, 1, statusLabel);

        if (imageState.getErrorMessage() == null) {
            contentGrid.clearCell(rowIndex, 2);
        } else {
            final Anchor errorAnchor = new Anchor("(show error)");
            ImageErrorDialog errorDialog = new ImageErrorDialog(imageInfo, imageState);
            errorDialog.connectClickHandler(errorAnchor);
            errorDialog.addClearErrorHandler(new ClearErrorHandler() {
                public void onClearError(ClearErrorEvent event) {
                    // Forward the event to subscribers of this widget.
                    StatusWidget.this.fireEvent(event);
                }
            });
            errorAnchor.setStyleName("istore-show-error-anchor");
            contentGrid.setWidget(rowIndex, 2, errorAnchor);
        }
    }

    private void updateHeader(StatusCounts statusCounts) {

        StringBuilder builder = new StringBuilder();

        if (statusCounts.downloading != 0) {
            builder.append(statusCounts.downloading);
            if (statusCounts.downloading == 1) {
                builder.append(" image downloading");
            } else {
                builder.append(" images downloading");
            }
        }

        if (statusCounts.installing != 0) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(statusCounts.installing);
            if (statusCounts.installing == 1) {
                builder.append(" image installing");
            } else {
                builder.append(" images installing");
            }
        }

        if (statusCounts.errorMessages != 0) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append(statusCounts.errorMessages);
            if (statusCounts.errorMessages == 1) {
                builder.append(" error message");
            } else {
                builder.append(" error messages");
            }
        }

        if (builder.length() == 0) {
            builder.append("No requested changes in progress");
        }

        headerLabel.setText(builder.toString());
    }

    public void addClearErrorHandler(ClearErrorHandler<ImageState> handler) {
        addHandler(handler, ClearErrorEvent.getType());
    }

}
