/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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

      this.tableWrapper = $keyTable.eucatable({
        id : 'keys', // user of this widget should customize these options,
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
          ],
          "fnDrawCallback": function( oSettings ) { thisObj._drawCallback(oSettings); }
        },
        text : {
          header_title : keypair_h_title,
          create_resource : keypair_create,
          resource_found : keypair_found,
        },
        menu_actions : function(args){ return thisObj._buildActionsMenu(args);},
        row_click : function (args) { thisObj._handleRowClick(args);},
        menu_click_create : function (args) { thisObj.$addDialog.eucadialog('open')},
        help_click : function(evt) { 
          var $helpHeader = $('<div>').addClass('euca-table-header').append(
                              $('<span>').text(help_keypair['landing_title']).append(
                                $('<div>').addClass('help-link').append(
                                  $('<a>').attr('href','#').html('&larr;'))));
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
        'cancel': {domid: 'keys-cancel-btn', text: keypair_dialog_cancel_btn, focus:true, click: function() { $add_dialog.eucadialog("close");}},
        },
        help : {title: help_keypair['dialog_add_title'], content: $add_help},
      });
      $add_dialog.eucadialog('onKeypress', 'key-name', createButtonId); 
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _drawCallback : function(oSettings) {
      thisObj = this;
      $('#table_keys_count').html(oSettings.fnRecordsDisplay());
      this.element.find('table tbody').find('tr').each(function(index, tr) {
        $currentRow = $(tr);
        $currentRow.click( function (e) {
          // checked/uncheck on checkbox
          $rowCheckbox = $(e.target).parents('tr').find(':input[type="checkbox"]');
          $rowCheckbox.attr('checked', !$rowCheckbox.is(':checked'));
          thisObj._handleRowClick();
        });
        $currentRow.find(':input[type="checkbox"]').click( function (e) {
          $cb = $(this)
          $cb.attr('checked', $cb.is(':checked'));
          thisObj._handleRowClick();
          e.stopPropagation();
        });
      });
    },

    _handleRowClick : function() {
      if ( this.tableWrapper.eucatable('countSelectedRows') == 0 )
        this.tableWrapper.eucatable('deactivateMenu');
      else
        this.tableWrapper.eucatable('activateMenu');
    },

    _addKeyPair : function(keyName) {
      var thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=CreateKeyPair",
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
            notifySuccess('add-key-pair', keypair_create_success + ': ' + keyName);
            thisObj.tableWrapper.eucatable('refreshTable');
          } else {
            notifyError('add-key-pair',keypair_create_error + ' ' + keyName);
          }
        },
        error:
        function(jqXHR, textStatus, errorThrown){
          notifyError('add-key-pair', keypair_delete_error + ' ' + keyName);
        }
      });
    },

    _deleteSelectedKeyPairs : function () {
      var thisObj = this;
      var rowsToDelete = thisObj.tableWrapper.eucatable('getAllSelectedRows');
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var keyName = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DeleteKeyPair&KeyName=" + keyName,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"true",
          success:
          (function(keyName) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess('delete-keypair', keypair_delete_success + ' ' + keyName);
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                notifyError('delete-keypair', keypair_delete_error + ' ' + keyName);
              }
           }
          })(keyName),
          error:
          (function(keyName) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError('delete-keypair', keypair_delete_error + ' ' + keyName);
            }
          })(keyName)
        });
      }
    },
    
    _buildActionsMenu : function() {
      thisObj = this;
      var itemsList = { 'delete': {"name": table_menu_delete_action, callback: function(key, opt) { 
        keysToDelete = thisObj.tableWrapper.eucatable('getValueForSelectedRows', 1);
        if ( keysToDelete.length > 0 ) {
          thisObj.$delDialog.eucadialog('setSelectedResources', keysToDelete);
          thisObj.$delDialog.dialog('open');
        }
      }}}
      return itemsList;
    },

    close: function() {
      this._super('close');
    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
