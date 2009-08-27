package edu.ucsb.eucalyptus.admin.client.extensions.store;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;


class ImageSectionWidget extends Composite {

    private final String SEPARATOR_URI = GraphicsUtil.uri("image-separator.png");

    private VerticalPanel verticalPanel = new VerticalPanel();
    private VerticalPanel imagesPanel = new VerticalPanel();
    private boolean firstImageWidget = true;

    public ImageSectionWidget(ImageSection imageSection) {
        if (imageSection.getTitle() != null) {
            Label titleLabel = new Label(imageSection.getTitle());
            titleLabel.setStyleName("istore-section-title");
            verticalPanel.add(titleLabel);
        }
        if (imageSection.getSummary() != null) {
            Label summaryLabel = new Label(imageSection.getSummary());
            summaryLabel.setStyleName("istore-section-summary");
            verticalPanel.add(summaryLabel);
        }
        initWidget(verticalPanel);
        setStyleName("istore-image-section-widget");
        imagesPanel.setStyleName("istore-images-panel");
    }

    public void addImageWidget(ImageWidget imageWidget) {
        if (firstImageWidget) {
            firstImageWidget = false;
            verticalPanel.add(new FrameWidget(imagesPanel));
        } else {
            Image separator = new Image(SEPARATOR_URI);
            SimplePanel separatorPanel = new SimplePanel();
            separatorPanel.setWidget(separator);
            imagesPanel.add(separatorPanel);
            separatorPanel.setStyleName("istore-image-separator");
        }
        imagesPanel.add(imageWidget);
    }

}
