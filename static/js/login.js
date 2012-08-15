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
  $.widget('eucalyptus.login', { 
    _options : { },
    _init : function() { },
    _create : function() { 
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #loginTmpl').clone(); 
      var $login = $($tmpl.render($.i18n.map));
      
      var $form = $login.find('form');
      // set the login event handler
      $form.find('input[name=account]').focus();
      $form.find('input[type=text]').change( function(evt) {
        if($(this).val() != null && $(this).val()!='')
          $form.find('input[name=login]').removeAttr('disabled');
      });
      $form.find('input[type=submit]').click(function(evt) {
        var param = {
          account:$form.find('input[id=account]').val(),
          username:$form.find('input[id=username]').val(),
          password:$form.find('input[id=password]').val() 
        };
        thisObj._trigger('doLogin', evt, { param: param,
          onSuccess: function(args){
            $login.remove();
            eucalyptus.main($.eucaData);
   	  },
          onError: function(args){
             // TODO: need an error notification screen for login failure
    	     alert("login failed: "+args);
          }		     
        });
        return false;
      });
      //rendered = $login.render($.i18n.map);
      this.element.append($login);
      $('html body').find('.euca-container .euca-header').header({show_logo:true,show_navigator:false,show_user:false,show_help:false});
    },
    _destroy : function() { },
  });
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
