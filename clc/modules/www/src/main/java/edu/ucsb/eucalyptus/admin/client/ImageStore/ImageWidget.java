package edu.ucsb.eucalyptus.admin.client.ImageStore;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.DisclosurePanel;

import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.CloseEvent;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;

import com.google.gwt.core.client.GWT;


public class ImageWidget extends Composite {

    private static final String DEFAULT_ICON_URI = GraphicsUtil.uri("default-image-icon.png");
    private static final String UPGRADE_ICON_URI = GraphicsUtil.uri("available-upgrade.png");
    private static final String PROGRESS_BASE_URI = GraphicsUtil.uri("progress-base.png");
    private static final String PROGRESS_DONE_URI = GraphicsUtil.uri("progress-done.png");
    private static final String INSTALL_BUTTON_URI = GraphicsUtil.uri("install-button.png");
    private static final String DOWNLOADING_BUTTON_URI = GraphicsUtil.uri("downloading-button.gif");
    private static final String INSTALLING_BUTTON_URI = GraphicsUtil.uri("installing-button.gif");
    private static final String INSTALLED_BUTTON_URI = GraphicsUtil.uri("installed-button.png");
    private static final String SPINNER_URI = GraphicsUtil.uri("spinner.gif");

    /* Left panel. */
    private final Image iconImage = new Image();
    private final SimplePanel providerTitlePanel = new SimplePanel();

    /* Center panel. */
    private final Label titleLabel = new Label();
    private final Label summaryLabel = new Label();
    private final Label readMoreLabel = new Label("read more...");
    private final FlowPanel versionPanel = new FlowPanel();
    private final Label versionLabel = new InlineLabel("Unknown");
    private final FlowPanel sizePanel = new FlowPanel();
    private final Label sizeLabel = new InlineLabel("Unknown");
    private final FlowPanel tagsPanel = new FlowPanel();
    private final Label tagsLabel = new InlineLabel();
    private final HTML descriptionHtml = new HTML();

    /* Right panel. */
    private final FlexTable commandPanel = new FlexTable();
    private final SimplePanel buttonPanel = new SimplePanel();
    private final Image spinnerImage = new Image(SPINNER_URI);
    private final VerticalPanel progressPanel = new VerticalPanel();
    private ProgressBarWidget progressBar;

    private int showErrorRowIndex;
    private int cancelRowIndex;
    private int upgradeIconRowIndex;

    private final ImageInfo imageInfo;
    private ImageState imageState;

    public ImageWidget(ImageInfo imageInfo, ImageState imageState) {
        this.imageInfo = imageInfo;

        HorizontalPanel horizontalPanel = new HorizontalPanel();

        /* Three vertical panels inside a horizontal panel. */
        horizontalPanel.add(buildIconPanel());
        horizontalPanel.add(buildTitlePanel());
        horizontalPanel.add(buildCommandPanel());

        /* This should really be in the CSS somehow, but I haven't yet
         * found a trivial way to set the padding attribute on the TDs
         * of the top table only, and not on contained tables. */
        horizontalPanel.setSpacing(15);

        initWidget(horizontalPanel);

        setStyleName("istore-image-widget");

        setImageTitle(imageInfo.getTitle());
        setSummary(imageInfo.getSummary());
        setIconUri(imageInfo.getIconUri());
        setVersion(imageInfo.getVersion());
        setSizeInMB(imageInfo.getSizeInMB());
        setProvider(imageInfo.getProviderTitle(), imageInfo.getProviderUri());
        setTags(imageInfo.getTags());
        setDescriptionHtml(imageInfo.getDescriptionHtml());

        setImageState(imageState);
    }

    public ImageInfo getImageInfo() {
        return imageInfo;
    }

    public ImageState getImageState() {
        return imageState;
    }

    public void setImageState(ImageState imageState) {
        this.imageState = imageState;

        if (imageState.getStatus() == ImageState.Status.UNKNOWN ||
            (imageState.getStatus() == ImageState.Status.UNINSTALLED &&
             !imageState.hasAction(ImageState.Action.INSTALL))) {
            GWT.log("ERROR: Received unknown image status.", null);
            commandPanel.setVisible(false);
        } else {
            updateButtonImage(imageState.getStatus());

            Integer percentage = imageState.getProgressPercentage();
            if (percentage == null) {
                progressPanel.setVisible(false);
            } else {
                progressBar.setPercentage(percentage);
                progressPanel.setVisible(true);
            }

            setShowErrorVisible(imageState.getErrorMessage() != null);
            setCancelVisible(imageState.hasAction(ImageState.Action.CANCEL));
            setUpgradeIconVisible(imageState.isUpgrade());
        }
    }

    public void addInstallHandler(InstallHandler<ImageWidget> handler) {
        addHandler(handler, InstallEvent.getType());
    }

    public void addCancelHandler(CancelHandler<ImageWidget> handler) {
        addHandler(handler, CancelEvent.getType());
    }

    public void addClearErrorHandler(ClearErrorHandler<ImageState> handler) {
        addHandler(handler, ClearErrorEvent.getType());
    }

    private Widget buildIconPanel() {
        VerticalPanel verticalPanel = new VerticalPanel();
        Label byLabel = new Label("by");

        verticalPanel.add(iconImage);
        verticalPanel.add(byLabel);
        verticalPanel.add(providerTitlePanel);

        byLabel.setStyleName("istore-provider-title-by");

        verticalPanel.setSpacing(3);
        verticalPanel.setStyleName("istore-image-icon-panel");

        return verticalPanel;
    }

    private Widget buildTitlePanel() {

        FlowPanel visibleDetailsPanel = new FlowPanel();
        visibleDetailsPanel.add(versionPanel);

        FlowPanel hiddenDetailsPanel = new FlowPanel();
        hiddenDetailsPanel.add(sizePanel);
        hiddenDetailsPanel.add(tagsPanel);

        InlineLabel versionHeaderLabel = new InlineLabel("Image version:");
        InlineLabel sizeHeaderLabel = new InlineLabel("Image size:");
        InlineLabel tagsHeaderLabel = new InlineLabel("Image tags:");

        versionPanel.add(versionHeaderLabel);
        versionPanel.add(versionLabel);
        sizePanel.add(sizeHeaderLabel);
        sizePanel.add(sizeLabel);
        tagsPanel.add(tagsHeaderLabel);
        tagsPanel.add(tagsLabel);

        VerticalPanel readMorePanel = new VerticalPanel();
        readMorePanel.add(hiddenDetailsPanel);
        readMorePanel.add(descriptionHtml);

        DisclosurePanel readMoreDisclosurePanel = new DisclosurePanel();
        readMoreDisclosurePanel.setHeader(readMoreLabel);
        readMoreDisclosurePanel.setContent(readMorePanel);

        VerticalPanel topPanel = new VerticalPanel();
        topPanel.add(titleLabel);
        topPanel.add(visibleDetailsPanel);
        topPanel.add(summaryLabel);
        topPanel.add(readMoreDisclosurePanel);

        titleLabel.setStyleName("istore-image-title");
        summaryLabel.setStyleName("istore-image-summary");
        topPanel.setStyleName("istore-title-panel");
        readMoreLabel.setStyleName("istore-read-more-label");

        visibleDetailsPanel.setStyleName("istore-visible-image-details-panel");
        hiddenDetailsPanel.setStyleName("istore-hidden-image-details-panel");
        versionHeaderLabel.setStyleName("istore-image-version-label");
        versionHeaderLabel.addStyleName("istore-image-detail-label");
        versionLabel.setStyleName("istore-image-version-value-label");
        versionLabel.addStyleName("istore-image-detail-value-label");
        sizeHeaderLabel.setStyleName("istore-image-size-label");
        sizeHeaderLabel.addStyleName("istore-image-detail-label");
        sizeLabel.setStyleName("istore-image-size-value-label");
        sizeLabel.addStyleName("istore-image-detail-value-label");
        tagsHeaderLabel.setStyleName("istore-image-tags-label");
        tagsHeaderLabel.addStyleName("istore-image-detail-label");
        tagsLabel.setStyleName("istore-image-tags-value-label");
        tagsLabel.addStyleName("istore-image-detail-value-label");

        descriptionHtml.setStyleName("istore-image-description");

        ReadMoreOpenCloseHandler rmoch = this.new ReadMoreOpenCloseHandler();
        readMoreDisclosurePanel.addOpenHandler(rmoch);
        readMoreDisclosurePanel.addCloseHandler(rmoch);

        return topPanel;
    }



    private Widget buildCommandPanel() {
        progressBar = new ProgressBarWidget(PROGRESS_BASE_URI,
                                            PROGRESS_DONE_URI,
                                            120, 12);

        final Anchor cancelAnchor = new Anchor("cancel");
        final Anchor showErrorAnchor = new Anchor("show error");
        
        showErrorAnchor.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                event.preventDefault();
                ImageInfo imageInfo = ImageWidget.this.imageInfo;
                ImageState imageState = ImageWidget.this.imageState;
                ImageErrorDialog dialog = new ImageErrorDialog(imageInfo,
                                                               imageState);
                dialog.showRelativeTo(showErrorAnchor);
                dialog.addClearErrorHandler(new ClearErrorHandler<ImageState>() {
                    public void onClearError(ClearErrorEvent<ImageState> event) {
                        ImageWidget.this.fireEvent(event);
                    }
                });
            }
        });

        Image upgradeIconImage = new Image(UPGRADE_ICON_URI);

        commandPanel.setCellSpacing(0);
        commandPanel.setCellPadding(0);

        spinnerImage.setVisible(false);

        commandPanel.setWidget(0, 0, buttonPanel);
        commandPanel.setWidget(0, 1, spinnerImage);
        commandPanel.setWidget(1, 0, progressPanel);
        showErrorRowIndex = 2;
        cancelRowIndex = 3;
        upgradeIconRowIndex = 4;
        commandPanel.setWidget(showErrorRowIndex, 0, showErrorAnchor);
        commandPanel.setWidget(cancelRowIndex, 0, cancelAnchor);
        commandPanel.setWidget(upgradeIconRowIndex, 0, upgradeIconImage);

        progressPanel.add(progressBar);

        commandPanel.setStyleName("istore-command-panel");
        buttonPanel.setStyleName("istore-button-panel");
        spinnerImage.setStyleName("istore-spinner");
        progressPanel.setStyleName("istore-progress-panel");
        cancelAnchor.setStyleName("istore-cancel-anchor");
        showErrorAnchor.setStyleName("istore-show-error-anchor");
        upgradeIconImage.setStyleName("istore-upgrade-icon");

        commandPanel.getRowFormatter().setStyleName(showErrorRowIndex,
                                                    "istore-show-error-panel");
        commandPanel.getRowFormatter().setStyleName(cancelRowIndex,
                                                    "istore-cancel-panel");
        commandPanel.getRowFormatter().setStyleName(upgradeIconRowIndex,
                                                    "istore-upgrade-icon-panel");

        // Translate the click event into a cancel event.
        cancelAnchor.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                setInProgress(true);
                ImageWidget.this.fireEvent(new CancelEvent<ImageWidget>(ImageWidget.this));
                event.preventDefault();
            }
        });

        return commandPanel;
    }

    private void setShowErrorVisible(boolean isVisible) {
        commandPanel.getRowFormatter().setVisible(showErrorRowIndex, isVisible);
    }

    private void setCancelVisible(boolean isVisible) {
        commandPanel.getRowFormatter().setVisible(cancelRowIndex, isVisible);
    }

    private void setUpgradeIconVisible(boolean isVisible) {
        commandPanel.getRowFormatter().setVisible(upgradeIconRowIndex, isVisible);
    }

    private void setImageTitle(String title) {
        titleLabel.setText(title);
    }

    private void setSummary(String summary) {
        summaryLabel.setText(summary);
    }

    private void setVersion(String version) {
        if (version == null) {
            versionPanel.setVisible(false);
        } else {
            versionLabel.setText(version);
            versionPanel.setVisible(true);
        }
    }

    private void setSizeInMB(Integer sizeInMB) {
        if (sizeInMB == null) {
            sizePanel.setVisible(false);
        } else {
            sizeLabel.setText(sizeInMB.toString() + "MB");
            sizePanel.setVisible(true);
        }
    }

    private void setTags(List<String> tags) {
        if (tags == null || tags.size() == 0) {
            tagsPanel.setVisible(false);
        } else {
            String joinedTags;
            if (tags.size() == 1) {
                joinedTags = tags.get(0);
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(tags.get(0));
                for (int i = 1; i != tags.size(); i++) { 
                    builder.append(", ");
                    builder.append(tags.get(i));
                }
                joinedTags = builder.toString();
            }
            tagsLabel.setText(joinedTags);
            tagsPanel.setVisible(true);
        }
    }

    private void setDescriptionHtml(String description) {
        descriptionHtml.setHTML(description);
    }

    private void setIconUri(String uri) {
        if (uri == null) {
            iconImage.setUrl(DEFAULT_ICON_URI);
        } else {
            iconImage.setUrl(uri);
        }
    }

    private void setProvider(String title, String uri) {
        String styleName = "istore-known-provider-title";

        if (title == null) {
            styleName = "istore-unknown-provider-title";
            title = "Unknown";
        }

        Widget providerTitleWidget;
        if (uri == null) {
            Label providerLabel = new Label();
            providerLabel.setText(title);
            providerTitleWidget = providerLabel;
        } else {
            Anchor providerAnchor = new Anchor();
            providerAnchor.setText(title);
            providerAnchor.setHref(uri);
            providerTitleWidget = providerAnchor;
        }

        providerTitlePanel.setWidget(providerTitleWidget);
        providerTitleWidget.setStyleName(styleName);
        providerTitleWidget.addStyleName("istore-provider-title");
    }

    private Image createButtonImage(ImageState.Status status) {
        String uri;
        switch (status) {
            case UNINSTALLED: uri = INSTALL_BUTTON_URI; break;
            case DOWNLOADING: uri = DOWNLOADING_BUTTON_URI; break;
            case INSTALLING: uri = INSTALLING_BUTTON_URI; break;
            case INSTALLED: uri = INSTALLED_BUTTON_URI; break;
            default:
                return null; // Unsupported status.
        }
        return new Image(uri);
    }

    private void updateButtonImage(ImageState.Status status) {
        final Image buttonImage = createButtonImage(status);

        // If we don't have support for the given status, prevent any
        // commands from happening on this image.
        if (buttonImage == null) {
            GWT.log("ERROR: Couldn't find an appropriate button for the given status.", null);
            commandPanel.setVisible(false);
        } else {
            commandPanel.setVisible(true);
            if (status == ImageState.Status.UNINSTALLED) {
                // Translate the click event into a download event.
                buttonImage.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        setInProgress(true);
                        ImageWidget.this.fireEvent(new InstallEvent<ImageWidget>(ImageWidget.this));
                    }
                });
                buttonImage.setStyleName("istore-install-button");
            }
            buttonPanel.setWidget(buttonImage);
        }
    }

    public void setInProgress(boolean inProgress) {
        spinnerImage.setVisible(inProgress);
        Widget buttonImage = buttonPanel.getWidget();
        if (buttonImage != null &&
            imageState.getStatus() == ImageState.Status.UNINSTALLED) {
            if (inProgress) {
                buttonImage.addStyleName("istore-install-button-progress");
            } else {
                buttonImage.removeStyleName("istore-install-button-progress");
            }
        }
    }

    private class ReadMoreOpenCloseHandler
            implements OpenHandler<DisclosurePanel>,
                       CloseHandler<DisclosurePanel> {

        public void onOpen(OpenEvent<DisclosurePanel> event) {
            readMoreLabel.setText("hide...");
        }

        public void onClose(CloseEvent<DisclosurePanel> event) {
            readMoreLabel.setText("read more...");
        }
    }

}

