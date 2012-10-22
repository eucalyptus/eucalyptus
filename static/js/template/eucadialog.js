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
  $.widget('eucalyptus.eucadialog', {
    options : { 
       id : null, // e.g., keys-add, keys-delete, etc
       title : null,
       // e.g., add : { domid: keys-add-btn, text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
       buttons :  {},
       help : null,  // { content: ... }
       on_open: null, // {spin: True, callback: function(){}}
       user_val: null, // for user use...
       help_icon_class : 'help-link',
       on_close: null,
       width : null,
       afterHelpFlipped : null,
       beforeHelpFlipped : null,
    },
    $error_div : null,
    help_flipped : false,
    _init : function() {
      var thisObj = this;
      /// create div for displaying errors
      thisObj.$error_div = $('<div>').addClass('dialog-error');
      thisObj.element.append(thisObj.$error_div);

      this.element.dialog({
         autoOpen: false,  // assume the three params are fixed for all dialogs
         modal: true,
         width: thisObj.options.width ? thisObj.options.width : 600,
         maxHeight: thisObj.options.maxHeight ? thisObj.options.maxHeight : false,
         height: thisObj.options.height ? thisObj.options.height : 'auto',
         dialogClass: 'euca-dialog-container',
         show: 'fade',
         // don't add hide: 'fade' here b/c it causes an issue with positioning the dialog next to another dialog
         resizable: false,
         closeOnEscape : true,
         position: { my: 'center', at: 'center', of: window, collision: 'none'},
         title: thisObj.options.title,
         user_val: thisObj.options.user_val,
         open: function(event, ui) {
             iamBusy();
             $titleBar = thisObj.element.parent().find('.ui-dialog-titlebar');
             if($titleBar.find('.' + thisObj.options.help_icon_class).length <= 0)
               $titleBar.append($('<div>').addClass(thisObj.options.help_icon_class).append($('<a>').attr('href','#').text('?')));

             $.each(thisObj.options.buttons, function (btn_id, btn_prop){
               var btn_dom_id = thisObj._getButtonId(btn_prop.domid, btn_id);
               var $btn = thisObj.element.parent().find("#"+btn_dom_id);
               if (!$btn || $btn.length<=0) {
                 $btn = thisObj.element.parent().find(":button:contains('"+ btn_id +"')");
                 $btn.attr('id', btn_dom_id);
               };
               $btn.find('span').text(btn_prop.text);
               if(btn_prop.focus !== undefined && btn_prop.focus){
                 $btn.focus();
               }
               if(btn_prop.disabled !== undefined && btn_prop.disabled){
                 $btn.prop("disabled", true).addClass("ui-state-disabled");
               }
             });

             /* page-level help -- prototype */
             thisObj._setHelp(thisObj.element.parent());
             /* call onOpen function if passed */
             if ( thisObj.options.on_open ){
               if(thisObj.options.on_open['spin']){
                 thisObj._activateSpinWheel();
                 if ($.type(thisObj.options.on_open.callback) === 'array'){
                   $.each(thisObj.options.on_open.callback, function(idx, callback){
                     $.when(callback()).done( function(){ thisObj._removeSpinWheel(); }
                       ).fail( function(){ thisObj.element.dialog('close'); }
                     );
                   });
                 }else{
                   $.when(thisObj.options.on_open.callback()).done( function(){ thisObj._removeSpinWheel(); }
                     ).fail( function(){ thisObj.element.dialog('close'); }
                   );
                 }
               } else {
                 if ($.type(thisObj.options.on_open.callback) === 'array'){
                   $.each(thisObj.options.on_open.callback, function(idx, callback){
                     callback();
                   });
                 }else
                   thisObj.options.on_open.callback();
               }
             }
             // clean old errors on open
             thisObj.element.find('.field-error').detach();
         },
         close: function(event, ui) { 
           if( thisObj.options.on_close) { 
             thisObj.options.on_close.callback();
           }
         },
         buttons: thisObj._makeButtons(),
      });
      this.element.qtip();
    },

    _getButtonId : function(buttonDomId, buttonId) {
      return buttonDomId !== undefined ? buttonDomId : 'btn-' + this.options.id + '-' + buttonId;
    },

    _create : function() {  
    },

    _destroy : function() {
    },

    _setOption: function(key, value) {
      if (key === 'title') {
        this.element.dialog('option', 'title', value);
      }
      $.Widget.prototype._setOption.apply(this, arguments);
    },

    _activateSpinWheel : function() {
      var $spinWheel = $('<div>').addClass('status-readout').append(
                         $('<img>').attr('src','images/dots32.gif'),
                       $('<span>').text('Loading...')
                       );
      this.element.prepend($spinWheel);
      this.element.find('.dialog-inner-content').hide();
    },
 
    _removeSpinWheel : function() {
      var $spinWheel = this.element.find('.status-readout');
      $spinWheel.remove();
      this.element.find('.dialog-inner-content').show();
    },

    _setHelp : function($dialog) {
      var thisObj = this;
      var $buttonPane = $dialog.find('.ui-dialog-buttonpane');
      var $titleBar = $dialog.find('.ui-dialog-titlebar');
      var $contentPane =  this.element.find('.dialog-inner-content');
      $contentPane.after($('<div>').addClass('dialog-help-content').append(thisObj.options.help.content).hide()); 
      var $helpPane = this.element.find('.dialog-help-content');
      var $resourcePane = this.element.find('.selected-resources');
      var $helpLink = $titleBar.find('.'+thisObj.options.help_icon_class+' a');
      var $help = thisObj.options.help;
      if(!$help || !$help.content || !$help.content.find('.dialog-help-content').html() || $help.content.find('.dialog-help-content').html().trim().length <= 0){
        $helpLink.remove();
        return;
      }
      $helpLink.click(function(evt) {
        if(!thisObj.help_flipped){ // TODO: is this right comparison(text comparison)?
          thisObj.element.data('dialog').option('closeOnEscape', false);
          if (thisObj.options.beforeHelpFlipped && isFunction(thisObj.options.beforeHelpFlipped))
            thisObj.options.beforeHelpFlipped.call();
          $contentPane.fadeOut(function(){
            $helpPane.fadeIn();
            thisObj.help_flipped = true;
            $titleBar.find('.'+thisObj.options.help_icon_class).removeClass().addClass('help-return').before(
              $('<div>').addClass('help-popout').append(
                $('<a>').attr('href','#').text('popout').click(function(e){
                  if(thisObj.options.help['url']){
                    if(thisObj.options.help['pop_height'])
                      popOutDialogHelp(thisObj.options.help['url'], thisObj.options.help['pop_height']);
                    else
                      popOutDialogHelp(thisObj.options.help['url']);
                  }
                  $helpLink.trigger('click');
                })));
            $helpLink.html('&nbsp;');
            $buttonPane.hide();
            $resourcePane.hide();
            $titleBar.find('span').text('');
            if (thisObj.options.afterHelpFlipped && isFunction(thisObj.options.afterHelpFlipped))
              thisObj.options.afterHelpFlipped.call();
          });
        }else{
          thisObj.element.data('dialog').option('closeOnEscape', true);
          $helpPane.fadeOut(function(){
            $contentPane.fadeIn();
            thisObj.help_flipped = false;
            $titleBar.find('.help-popout').detach();
            $titleBar.find('.help-return').removeClass().addClass(thisObj.options.help_icon_class);
            $helpLink.html('&nbsp;');
            $buttonPane.show();
            $resourcePane.show();
            $titleBar.find('span').text(thisObj.options.title);
          });
        }
      });
      thisObj.element.find('.help-revert-button a').click( function(evt) {
        $helpLink.trigger('click');
      });
    },
    _makeButtons : function() {
      var btnArr = [];
       // e.g., add : { text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
      $.each(this.options.buttons, function (btn_id, btn_prop){
        var btn = {}
        btn.text = btn_id; // this will be replaced in open()
        $.each(btn_prop, function(key, value){
          if (typeof(value) == "function")
            btn[key] = value;
        });
        btnArr.push(btn);  
      });
      return btnArr; 
    },

/**** Public Methods ****/
    open : function() {
      this.element.dialog('open');      
    },

    close : function() {
      var thisObj = this;
      this.element.dialog('close');
      // this method should clean-up things
      this.element.find('input').each(function () { 
        $(this).val(''); // clear all input fields TODO: what if some fields have initialized data?
      });
      this.element.find('textarea').each(function () { 
        $(this).val(''); // clear all input fields TODO: what if some fields have initialized data?
      });
      if(this.help_flipped){
        this.element.find('.dialog-inner-content').revertFlip();
        $buttonPane.show();
        $titleBar.find('span').text(thisObj.options.title);
      } 

      $.each(thisObj._note_divs, function(idx, id){
        thisObj.element.find('#'+id).children().detach();
      });
      thisObj._note_divs = [];
      this.element.find('.dialog-error').children().detach();
    },

    hideButton : function(buttonDomId, buttonId) {
      var effectiveId = this._getButtonId(buttonDomId, buttonId);
      this.element.parent().find('#'+effectiveId).hide();
    },

    showButton : function(buttonDomId, buttonId, disabled) {
      var effectiveId = this._getButtonId(buttonDomId, buttonId);
      var $button = this.element.parent().find('#'+effectiveId);
      $button.show();
      if ( disabled )
        $button.prop("disabled", true).addClass("ui-state-disabled");
    },

    enableButton : function(buttonDomId){
      buttonDomId = buttonDomId.replace('#','');
      var $button = this.element.parent().find('#'+buttonDomId); 
      $button.removeAttr("disabled").removeClass("ui-state-disabled");
    },

    disableButton : function(buttonDomId){
      buttonDomId = buttonDomId.replace('#','');
      var $button = this.element.parent().find('#'+buttonDomId); 
      $button.prop("disabled", true).addClass("ui-state-disabled");
 
    },

    /// 
    /// resources ={title:[ , ], contents:[[val0_0, val0_1, .. ], [val1_0, val1_2, ..]] 
    setSelectedResources : function (resources) {
      var thisObj = this;
      var $div = this.element.find('.selected-resources');
      if (! $div){ return false; }
      $div.children().detach();
      $div.append($('<table>').append($('<thead>'), $('<tbody>')));
      var $head = $div.find('thead');
      var $body = $div.find('tbody');

      var $tr = $('<tr>');
      $.each(resources.title, function(idx, val){
        $tr.append($('<th>').text(val.toUpperCase())); 
      }); 
      $head.append($tr);
      $.each(resources.contents, function(i, row){
        $tr = $('<tr>');
        $.each(row, function(j, val){
          $tr.append($('<td>').text(val)); 
        });
        $body.append($tr);
      });
      return true; 
    },

    getSelectedResources : function (column) {
      var thisObj = this;
      var $body = thisObj.element.find('tbody');
      var array = [];
      $body.find('tr').each( function() {
        var val = $(this).find('td:eq('+column+')').text();
        array.push(val);
      }); 
      return array;           
    },

    _note_divs : [],
    addNote : function(div_id, note){
      // insert the additional notes in place of the div id=div_id;
      var $div = this.element.find('#'+div_id);
      if(!$div) return;
      $div.html(note);
      this._note_divs.push(div_id);
    },

    removeNote : function(div_id){
      var thisObj = this;
      var idxToDel = -1;
      $.each(thisObj._note_divs, function(idx, id){
        if(id === div_id){
          thisObj.element.find('#'+id).children().detach();
          idxToDel = idx;
        }
      });
      if(idxToDel >=0){
        thisObj._note_divs.splice(idxToDel, 1);
      }
    },

    buttonOnChange : function(evtSrc, buttonId, check){
      var thisObj = this;
      evtSrc.unbind('change').bind('change', function(e){
        var button = thisObj.element.parent().find('#'+buttonId.replace('#',''));
        if (isFunction(check) && check.call(evtSrc))
          button.removeAttr('disabled').removeClass('ui-state-disabled');
        else
          button.prop('disabled',true).addClass('ui-state-disabled');
      });
    },

    buttonOnFocus : function(evtSrc, buttonId, check){
      var thisObj = this;
      evtSrc.focusout( function(e){
        var button = thisObj.element.parent().find('#'+buttonId.replace('#',''));
        if (isFunction(check) && check.call(evtSrc))
          button.removeAttr('disabled').removeClass('ui-state-disabled');
        else
          button.prop('disabled',true).addClass('ui-state-disabled');
      });
    },
      
    buttonOnKeyup : function(evtSrc, buttonId, checkFunction){
      var thisObj = this;
      var $button = null;
      evtSrc.keyup( function(e){
        if( $button==null )
          $button = thisObj.element.parent().find('#'+buttonId.replace('#',''));
        if( e.which === RETURN_KEY_CODE || e.which === RETURN_MAC_KEY_CODE ) {
           if ( isFunction(checkFunction) ) {
             if ( checkFunction.call(this) )
               $button.trigger('click');
           } else {
               $button.trigger('click');
           }
        } else if ( e.which === 0 ) {
        } else if ( e.which === BACKSPACE_KEY_CODE && $(this).val().length == 0 ) {
          $button.prop("disabled", true).addClass("ui-state-disabled");
        } else {
           if ( isFunction(checkFunction) ) {
             if ( checkFunction.call(this) )
               $button.prop("disabled", false).removeClass("ui-state-disabled");
           } else {
             $button.prop("disabled", false).removeClass("ui-state-disabled");
           }
        }
      });
    },

    activateButton : function(buttonId) {
      var $button = this.element.parent().find('#'+buttonId.replace('#',''));
      $button.prop("disabled", false).removeClass("ui-state-disabled");
    },

    onChange : function(evt_src_id, button_id, checkFunction) {
      var evt_src_id = evt_src_id.replace('#','');
      var $evt_src = this.element.find('#'+evt_src_id);

      $evt_src.unbind('change').bind('change',function(e){
         if ( isFunction(checkFunction) ) {
           checkFunction.call(this);
         }
      });
    },
    
    // This function takes the name of a value (input field's id) and a regular
    // expression object to match against. It tests the value of the field and if it matches,
    // returns the value, otherwise a null is returned and an error message is set on a
    // div named for the field (with "-error" appended to the id)
    // i.e.  <input id="sgroup-name" type="text" class="required" maxlength="254"/>
    //       <div id="sgroup-name-error" class="sg-error-msg"></div>
    get_validate_value : function(val_name, pattern, msg) {
      var val = this.getValue('#'+val_name);
      if (pattern.test(val)) {
        this.element.find("#"+val_name+"-error").html("");
        return val;
      }
      else {
        this.element.find("#"+val_name+"-error").html(msg);
        return null;
      }
    },

    getValue : function(fieldSelector) {
      var field = this.element.find(fieldSelector);
      if (field) {
        return asText($.trim(field.val()));
      } else {
        return undefined;
      }
    },

    validateOnType : function(fieldSelector, checkFunction) {
      var thisObj = this;
      var field = thisObj.element.find(fieldSelector);
      if (field && isFunction(checkFunction)) {
        field.keyup( function() {
          errorMsg = checkFunction.call(this, $.trim(field.val()));
          if (errorMsg)
            thisObj.showFieldError(fieldSelector, errorMsg);
          else
            thisObj.hideFieldError(fieldSelector);
        });
      }
    },

    hideFieldError : function(fieldSelector) {
      var $input = this.element.find(fieldSelector);
      $next = $input.next();
      if ($next && 'field-error' == $next.attr('class')) {
        $next.detach();
      }
    },

    showFieldError : function(fieldSelector, error) {
      var $input = this.element.find(fieldSelector);
      $next = $input.next();
      if ($next && 'field-error' != $next.attr('class')) {
        $input.after($('<div>').addClass('field-error').html(error));
      } else {
        $next.html(error);
      }
    },

    showError : function(error, append){
      if(!append)
        this.$error_div.children().detach();
      else
        this.$error_div.append($('<br>'));
      this.$error_div.append(
        $('<span>').html(error)); 
    },

    getDialogOption : function(option){
      return this.element.dialog('option', option);
    },

    setDialogOption : function(option, value) {
      this.element.dialog('option', option, value);
    },

/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
