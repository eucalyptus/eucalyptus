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
  $.widget('eucalyptus.maincontainer', {
    options : { 
        default_selected : 'dashboard',
    },

    _curSelected : null,
    _changepwdDialog : null,
    _aboutDialog : null,

    _init : function() {
      var hash = location.hash;
      if (hash)
        hash = hash.replace(/^#/, '');
      if (hash !== '')
        this.updateSelected(hash);
      else
        this.updateSelected(this.options.default_selected);
      this.element.show();
    },

    _create : function() {
      var thisObj = this;
      // change password dialog
      $tmpl = $('html body').find('.templates #changePasswordTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_changepwd)));
      var $cp_dialog = $rendered.children().first();
      var $cp_dialog_help = $rendered.children().last();
      this._changepwdDialog = $cp_dialog.eucadialog({
        id: 'change-passwd',
        title: login_change_passwd_title,
        buttons: {
          'change': { domid: 'change-pwd', text: login_change_passwd_submit, disabled: true, click: function() {
              // should show activity somehow
              //$form.find('.button-bar').append(
              //  $('<img>').attr('id','login-spin-wheel').attr('src','images/dots32.gif'));
              var current = trim($form.find('input[id=current]').val());
              var newpwd = trim($form.find('input[id=newpwd]').val());
              var confirmpwd = trim($form.find('input[id=confirmpwd]').val());

              var isValid = true;
              if (newpwd != confirmpwd) {
                isValid = false;
                thisObj._changepwdDialog.eucadialog('showError', login_change_passwd_dont_match);
              }
              
              if (isValid) {
                thisObj._trigger('doLogin', evt, { param: param,
                  onSuccess: function(args){
                    if ($.eucaData['u_session']['account'] === 'eucalyptus'){
                      thisObj.popupWarning(login_account_warning, function(){ 
                        var admin_url = $.eucaData.g_session['admin_console_url'];
                        window.open(admin_url, '_blank');
                      });
                    }

                    thisObj._changepwdDialog.eucadialog("close");
                    eucalyptus.main($.eucaData);
                  },
                  onError: function(args){
                    $form.find('.button-bar img').remove();
                    $form.find('.button-bar input').removeAttr('disabled');
                    $form.find('.button-bar input').show();
                    thisObj.errorDialog.eucadialog('open');
                    var msgdiv = thisObj.errorDialog.find("#login-error-message p")
                    if (args.search("Timeout")>-1) {
                      // XSS Note:: No need to encode 'cloud_admin' since it's a static string from the file "messages.properties" - Kyo
                      msgdiv.addClass('dialog-error').html($.i18n.prop('login_timeout', '<a href="#">'+cloud_admin+'</a>'));
                      msgdiv.find('a').click( function(e){
                        if(thisObj.options.support_url.indexOf('mailto') >= 0)
                          window.open(thisObj.options.support_url, '_self');
                        else
                          window.open(thisObj.options.support_url,'_blank');
                      });
                    } else {
                      msgdiv.addClass('dialog-error').html(login_failure);
                    }
                    thisObj._changepwdDialog.eucadialog("close");
                  }
                });
              }
              return false;
            }
          },
          'cancel': { text: dialog_cancel_btn, focus:false, click: function() { $cp_dialog.eucadialog("close"); } }
        },
        help: { content: $cp_dialog_help, url: help_changepwd.dialog_attach_content_url },
      });

      var $form = $cp_dialog.find('form');
      // set the login event handler
      $form.find('input[type=password]').change( function(evt) {
        var current = trim($form.find('input[id=current]').val());
        var newpwd = trim($form.find('input[id=newpwd]').val());
        var confirmpwd = trim($form.find('input[id=confirmpwd]').val());
        // should check that all files comply, then enable button
        if (current != null && current != '' &&
            newpwd != null && newpwd != '' &&
            confirmpwd != null && confirmpwd != '') {
          thisObj._changepwdDialog.eucadialog('enableButton', 'change-pwd');
        }
      });

      // about cloud dialog
      $tmpl = $('html body').find('.templates #aboutCloudDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_about)));
      var $dialog = $rendered.children().first();
      var $dialog_help = $rendered.children().last();
      this._aboutDialog = $dialog.eucadialog({
        id: 'about-cloud',
        title: about_dialog_title,
        buttons: {
          'cancel': { text: dialog_close_btn, focus:true, click: function() { $dialog.eucadialog("close"); } }
        },
        afterHelpFlipped : function() {
          $scrollable = thisObj._aboutDialog.find(".animated");
          $scrollable.css('overflow-y', 'hidden');
          $scrollable.stop();
          $scrollable.animate({scrollTop : 0}, 1);
          $scrollable.animate({scrollTop : $scrollable[0].scrollHeight}, 40*1000, undefined, function() {$scrollable.stop()});
          $scrollable.click( function() {
            $scrollable.stop();
            $scrollable.css('overflow-y', 'scroll');
          });
          return true;
        },
        beforeHelpFlipped : function() {
          thisObj._aboutDialog.eucadialog('setDialogOption','position', 'top');
        return true;
                          },
        help: { content: $dialog_help },
          help_icon_class : 'help-euca',
        });
        this._aboutDialog.find('#version').html($.eucaData.g_session['version']);
        var admin_url = $.eucaData.g_session['admin_console_url'];
        if(admin_url.indexOf('://localhost:') > 0)
          admin_url = admin_url.replace('localhost', location.hostname)
        this._aboutDialog.find('#admin-url').attr('href', admin_url);

      $(window).hashchange( function(){
        thisObj._windowsHashChanged();
      });
    },

    _destroy : function() {
                   },

    _windowsHashChanged : function() {
                          var hash = location.hash;
                          if (hash){
                              hash = hash.replace(/^#/, '');
                          }
                          if (this._curSelected !== hash && hash !== '')
                              this.updateSelected(hash);
                          iamBusy();
                      },

                      // event receiver for menu selection
    changeSelected : function (evt, ui) {
                     this.updateSelected(ui.selected, ui.filter, ui.options);
                 },

    updateSelected : function (selected, filter, options) {
                     var thisObj = this;
                     if(this._curSelected === selected){
                         return;
                     }

                     if(this._curSelected !== null){
                         var $curInstance = this.element.data(this._curSelected);
                         if($curInstance !== undefined && options !== KEEP_VIEW){
                             $curInstance.close();
                         }
                     }
                     var $container = $('html body').find(DOM_BINDING['main']);
                     if (options !== KEEP_VIEW)
                         $container.children().detach();
                     switch(selected){
                         case 'dashboard':
                             this.element.dashboard({select: function(evt, ui){$container.maincontainer("changeSelected", evt, ui)}});
                             break;
                         case 'instance':
                             this.element.instance({'state_filter': filter});
                             break;
                         case 'scaling':
                             this.element.scaling();
                             break;
                         case 'balancing':
                             this.element.balancing();
                             break;
                         case 'keypair':
                             this.element.keypair();
                             break;
                         case 'sgroup':
                             this.element.sgroup();
                             break;
                         case 'volume':
                             this.element.volume();
                             break;
                         case 'snapshot':
                             this.element.snapshot();
                             break;
                         case 'bucket':
                             this.element.bucket();
                             break;
                         case 'eip':
          this.element.eip();
          break;
        case 'launcher':
          var option = {};
          if(filter && filter['image'])
            option['image'] = filter['image']; 
          if(filter && filter['type'])
            option['type'] = filter['type'];
          if(filter && filter['security'])
            option['security'] = filter['security'];
          if(filter && filter['advanced'])
            option['advanced'] = filter['advanced'];
          this.element.launcher(option);
          break;
        case 'image':
          this.element.image();
          break;
        case 'logout':
          logout();
          break;
        case 'help':
          window.open($.eucaData.g_session['help_url'], '_blank');
          break;
        case 'changepwd':
          this._changepwdDialog.eucadialog("open");
          break;
        case 'aboutcloud':
          this._aboutDialog.eucadialog("open");
          break;
      }
      if (options !== KEEP_VIEW) {
        this._curSelected = selected;
        location.hash = selected;
      }
    },

    changePassword : function (current, newpwd){
      var tok = current+':'+newpwd;
      var hash = toBase64(tok);
	    $.ajax({
	      type:"POST",
 	      data:"action=changepwd&"+hash, 
        beforeSend: function (xhr) { 
          $main.find('#euca-main-container').append(
          $('<div>').addClass('spin-wheel').append( 
          $('<img>').attr('src','images/dots32.gif'))); // spinwheel
          $main.find('#euca-main-container').show();
        },
    	  dataType:"json",
	      async:"false",
	      success: function(out, textStatus, jqXHR) {
	        $.extend($.eucaData, {'g_session':out.global_session, 'u_session':out.user_session});
          eucalyptus.help({'language':out.global_session.language}); // loads help files
          args.onSuccess($.eucaData); // call back to login UI
          if (args.param.account.substring(args.param.account.length-13) == 'amazonaws.com') {
            IMG_OPT_PARAMS = '&Owner=self';
          }
        },
        error: function(jqXHR, textStatus, errorThrown){
          var $container = $('html body').find(DOM_BINDING['main']);
          $container.children().detach(); // remove spinwheel
	        args.onError(errorThrown);
        }
 	    });
    },

    clearSelected : function (){
      var $curInstance = this.element.data(this._curSelected);
      if($curInstance !== undefined){
        $curInstance.close();
      }
      this._curSelected = null;
    },

    getSelected : function () {
      return this._curSelected;
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
