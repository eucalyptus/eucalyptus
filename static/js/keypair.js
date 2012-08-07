(function($, eucalyptus) {
  $.widget('eucalyptus.keypair', $.eucalyptus.eucawidget, {
    options : { },
    keytable : null,
    
    _init : function() {
         var $tmpl = $('html body').find('.templates #keypairTblTmpl').clone();
         var $wrapper = $($tmpl.render($.i18n.map));
         this.element.add($wrapper);
         
         var $base_table = $wrapper.find('table'); 
         $wrapper.eucatable({
           id : 'keys', // user of this widget should customize these options,
           base_table : $base_table,
           dt_arg : {
                "bProcessing": true,
                "sAjaxSource": "../ec2?type=key&Action=DescribeKeyPairs",
                "sAjaxDataProp": "results",
                "bAutoWidth" : false,
                "sPaginationType": "full_numbers",
                "sDom": '<"table_keys_header">f<"clear"><"table_keys_top">rtp<"clear">',
                "aoColumns": [
                  {
                    "bSortable": false,
                    "fnRender": function(oObj) { return '<input type="checkbox" onclick="updateActionMenu(\'keys\')"/>' },
                    "sWidth": "20px",
                  },
                  { "mDataProp": "name" },
                  { "mDataProp": "fingerprint", "bSortable": false }
                ],
                "fnDrawCallback": function( oSettings ) {
                $('#table_keys_count').html(oSettings.fnRecordsTotal());
                }
              },
           header_title : keypair_h_title,
           search_refresh : search_refresh,
           txt_create : keypair_create,
           txt_found : keypair_found,
           menu_text : table_menu_main_action,
           menu_actions : { delete: [table_menu_delete_action, function () { deleteAction('keys', 1); } ] }
      }).appendTo(this.element);
      //TODO: can dialogs be more generic?
      // init delete dialog
      var deleteButtonId = "delete-" + S4();
      var cancelButtonId = "cancel-" + S4();
      $('#keys-delete-dialog').dialog({
         autoOpen: false,
         modal: true,
         width: 600,
         open: function(event, ui) {
	     $('.ui-widget-overlay').live("click", function() {
	       $('#keys-delete-dialog').dialog("close");
	     });
             $(":button:contains('Delete')").attr('id', deleteButtonId);
             $('#' + deleteButtonId + ' span').text("Yes, delete");
             $(":button:contains('Cancel')").attr('id', cancelButtonId);
             $('#' + cancelButtonId + ' span').text("Cancel");
             $('#' + cancelButtonId).focus();
             $('.ui-dialog-titlebar').append('<div class="help-link"><a href="#">?</a></div>');
           },
         buttons: [
          {
            text: "Delete",
            click: function() { deleteSelectedKeyPairs(); $(this).dialog("close"); }
          },
          {
            text: "Cancel",
            click: function() { $(this).dialog("close"); }
          }
        ]
      });
      // init add dialog
      var createButtonId = "create-" + S4();
      var cancelButtonId = "cancel-" + S4();
      $('#keys-add-dialog').dialog({
         autoOpen: false,
         modal: true,
         width: 600,
         open: function(event, ui) {
	     $('.ui-widget-overlay').live("click", function() {
	       $('#keys-add-dialog').dialog("close");
	     });
             $('.ui-dialog-titlebar').append('<div class="help-link"><a href="#">?</a></div>');
             $(":button:contains('Create')").attr('id', createButtonId);
             $createButton = $('#' + createButtonId);
             $createButton.prop("disabled", true).addClass("ui-state-disabled");
             $('#' + createButtonId + ' span').text("Create and download");
             $(":button:contains('Cancel')").attr('id', cancelButtonId);
             $('#' + cancelButtonId + ' span').text("Cancel");
             $('#key-name').keypress( function(e) {
               if( e.which === RETURN_KEY_CODE || e.which === RETURN_MAC_KEY_CODE ) {
                 $('#' + createButtonId).trigger('click');
               } else if ( e.which === 0 ) {
               } else if ( e.which === BACKSPACE_KEY_CODE && $(this).val().length == 1 ) {
                 $createButton.prop("disabled", true).addClass("ui-state-disabled");
               } else if ( $(this).val().length == 0 ) {
                 $createButton.prop("disabled", true).addClass("ui-state-disabled");
               } else {
                 $createButton.prop("disabled", false).removeClass("ui-state-disabled");
               }
             });
           },
         buttons: [
          {
            text: "Create", // do not translate here
            click: function() {
              var keyName = $.trim($('#key-name').val());
              var keyPattern = new RegExp('^[A-Za-z0-9_\s-]{1,256}$');
              if (keyPattern.test(keyName)) {
                $(this).dialog("close"); addKeyPair(keyName);
              } else {
                $('#keys-add-dialog div.dialog-notifications').html("Name must be less than 256 alphanumeric characters, spaces, dashes, and/or underscores.");
              }
            }
          },
          {
            text: "Cancel", // do not translate here
            click: function() { $(this).dialog("close"); }
          }
        ]
      });

    },

    _create : function() { 
    },

    _destroy : function() {
    },

    close: function() {
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
