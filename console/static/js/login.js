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
  $.widget('eucalyptus.login', { 
    options : {
      support_url : ''
    },
    loginDialog : null,
    errorDialog : null,
    changepwdDialog : null,
    nocookiesDialog : null,
    _init : function() {
     },
    _create : function() { 
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #loginTmpl').clone(); 
      var $login = $($tmpl.render($.i18n.map));
      thisObj.loginDialog = $login;
      var $form = $login.find('form');

      // change password dialog
      $tmpl = $('html body').find('.templates #changePasswordTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_changepwd)));
      var $cp_dialog = $rendered.children().first();
      var $cp_dialog_help = $rendered.children().last();
      var $cp_form = $cp_dialog.find('form');
      this.changepwdDialog = $cp_dialog.eucadialog({
        id: 'change-passwd',
        title: login_change_passwd_title,
        buttons: {
          'change': { domid: 'change-pwd', text: login_change_passwd_submit, disabled: true, click: function() {
              var current = trim($cp_form.find('input[id=current]').val());
              var newpwd = trim($cp_form.find('input[id=newpwd]').val());
              var confirmpwd = trim($cp_form.find('input[id=confirmpwd]').val());

              var isValid = true;
              if (newpwd != confirmpwd) {
                isValid = false;
                thisObj.changepwdDialog.eucadialog('showError', login_change_passwd_dont_match);
              }
              
              if (isValid) {
                var account = $form.find('input[id=account]').val();
                var user = $form.find('input[id=username]').val();
                thisObj._changePassword(account, user, current, newpwd);
              }
              return false;
            }
          },
          'cancel': { text: dialog_cancel_btn, focus:false, click: function() { $cp_dialog.eucadialog("close"); } }
        },
        help: { content: $cp_dialog_help, url: help_changepwd.dialog_attach_content_url },
      });

      // set the login event handler
      $cp_form.find('input[type=password]').keyup( function(evt) {
        var current = trim($cp_form.find('input[id=current]').val());
        var newpwd = trim($cp_form.find('input[id=newpwd]').val());
        var confirmpwd = trim($cp_form.find('input[id=confirmpwd]').val());
        thisObj.changepwdDialog.eucadialog('showError', null);
        // should check that all files comply, then enable button
        if (current != null && current != '' &&
            newpwd != null && newpwd != '' &&
            confirmpwd != null && confirmpwd != '') {
          thisObj.changepwdDialog.eucadialog('enableButton', 'change-pwd');
        }
      });
      
      // login dialog
      var $tmpl = $('html body').find('.templates #loginErrorDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $err_dialog = $rendered.children().first();
      var $err_help = $rendered.children().last();
      this.errorDialog = $err_dialog.eucadialog({
        id: 'login-failure',
        title: login_failure_title,
        buttons: {
          'Close': {text: dialog_close_btn, focus:true, click: function() { $err_dialog.eucadialog("close");}}
        },
        help: {content: $err_help}
      });

      var $tmpl = $('html body').find('.templates #noCookiesDlgTmpl').clone();
      var $cookies_dialog = $($tmpl.render($.extend($.i18n.map)));

      if (navigator.cookieEnabled == false) {
        this.element.append($cookies_dialog);
        return;
      }
      // set the login event handler
      $form.find('input[type=text]').change( function(evt) {
        if($(this).val() != null && $(this).val()!='')
          $form.find('input[name=login]').removeAttr('disabled');
      });
      $form.find('input[type=submit]').click(function(evt) {
        $(this).attr('disabled','disabled');
        $(this).hide();
        $form.find('.button-bar').append(
          $('<img>').attr('id','login-spin-wheel').attr('src','images/dots32.gif'));

        var param = {
          account:trim($form.find('input[id=account]').val()),
          username:trim($form.find('input[id=username]').val()),
          password:trim($form.find('input[id=password]').val()),
          remember:$form.find('input[id=remember]').attr('checked') 
        };
       
        thisObj._trigger('doLogin', evt, { param: param,
          onSuccess: function(args){
            if ($.eucaData['u_session']['account'] === 'eucalyptus'){
              thisObj.popupWarning(login_account_warning, function(){ 
                var admin_url = $.eucaData.g_session['admin_console_url'];
                window.open(admin_url, '_blank');
              });
            }

            $login.remove();
            eucalyptus.main($.eucaData);
          },
          onError: function(args){
            $form.find('.button-bar img').remove();
            $form.find('.button-bar input').removeAttr('disabled');
            $form.find('.button-bar input').show();
            if (args.search("Forbidden")>-1) {
              thisObj.changepwdDialog.eucadialog("open");
              thisObj.changepwdDialog.find("#change-passwd-prompt p").html(login_change_passwd_prompt);
            }
            else {
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
                // normal login failure
                msgdiv.addClass('dialog-error').html(login_failure);
              }
            }
          }		     
        });
        return false;
      });
      last_account = $.cookie('account');
      last_username = $.cookie('username');
      last_remember = $.cookie('remember');
      if (last_account != null) {
        $form.find('input[id=account]').val(last_account);
        $form.find('input[id=username]').val(last_username);
        if (last_remember = 'true') {
            $form.find('input[id=remember]').attr('checked', '');
        }
        $form.find('input[name=login]').removeAttr('disabled');
      }
      // XSS Note:: No need to encode 'cloud_admin' since it's a static string from the file "messages.properties" - Kyo
      $login.find("#password-help").html($.i18n.prop('login_pwd_help', '<a href="#">'+cloud_admin+'</a>'));
      $login.find('#password-help a').click(function(e){
        if(thisObj.options.support_url.indexOf('mailto') >= 0)
          window.open(thisObj.options.support_url, '_self');
        else
          window.open(thisObj.options.support_url,'_blank');
      });
      this.element.append($login);
      $('html body').find('.euca-container .euca-header').header({show_logo:true,show_navigator:false,show_user:false,show_help:false});
      if (last_account == null) {
        $form.find('input[id=account]').focus();
      }
      else {
        $form.find('input[id=password]').focus();
      }
    },
    _destroy : function() { },

    _changePassword : function (acct, user, current, newpwd){
      var thisObj = this;
      var tok = acct +':'+ user +':'+ toBase64(current)+':'+toBase64(newpwd);
      var hash = toBase64(tok);
      $.ajax({
        type:"POST",
        data:"action=changepwd"+"&_xsrf="+$.cookie('_xsrf')+"&Authorization="+hash,
        dataType:"json",
        async:"false",
        success: function(out, textStatus, jqXHR) {
          thisObj.changepwdDialog.eucadialog("close");
	        $.extend($.eucaData, {'g_session':out.global_session, 'u_session':out.user_session});
          eucalyptus.help({'language':out.global_session.language}); // loads help files
          thisObj.loginDialog.remove();
          eucalyptus.main($.eucaData);
        },
        error: function(jqXHR, textStatus, errorThrown){
          thisObj.changepwdDialog.eucadialog("close");
          thisObj.errorDialog.eucadialog('open');
          msgdiv.addClass('dialog-error').html($.i18n.prop('login_change_passwd_error', errorThrown));
        }
 	    });
    },

  /////// PUBLIC METHODS //////
    popupError : function(bError, msg, callback) {
     var thisObj = this;
     thisObj.errorDialog.eucadialog('open');
     if(callback){
       thisObj.errorDialog.data('eucadialog').option('on_close', {callback: callback});
     }
     var msgdiv = thisObj.errorDialog.find("#login-error-message p")
     if(typeof bError != undefined && bError)
       msgdiv.addClass('dialog-error');
     msgdiv.html(msg);
     thisObj.errorDialog.find('#login-error-message a').click(function(e){
       if(thisObj.options.support_url.indexOf('mailto') >= 0)
         window.open(thisObj.options.support_url, '_self');
       else
         window.open(thisObj.options.support_url,'_blank');
     });
   },

   popupWarning : function(msg, onClick){
     var thisObj = this;
     thisObj.errorDialog.eucadialog('open');
     var msgdiv = thisObj.errorDialog.find("#login-error-message p"); 
     thisObj.errorDialog.eucadialog('option', 'title', login_warning_title);
     thisObj.errorDialog.parent().find('span.ui-button-text').text(dialog_continue_btn);
     msgdiv.html(msg);
     if(onClick){
       thisObj.errorDialog.find('#login-error-message a').click(onClick);
     }
   },

   showPasswordChange : function () {
     var thisObj = this;
     thisObj.changepwdDialog.eucadialog('open');
   }
  });
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
