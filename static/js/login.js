(function($, eucalyptus) {
  eucalyptus.login = function(args) {
    // take the login form, add events, and put it to main container
    var $login = $('html body').find('.templates #login').clone();

    var $form = $login.find('form');
    // set the login event handler
    $form.find('input[name=account]').focus();
    $form.find('input[type=text]').change( function(evt) {
      if($(this).val() != null && $(this).val()!='')
          $form.find('input[name=login]').removeAttr('disabled');
    });
    $form.find('input[type=submit]').click(function() {
      var param = {
            account:$form.find('input[name=account]').val(),
            username:$form.find('input[name=username]').val(),
		    password:$form.find('input[name=password]').val() 
      };
      args.doLogin({ param: param,
        onSuccess: function(args){
          $login.remove();
          eucalyptus.main($.eucaData);
   	},
        onError: function(args){
    	     alert("login failed: "+args);
        }		     
      }); 
      return false;
    });
    $login.show();

    var $main = $('html body').find('.euca-main-outercontainer .inner-container');
    $login.appendTo($main);
    $('html body').find('.euca-container .euca-header').header({show_logo:true,show_navigator:false,show_user:false,show_help:false});
  }
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
