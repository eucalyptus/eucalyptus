(function($, eucalyptus) {
  eucalyptus.login = function(args) {
  // select the login form
    var $login = $('html body').find('.templates #login').clone();
    $login.removeClass('templates inactive').addClass('templates active');
    $login.appendTo('html body');

    var $form = $login.find('form');
    // set the login event handler
    $form.find('input[name=account]').focus();
    $form.find('input[name=account]').onChange = function($form) {
        eucalyptus.login.enable($form)
    };
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
  }
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});

eucalyptus.login.enable = function(form) {
  var account = form.find('input[name=account]').val();
  var name = form.find('input[name=name]').val();
  var passwd = form.find('input[name=passwd]').val();
  var login = form.find('input[type=submit]');
  if ((account==null || account=="") && (name==null || name=="") && (passwd==null || passwd==""))
    login.disabled="1";
  else
    delete login.disabled;

}
