package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.FlowPanel;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;

import com.google.gwt.core.client.GWT;


public class SearchWidget extends Composite {

    private final String SEARCH_BUTTON_URI = GraphicsUtil.uri("search-button.png");

    TextBox searchBox = new TextBox();
    Image searchButtonImage = new Image(SEARCH_BUTTON_URI);
    
    public SearchWidget() {
        Label titleLabel = new Label("Search");
        titleLabel.setStyleName("istore-section-title");
        searchBox.setStyleName("istore-search-box");
        searchButtonImage.setStyleName("istore-search-button");

        FlowPanel flowPanel = new FlowPanel();
        flowPanel.add(titleLabel);
        flowPanel.add(searchBox);
        flowPanel.add(searchButtonImage);
        flowPanel.setStyleName("istore-search-panel");
        
        initWidget(flowPanel);

        setInProgress(false);

        // Translate the click event into a search event.
        searchButtonImage.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                setInProgress(true);
                SearchWidget.this.fireEvent(new SearchEvent(SearchWidget.this,
                                                            searchBox.getText()));
            }
        });

        // Translate the enter key into a search event too.
        searchBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == 13) {
                    setInProgress(true);
                    SearchWidget.this.fireEvent(new SearchEvent(SearchWidget.this,
                                                                searchBox.getText()));
                }
            }
        });
    }

    public void setInProgress(boolean inProgress) {
        if (inProgress) {
            searchButtonImage.addStyleName("istore-search-button-progress");
            searchBox.addStyleName("istore-search-box-progress");
        } else {
            searchButtonImage.removeStyleName("istore-search-button-progress");
            searchBox.removeStyleName("istore-search-box-progress");
        }
        searchBox.setReadOnly(inProgress);
    }

    public <T> void addSearchHandler(SearchHandler<T> handler) {
        addHandler(handler, SearchEvent.getType());
    }

}
