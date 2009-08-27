package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;


public class FrameWidget extends Composite {

    private final String FRAME_TOP_URI = GraphicsUtil.uri("frame-top.png");
    private final String FRAME_LEFT_URI = GraphicsUtil.uri("frame-left.png");
    private final String FRAME_BOTTOM_URI = GraphicsUtil.uri("frame-bottom.png");

    public FrameWidget(Widget content) {
        FlexTable table = new FlexTable();
        table.setWidget(0, 0, new Image(FRAME_TOP_URI));
        table.setWidget(1, 0, new Image(FRAME_LEFT_URI));
        table.setWidget(2, 0, new Image(FRAME_BOTTOM_URI));
        table.setWidget(1, 1, content);
        table.setStyleName("istore-frame-widget");
        table.setCellSpacing(0);
        table.setCellPadding(0);
        FlexTable.FlexCellFormatter formatter = table.getFlexCellFormatter();
        formatter.setColSpan(0, 0, 2);
        formatter.setColSpan(2, 0, 2);
        formatter.setStyleName(0, 0, "istore-top-frame-image");
        formatter.setStyleName(1, 0, "istore-left-frame-image");
        formatter.setStyleName(2, 0, "istore-bottom-frame-image");
        formatter.getElement(1, 0).getStyle().setProperty("background", "url(" + FRAME_LEFT_URI + ") repeat-y");
        initWidget(table);
    }

}
