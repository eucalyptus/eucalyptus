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
    _options : { },
    errorDialog : null,
    _init : function() { },
    _create : function() { 
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #loginTmpl').clone(); 
      var $login = $($tmpl.render($.i18n.map));
      
      var  $tmpl = $('html body').find('.templates #loginErrorDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $err_dialog = $rendered.children().first();
      var $err_help = $rendered.children().last();
      this.errorDialog = $err_dialog.eucadialog({
        id: 'login-failure',
        title: login_failure_title,
        buttons: {
          'Close': {text: dialog_close_btn, focus:true, click: function() { $err_dialog.eucadialog("close");}}
        },
        help: {content: $err_help},
      });

      var $form = $login.find('form');
      // set the login event handler
      $form.find('input[type=text]').change( function(evt) {
        if($(this).val() != null && $(this).val()!='')
          $form.find('input[name=login]').removeAttr('disabled');
      });
      $form.find('input[type=submit]').click(function(evt) {
        var param = {
          account:$form.find('input[id=account]').val(),
          username:$form.find('input[id=username]').val(),
          password:$form.find('input[id=password]').val(),
          remember:$form.find('input[id=remember]').val() 
        };
        thisObj._trigger('doLogin', evt, { param: param,
          onSuccess: function(args){
            $login.remove();
            eucalyptus.main($.eucaData);
   	      },
          onError: function(args){
            thisObj.errorDialog.eucadialog('open');
          }		     
        });
        return false;
      });
      last_account = $.cookie('account');
      last_username = $.cookie('username');
      if (last_account != null) {
        $form.find('input[id=account]').val(last_account);
        $form.find('input[id=username]').val(last_username);
        $form.find('input[name=login]').removeAttr('disabled');
      }
      //rendered = $login.render($.i18n.map);
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
  });
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
