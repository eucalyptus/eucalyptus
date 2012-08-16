/*************************************************************************
 * Copyright 2011-2012 Eucalyptus Systems, Inc.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

(function($, eucalyptus) {
  $.widget('eucalyptus.keypair', $.eucalyptus.eucawidget, {
    options : { },
    tableWrapper : null,
    $delDialog : null,
    $addDialog : null,
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #keypairTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_keypair)));
      var $keyTable = $wrapper.children().first();
      var $keyHelp = $wrapper.children().last();
      this.element.add($keyTable);

      var $base_table = $keyTable.find('table');
      this.tableWrapper = $keyTable.eucatable({
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
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { "mDataProp": "name" },
            { "mDataProp": "fingerprint", "bSortable": false }
          ]
        },
        header_title : keypair_h_title,
        search_refresh : search_refresh,
        txt_create : keypair_create,
        txt_found : keypair_found,
        menu_text : table_menu_main_action,
        menu_actions : { delete: { name: table_menu_delete_action, callback: function(key, opt) { thisObj.deleteAction(thisObj); } } },
        row_click : function (args) { thisObj.handleRowClick(args); },
        menu_click_create : function (args) { thisObj.$addDialog.eucadialog('open')},
        help_click : function(evt) { 
          var $helpHeader = $('<div>').addClass('euca-table-header').append(
                              $('<span>').text(help_keypair['landing_title']));
          thisObj._flipToHelp(evt,$helpHeader, $keyHelp);
        },
      });
      this.tableWrapper.appendTo(this.element);

      var $tmpl = $('html body').find('.templates #keypairDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_keypair)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();

      this.$delDialog = $del_dialog.eucadialog({
         id: 'keys-delete',
         title: keypair_dialog_del_title,
         buttons: {
           'delete': {text: keypair_dialog_del_btn, click: function() { thisObj._deleteSelectedKeyPairs(); $del_dialog.dialog("close");}},
           'cancel': {text: keypair_dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
         },
         help: {title: help_keypair['dialog_delete_title'], content: $del_help}, 
       });

      var createButtonId = 'keys-add-btn'; 
      $tmpl = $('html body').find('.templates #keypairAddDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_keypair)));
      $add_dialog = $rendered.children().first();
      $add_help = $rendered.children().last();

      // add custom event handler to dialog elements
      // when calling eucadialog, the buttons should have domid to attach the specific domid that's used by event handler written here 
      $add_dialog.find('#key-name').keypress( function(e){
        var $createButton = $('#'+createButtonId);
        if( e.which === RETURN_KEY_CODE || e.which === RETURN_MAC_KEY_CODE ) {
           $createButton.trigger('click');
        } else if ( e.which === 0 ) {
        } else if ( e.which === BACKSPACE_KEY_CODE && $(this).val().length == 1 ) {
           $createButton.prop("disabled", true).addClass("ui-state-disabled");
        } else {
           $createButton.prop("disabled", false).removeClass("ui-state-disabled");
        }
      });

      this.$addDialog = $add_dialog.eucadialog({
        id: 'keys-add',
        title: keypair_dialog_add_title,
        buttons: { 
        // e.g., add : { domid: keys-add-btn, text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
        'create': { domid: createButtonId, text: keypair_dialog_create_btn, disabled: true,  click: function() {
                      var keyName = $.trim($add_dialog.find('#key-name').val());
                      var keyPattern = new RegExp('^[A-Za-z0-9_\s-]{1,256}$');
                      if (keyPattern.test(keyName)){
                        $add_dialog.dialog("close"); 
                        thisObj._addKeyPair(keyName);
                      }
                      else{
                        // TODO: notification should be handled better, generic way
                        $('#keys-add-dialog div.dialog-notifications').html(keypair_dialog_error_msg);
                      }
                    }
                  },
        'cancel': {text: keypair_dialog_cancel_btn, focus:true, click: function() { $add_dialog.eucadialog("close");}},
        },
        help : {title: help_keypair['dialog_add_title'], content: $add_help},
      });
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    handleRowClick : function(args) {
      if ( this.tableWrapper.eucatable('countSelectedRows') == 0 )
        this.tableWrapper.eucatable('deactivateMenu');
      else
        this.tableWrapper.eucatable('activateMenu');
    },

    _addKeyPair : function(keyName) {
      var thisObj = this;
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
            thisObj.tableWrapper.eucatable('refreshTable');
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
      var thisObj = this;
      var rowsToDelete = thisObj._getTableWrapper().eucatable('getAllSelectedRows');
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
                thisObj._getTableWrapper().eucatable('refreshTable');
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

    _getTableWrapper : function() {
      return this.tableWrapper;
    },

    deleteAction : function(thisObj) {
      $tableWrapper = thisObj._getTableWrapper();
      rowsToDelete = $tableWrapper.eucatable('getAllSelectedRows');
      if ( rowsToDelete.length > 0 ) {
        // show delete dialog box
        $deleteNames = this.$delDialog.find("span.delete-names")
        $deleteNames.html('');
        for ( i = 0; i<rowsToDelete.length; i++ ) {
          t = escapeHTML(rowsToDelete[i]);
          $deleteNames.append(t).append("<br/>");
        }
        this.$delDialog.dialog('open');
      }
    }

  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
