(function($, eucalyptus) {
  if (! $.eucaData){
	$.eucaData = {};
  }
  $(document).ready(function() {
    $.when( // this is to synchronize a chain of ajax calls 
      $.ajax({
        type:"POST",
        data:"action=lang",
        dataType:"json",
        async:"false", // async option deprecated as of jQuery 1.8
        success: function(out, textStatus, jqXHR){ 
          eucalyptus.i18n({'language':out.language});
        },
        error: function(jqXHR, textStatus, errorThrown){
          //TODO: should present error screen; can we use notification?
          alert("The server is not available");
          location.href='/';
        }
      })).done(function(out){
        // check cookie
        if ($.cookie('session-id')) {
          $.ajax({
  	    type:"POST",
	    data:"action=session&_xsrf="+$.cookie('_xsrf'),
	    dataType:"json",
	    async:"false",
	    success: function(out, textStatus, jqXHR){
	      $.extend($.eucaData, {'g_session':out.global_session, 'u_session':out.user_session});
              eucalyptus.main($.eucaData);
            },
	    error: function(jqXHR, textStatus, errorThrown){
              $.cookie('session-id','');
 	      location.href='/';	   
	    }
          });
        } else {
          var $main = $('html body').find('.euca-main-outercontainer .inner-container');
          $main.login({ doLogin : function(evt, args) {
              var tok = args.param.account+':'+args.param.username+':'+args.param.password;
              var hash = btoa(tok);
	      $.ajax({
	        type:"POST",
 	        data:"action=login", 
                beforeSend: function (xhr) { 
                   xhr.setRequestHeader('Authorization', 'Basic '+hash); 
                },
    	        dataType:"json",
	        async:"false",
	        success: function(out, textStatus, jqXHR) {
	          $.extend($.eucaData, {'g_session':out.global_session, 'u_session':out.user_session});
                  args.onSuccess($.eucaData); // call back to login UI
                },
                error: function(jqXHR, textStatus, errorThrown){
	          args.onError(errorThrown);
                }
 	     });
         }});
       } // end of else
    }); // end of done
  }); // end of document.ready
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
