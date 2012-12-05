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
    baseTable : null,
    delDialog : null,
    addDialog : null,
    importDialog : null,
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #keypairTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_keypair)));
      var $keyTable = $wrapper.children().first();
      var $keyHelp = $wrapper.children().last();
      this.baseTable = $keyTable;
      this.tableWrapper = $keyTable.eucatable({
        id : 'keys', // user of this widget should customize these options,
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": "../ec2?Action=DescribeKeyPairs",
          "fnServerData": function (sSource, aoData, fnCallback) {
                $.ajax( {
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": "_xsrf="+$.cookie('_xsrf'),
                    "success": fnCallback
                });

          },
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell",
            },
            {
              "fnRender": function(oObj) { return oObj.aData.name == null ? "" : "<span title='"+oObj.aData.name+"'>"+addEllipsis(oObj.aData.name, 75)+"</span>" },
              "iDataSort": 3,
            },
            { "mDataProp": "fingerprint", "bSortable": false },
            { "mDataProp": "name", "bVisible": false },
          ],
        },
        text : {
          header_title : keypair_h_title,
          create_resource : keypair_create,
          extra_resource : keypair_import,
          resource_found : 'keypair_found',
          resource_search : keypair_search,
          resource_plural : keypair_plural,
        },
        menu_actions : function(args){ 
          return {'delete': {"name": table_menu_delete_action, callback: function(key, opt) { thisObj._deleteAction(); } }};
        },
        menu_click_create : function (args) {
                                thisObj.addDialog.eucadialog('open');
                                thisObj.addDialog.find('input[id=key-name]').focus();
                            },
        menu_click_extra : function (args) {
                                thisObj.importDialog.eucadialog('open');
                                thisObj.importDialog.find('input[id=key-name]').focus();
                            },
        context_menu_actions : function(state) { 
          return {'delete': {"name": table_menu_delete_action, callback: function(key, opt) { thisObj._deleteAction(); } }};
        },
        help_click : function(evt) { 
          thisObj._flipToHelp(evt, {content:$keyHelp, url: help_keypair.landing_content_url});
        },
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #keypairDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_keypair)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();

      this.delDialog = $del_dialog.eucadialog({
         id: 'keys-delete',
         title: keypair_dialog_del_title,
         buttons: {
           'delete': {text: keypair_dialog_del_btn, click: function() {
                var keysToDelete = thisObj.delDialog.eucadialog('getSelectedResources',1);
                $del_dialog.eucadialog("close");
                thisObj._deleteSelectedKeyPairs(keysToDelete);
            }},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $del_dialog.eucadialog("close");}} 
         },
         help: { content: $del_help, url: help_keypair.dialog_delete_content_url },
       });

      var createButtonId = 'keys-add-btn'; 
      $tmpl = $('html body').find('.templates #keypairAddDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_keypair)));
      $add_dialog = $rendered.children().first();
      $add_help = $rendered.children().last();

      this.addDialog = $add_dialog.eucadialog({
        id: 'keys-add',
        title: keypair_dialog_add_title,
        buttons: { 
        // e.g., add : { domid: keys-add-btn, text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
        'create': { domid: createButtonId, text: keypair_dialog_create_btn, disabled: true,  click: function() {
                      var keyName = $.trim(asText($add_dialog.find('#key-name').val()));
                      if (KEY_PATTERN.test(keyName)){
                        $add_dialog.eucadialog("close"); 
                        thisObj._addKeyPair(keyName);
                      }
                      else{
                        thisObj.addDialog.eucadialog('showError', keypair_dialog_error_msg);
                      }
                    }
                  },
        'cancel': {domid: 'keys-cancel-btn', text: dialog_cancel_btn, click: function() { $add_dialog.eucadialog("close");}},
        },
        help : { content: $add_help, url: help_keypair.dialog_add_content_url, pop_height: 600 },
      });
      $add_dialog.find("#key-name").watermark(keypair_dialog_add_name_watermark);
      $add_dialog.eucadialog('buttonOnKeyup', $add_dialog.find('#key-name'), createButtonId); 
      $add_dialog.eucadialog('validateOnType', '#key-name', function(val) {
        keyName = $.trim(asText(val));
        if (keyName == '')
          return null;
        if (!KEY_PATTERN.test(keyName))
          return keypair_dialog_error_msg;
        else
          return null;
      });
      $tmpl = $('html body').find('.templates #keypairImportDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_keypair)));
      $import_dialog = $rendered.children().first();
      $import_help = $rendered.children().last();

      this.importDialog = $import_dialog.eucadialog({
        id: 'keys-import',
        title: keypair_dialog_import_title,
        buttons: { 
        'create': { domid: createButtonId, text: keypair_dialog_import_btn, disabled: true,  click: function() {
                      var keyName = $.trim(asText($import_dialog.find('#key-name').val()));
                      var keyContents = $.trim(asText($import_dialog.find('#key-import-contents').val()));
                      if (KEY_PATTERN.test(keyName)){
                        thisObj._importKeyPair(keyName, keyContents);
                        $import_dialog.eucadialog("close"); 
                      }
                      else{
                        thisObj.addDialog.eucadialog('showError', keypair_dialog_error_msg);
                      }
                    }
                  },
        'cancel': {domid: 'keys-cancel-btn', text: dialog_cancel_btn, focus:true, click: function() { $import_dialog.eucadialog("close");}},
        },
        help : { content: $import_help, url: help_keypair.dialog_import_content_url, pop_height: 600 },
      });
      $import_dialog.find("#key-name").watermark(keypair_dialog_add_name_watermark);
      $import_dialog.find("#key-import-contents").watermark(keypair_dialog_import_contents_watermark);
      $import_dialog.eucadialog('buttonOnKeyup', $import_dialog.find('#key-name'), createButtonId); 
      $import_dialog.find("input[type=file]").on('change', function(evt) {
            var file = evt.target.files[0];
            var reader = new FileReader();
            reader.onloadend = function(evt) {
                if (evt.target.readyState == FileReader.DONE) {
                    $import_dialog.find("#key-import-contents").val(evt.target.result);
                }
            }
            reader.readAsText(file);
        });
    },

    _destroy : function() {
    },

    _deleteAction : function() {
      var thisObj = this;
      var keysToDelete = [];
      var $tableWrapper = thisObj.tableWrapper;
      keysToDelete = $tableWrapper.eucatable('getSelectedRows', 3);
      var matrix = [];
      $.each(keysToDelete,function(idx, key){
        matrix.push([key, key]);
      });

      if ( keysToDelete.length > 0 ) {
        thisObj.delDialog.eucadialog('setSelectedResources', {title:[keypair_label], contents: matrix, limit:60, hideColumn: 1});
        thisObj.delDialog.dialog('open');
      }
    },

    _addKeyPair : function(keyName) {
      var thisObj = this;

      $.ajax({
        type:"POST",
        url:"/ec2?Action=CreateKeyPair",
        data:"_xsrf="+$.cookie('_xsrf') + "&KeyName=" + keyName,
        dataType:"json",
        async:false,
        success:
        function(data, textStatus, jqXHR){
          if (data.results && data.results.material) {
            $.generateFile({
              filename  : keyName,
              keyname   : keyName,
              _xsrf     : $.cookie('_xsrf'),
              script    : '/ec2?Action=GetKeyPairFile'
            });
            notifySuccess(null, $.i18n.prop('keypair_create_success', addEllipsis(keyName, 75)));
            thisObj.tableWrapper.eucatable('refreshTable');
            thisObj.tableWrapper.eucatable('glowRow', keyName);
          } else {
            notifyError($.i18n.prop('keypair_create_error', addEllipsis(keyName, 75)), undefined_error);
          }
        },
        error:
        function(jqXHR, textStatus, errorThrown){
          notifyError($.i18n.prop('keypair_create_error', addEllipsis(keyName, 75)), getErrorMessage(jqXHR));
        }
     });
       
    },

    _deleteSelectedKeyPairs : function (keysToDelete) {
      var thisObj = this;
      var done = 0;
      var all = keysToDelete.length;
      var error = [];

      doMultiAjax(keysToDelete, function(item, dfd){
        var keyName = item;
        $.ajax({
          type:"POST",
          url:"/ec2?Action=DeleteKeyPair",
          data:"_xsrf="+$.cookie('_xsrf')+"&KeyName="+keyName,
          dataType:"json",
          async:true,
          success: (function(keyName) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                ;
              } else {
                error.push({id:keyName, reason: undefined_error});
              }
           }
          })(keyName),
          error: (function(keyName) {
            return function(jqXHR, textStatus, errorThrown){
              error.push({id:keyName, reason:  getErrorMessage(jqXHR)});
            }
          })(keyName),
          complete: (function(keyName) {
            return function(jqXHR, textStatus){
              done++;
              if(done < all)
                notifyMulti(100*(done/all), $.i18n.prop('keypair_delete_progress', all));
              else {
                var $msg = $('<div>').addClass('multiop-summary').append(
                  $('<div>').addClass('multiop-summary-success').html($.i18n.prop('keypair_delete_done', (all-error.length), all)));
                if (error.length > 0)
                  $msg.append($('<div>').addClass('multiop-summary-failure').html($.i18n.prop('keypair_delete_fail', error.length)));
                notifyMulti(100, $msg.html(), error);
                thisObj.tableWrapper.eucatable('refreshTable');
              }
              dfd.resolve();
            }
          })(keyName),
        });
      });
    },
   
    _importKeyPair : function (keyName, keyContents) {
      var thisObj = this;
      var params = "_xsrf="+$.cookie('_xsrf')+"&KeyName="+keyName;
//    params += "&PublicKeyMaterial="+btoa(keyContents);  // sounds like btoa won't work on IE9?
      params += "&PublicKeyMaterial="+toBase64(keyContents);
      $.ajax({
        type:"POST",
        url:"/ec2?Action=ImportKeyPair",
        data:params,
        dataType:"json",
        async:true,
        success:
        (function(keyName) {
          return function(data, textStatus, jqXHR){
            if (data.results && data.results.fingerprint) {
              notifySuccess(null, $.i18n.prop('keypair_import_success', addEllipsis(keyName, 75)));
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError($.i18n.prop('keypair_import_error', keyName), undefined_error);
            }
         }
        })(keyName),
        error:
        (function(keyName) {
          return function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('keypair_import_error', addEllipsis(keyName, 75)), getErrorMessage(jqXHR));
          }
        })(keyName, keyContents)
      });
    },
   
/**** Public Methods ****/ 
    close: function() {
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },

    dialogAddKeypair : function(callback) {
      var thisObj = this;
      if(callback)
        thisObj.addDialog.data('eucadialog').option('on_close', {callback: callback});
      thisObj.addDialog.eucadialog('open')
    }, 
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
