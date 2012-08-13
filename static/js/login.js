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
