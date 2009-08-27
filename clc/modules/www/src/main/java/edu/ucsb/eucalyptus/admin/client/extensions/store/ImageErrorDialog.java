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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package edu.ucsb.eucalyptus.admin.client.extensions.store;

import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.HasClickHandlers;


class ImageErrorDialog extends DialogBox {

    public ImageErrorDialog(ImageInfo imageInfo, final ImageState imageState) {
        Button closeButton = new Button("Close");
        Button clearErrorButton = new Button("Clear Error");

        closeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                ImageErrorDialog.this.hide();
            }
        });

        clearErrorButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                ImageErrorDialog.this.hide();
                ImageErrorDialog.this.fireEvent(new ClearErrorEvent<ImageState>(imageState));
            }
        });

        TextArea textArea = new TextArea();
        textArea.setValue(imageState.getErrorMessage());

        FlowPanel buttonPanel = new FlowPanel();
        buttonPanel.add(closeButton);
        buttonPanel.add(clearErrorButton);

        clearErrorButton.setVisible(imageState.hasAction(ImageState.Action.CLEAR_ERROR));

        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.add(textArea);
        verticalPanel.add(buttonPanel);

        setText("Error from " + imageInfo.getTitle());
        setWidget(verticalPanel);

        // We use add here to avoid losing the default GWT theme rendering.
        addStyleName("istore-image-error-dialog");
        buttonPanel.setStyleName("istore-button-panel");
    }

    public <T extends Widget & HasClickHandlers> void connectClickHandler(final T widget) {
        widget.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                event.preventDefault();
                ImageErrorDialog.this.showRelativeTo(widget);
            }
        });
    }

    public void addClearErrorHandler(ClearErrorHandler<ImageState> handler) {
        addHandler(handler, ClearErrorEvent.getType());
    }
}
