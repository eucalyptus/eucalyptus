package edu.ucsb.eucalyptus.admin.client.extensions.store;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.core.client.GWT;


/** Graphic progress bar widget.  This widget works by gradually
 * overlaying an image which has the progress meter fully completed
 * on top of a base image which has the meter completely empty.
 */
public class ProgressBarWidget extends Composite {

    private final Image doneOverlayImage;
    private final int width;
    private final int height;

    ProgressBarWidget(String baseImageUrl, String doneImageUrl, int width, int height) {
        /* We use two panels here.  The external one is relative, while
         * the internal one is absolute, so that we can overlap the two
         * images inside it.  Having the external panel makes paddings
         * and whatnot work as expected. */
        SimplePanel externalPanel = new SimplePanel();
        AbsolutePanel internalPanel = new AbsolutePanel();

        Image baseImage = new Image(baseImageUrl);

        this.width = width;
        this.height = height;

        doneOverlayImage = new Image(doneImageUrl, 0, 0, 0, height);

        internalPanel.setPixelSize(width, height);
        internalPanel.add(baseImage, 0, 0);
        internalPanel.add(doneOverlayImage, 0, 0);

        externalPanel.setWidget(internalPanel);
        externalPanel.setStyleName("istore-progress-bar-widget");

        initWidget(externalPanel);
    }

    public void setPercentage(int percentage) {
        int visibleWidth = (percentage * width) / 100;
        doneOverlayImage.setVisibleRect(0, 0, visibleWidth, height);
    }

}
