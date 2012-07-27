(function($, eucalyptus) {
  if (! $.eucaData){
	$.eucaData = {};
  }
  $(document).ready(function() {
    // check language and retrieve i18n customization
    $.ajax({
      type:"POST",
      data:"action=lang",
      dataType:"json",
      async:"false",
      success: function(out, textStatus, jqXHR){ 
        eucalyptus.i18n({'language':out.language});
      },
      error: function(jqXHR, textStatus, errorThrown){
        alert("The server is not available");
        location.href='/';
      }
    });
 
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
      eucalyptus.login({
        doLogin : function(args) {
          var tok = args.param.username+':'+args.param.password;
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
        }
      });
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
