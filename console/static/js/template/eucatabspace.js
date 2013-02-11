

(function($, eucalyptus) {

  $.widget( "eucalyptus.eucatabspace", {
 
    // These options will be used as defaults
    options: { 
      clear: null
    },
 
    // Set up the widget
    _create: function() {
      var thisObj = this;
      var tabspaceTopDiv = $('<div>').addClass('tabspace_top_div_class').attr('id', 'tabspace_top_div_id').text('top-div-place-holder');
      var tabspaceUi = $('<ui>').addClass('tabspace_ui_class').attr('id', 'tabspace_ui_id').text('ui-place-holder');
      var tabspaceContentDiv = $('<div>').addClass('tabspace_content_div_class').attr('id', 'tabspace_content_div_id').text('div-place-holder');
     tabspaceTopDiv.append(tabspaceUi);
     tabspaceTopDiv.append(tabspaceContentDiv);
     thisObj.element.append(tabspaceTopDiv);
    },

    // Use the _setOption method to respond to changes to options
    _setOption: function( key, value ) {
      switch( key ) {
        case "clear":
          // handle changes to clear option
          break;
      }
      // In jQuery UI 1.8, you have to manually invoke the _setOption method from the base widget
      $.Widget.prototype._setOption.apply( this, arguments );
      // In jQuery UI 1.9 and above, you use the _super method instead
      this._super( "_setOption", key, value );
    },

    // Called when the tab is clicked by user
    // This functions hides all the tab page contents and display only the tab passed in the input 'tab'
    _showTabPageContent: function(tab){
//       alert("Handler for .click() called for: " + tab);
      var thisObj = this;

      // Hide all existing tabs
      var tabsapceAllPageContentDivs = thisObj.element.find('#tabspace_content_div_id.tabspace_content_div_class').find('.tabsapce_content_page_div');
      tabsapceAllPageContentDivs.each(function(index, div){
         $(div).hide();
      });

     // Display this tab
      $('#tabspace-content-page-'+tab).show();
    },
    
    //
    // PUBLIC METHODS 
    //

    // Use the destroy method to clean up any modifications your widget has made to the DOM
    destroy: function() {
      // In jQuery UI 1.8, you must invoke the destroy method from the base widget
      $.Widget.prototype.destroy.call( this );
      // In jQuery UI 1.9 and above, you would define _destroy instead of destroy and not call the base method
    },


    // Allows user to add tab-pages
    addTabPage: function(tab, obj){
      var thisObj = this;

      //
      // Create the Tab and Append to the Tabspace UI
      //
      var tabspaceUi = thisObj.element.find('#tabspace_ui_id.tabspace_ui_class');
      var tabspaceTabLi = $('<li>').addClass('tabspace_li').attr('id', tab);
      // Create a callback function that allows switching among the tabs
      tabspaceTabLi.append($('<a>').addClass('button tabspace-top-tab').attr('id', tab).text(tab).click(function(){
          thisObj._showTabPageContent(tab);
        }));
      // Append the newly creaed Tab
      tabspaceUi.append(tabspaceTabLi);

      //
      // Create the Tabspace Page Content Div Block and Append to the Tabspace Content Div Block
      //
      var tabspaceContentDiv = thisObj.element.find('#tabspace_content_div_id.tabspace_content_div_class');
      var tabspacePageContentDiv = $('<div>').addClass('tabsapce_content_page_div').attr('id', 'tabspace-content-page-' + tab).text('content-page-' + tab + '-palce-holder');
      // Error Check: Make sure that the input 'obj' contains an object.
      if( obj != null )
        tabspacePageContentDiv.append(obj.clone());
      // Show the first tab by default while hiding the rest
      if( tabspaceContentDiv.find('.tabsapce_content_page_div').length > 0 )
        tabspacePageContentDiv.hide();
      else
        tabspacePageContentDiv.show();
      // Append the newly create Tabspace Page Content Div Block
      tabspaceContentDiv.append(tabspacePageContentDiv);
    },

  });

}( jQuery ) );

