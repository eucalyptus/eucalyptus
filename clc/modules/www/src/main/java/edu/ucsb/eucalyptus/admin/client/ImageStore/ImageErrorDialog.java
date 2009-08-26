package edu.ucsb.eucalyptus.admin.client.ImageStore;

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
