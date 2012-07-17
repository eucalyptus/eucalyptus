(function($, eucalyptus) {
  eucalyptus.login = function(args) {
  // select the login form
    var $login = $('html body').find('.templates #login').clone();
    $login.removeClass('templates inactive').addClass('templates active');
    $login.appendTo('html body');

    var $form = $login.find('form');
    // set the login event handler
    $form.find('input[type=submit]').click(function() {
      var param = { username:$form.find('input[name=username]').val(),
		    password:$form.find('input[name=password]').val() 
      };
      args.doLogin({ param: param,
		     onSuccess: function(){
   	             },
		     onError: function(){
			alert("login failed!");
		     }		     
      }); 
    });
  }
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});

//})(jQuery, eucalyptus);

