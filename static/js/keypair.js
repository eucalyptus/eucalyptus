(function($, eucalyptus) {
  $.widget('eucalyptus.keypair', $.eucalyptus.eucawidget, {
    options : { },
    tableWrapper : null,
    delDialog : null,
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #keypairTblTmpl').clone();
      var $wrapper = $($tmpl.render($.i18n.map));
      this.element.add($wrapper);
      var $base_table = $wrapper.find('table');
      tableWrapper = $wrapper.eucatable({
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
              "fnRender": function(oObj) { return '<input type="checkbox" onclick="updateActionMenu(this)"/>' },
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
        menu_actions : { delete: [table_menu_delete_action, function (args) { thisObj.deleteAction(args) } ] }
      });
      tableWrapper.appendTo(this.element);

      $tmpl = $('html body').find('.templates #keypairDelDlgTmpl').clone();
      $del_dialog = $($tmpl.render($.i18n.map));

      this.delDialog = $del_dialog.eucadialog({
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
                        $('#keys-add-dialog div.dialog-notifications').html(keypair_dialog_error_msg);
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
            successNotification(keypair_create_success + ' ' + keyName);
            tableWrapper.eucatable('refreshTable');
          } else {
            errorNotification(keypair_create_error + ' ' + keyName);
          }
        },
        error:
        function(jqXHR, textStatus, errorThrown){
          errorNotification(keypair_delete_error + ' ' + keyName);
        }
      });
    },

    _deleteSelectedKeyPairs : function () {
      var rowsToDelete = tableWrapper.eucatable('getAllSelectedRows');
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
                successNotification(keypair_delete_success + ' ' + keyName);
                tableWrapper.eucatable('refreshTable');
              } else {
                errorNotification(keypair_delete_error + ' ' + keyName);
              }
           }
          })(keyName),
          error:
          (function(keyName) {
            return function(jqXHR, textStatus, errorThrown){
              errorNotification(keypair_delete_error + ' ' + keyName);
            }
          })(keyName)
        });
      }
    },

    close: function() {
      this._super('close');
    },

    deleteAction : function(rowsToDelete) {
      //TODO: add hide menu

      if ( rowsToDelete.length > 0 ) {
        // show delete dialog box
        $deleteNames = this.delDialog.find("span.delete-names")
        $deleteNames.html('');
        for ( i = 0; i<rowsToDelete.length; i++ ) {
          t = escapeHTML(rowsToDelete[i]);
          $deleteNames.append(t).append("<br/>");
        }
        this.delDialog.dialog('open');
      }
    }

  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
