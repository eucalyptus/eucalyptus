(function($, eucalyptus) {
  $.widget('eucalyptus.keypair', $.eucalyptus.eucawidget, {
    options : { },
    keytable : null,
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
         var thisObj = this;
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
      
      $tmpl = $('html body').find('.templates #keypairDelDlgTmpl').clone();
      $del_dialog = $($tmpl.render($.i18n.map));

      $del_dialog.eucadialog({ 
         id: 'keys-delete',
         title: $('<div>').addClass('help-link').append( 
                  $('<a>').attr('href','#').text('?')),
         buttons: {
           'delete': {text: keypair_dialog_del_btn, click: function() { thisObj._deleteSelectedKeyPairs(); $del_dialog.dialog("close");}},
           'cancel': {text: keypair_dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
         } 
       });

      var createButtonId = 'keys-add-btn';
      $tmpl = $('html body').find('.templates #keypairAddDlgTmpl').clone();
      $add_dialog = $($tmpl.render($.i18n.map));

      // add custom event handler to dialog elements
      // when calling eucadialog, the buttons should have domid to attach the specific domid that's used by event handler written here 
      $add_dialog.find('#key-name').keypress( function(e){
        var $createButton = $('#'+createButtonId);
        if( e.which === RETURN_KEY_CODE || e.which === RETURN_MAC_KEY_CODE ) 
           $createButton.trigger('click');
        else if ( e.which === 0 ) {
        } else if ( e.which === BACKSPACE_KEY_CODE && $(this).val().length == 1 ) 
           $createButton.prop("disabled", true).addClass("ui-state-disabled");
        else if ( $(this).val().length == 0 )
           $createButton.prop("disabled", true).addClass("ui-state-disabled");
        else 
           $createButton.prop("disabled", false).removeClass("ui-state-disabled");
      });

      $add_dialog.eucadialog({
        id: 'keys-add',
        title: $('<div>').addClass('help-link').append(
                 $('<a>').attr('href','#').text('?')),
        buttons: { 
        // e.g., add : { domid: keys-add-btn, text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
        'create': { domid: createButtonId, text: keypair_dialog_create_btn, disabled: true,  click: function() {
                      var keyName = $.trim($add_dialog.find('#key-name').val());
                      var keyPattern = new RegExp('^[A-Za-z0-9_\s-]{1,256}$');
                      if (keyPattern.test(keyName)){
                        $add_dialog.dialog("close"); thisObj._addKeyPair(keyName);
                      }
                      else{
                        // TODO: notification should be handled better, generic way
                        $('#keys-add-dialog div.dialog-notifications').html("Name must be less than 256 alphanumeric characters, spaces, dashes, and/or underscores.");
                      }
                    }
                  },
        'cancel': {text: keypair_dialog_cancel_btn, focus:true, click: function() { $add_dialog.dialog("close");}} 
      }});
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _addKeyPair : function(keyName) {
      alert('add keypair called for '+keyName);
      $.ajax({
        type:"GET",
        url:"/ec2?type=key&Action=CreateKeyPair",
        data:"_xsrf="+$.cookie('_xsrf') + "&KeyName=" + keyName,
        dataType:"json",
        async:"false",
        success:
        function(data, textStatus, jqXHR){
          if (data.results && data.results.material) {
             $.generateFile({
               filename    : keyName,
               content     : data.results.material,
               script      : '/support?Action=DownloadFile&_xsrf=' + $.cookie('_xsrf')
             });
           // TODO: can we wait till file is saved by user?
           successNotification("Added keypair " + keyName);
           // refresh table
           allTablesRef['keys'].fnReloadAjax();
         } else {
           errorNotification("Failed to create keypair " + keyName);
         }
        },
        error:
        function(jqXHR, textStatus, errorThrown){
          errorNotification("Failed to create keypair " + keyName);
        }
      });
    },
    _deleteSelectedKeyPairs : function () {
      var rowsToDelete = getAllSelectedRows('keys', 1);
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var keyName = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?type=key&Action=DeleteKeyPair&KeyName=" + keyName,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"true",
          success:
          (function(keyName) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                successNotification("Deleted keypair " + keyName);
                allTablesRef['keys'].fnReloadAjax();
              } else {
                errorNotification("Failed to delete keypair " + keyName);
              }
           }
          })(keyName),
          error:
          (function(keyName) {
            return function(jqXHR, textStatus, errorThrown){
              errorNotification("Failed to delete keypair " + keyName);
            }
          })(keyName)
        });
      }
    },

    close: function() {
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
