(function($, eucalyptus) {
  if (! $.eucaData){
	$.eucaData = {};
  }
  $(document).ready(function() {
  // check cookie
    if ($.cookie('session-id')) {
       $.ajax({
  	  type:"POST",
	  data:"action=session&_xsrf="+$.cookie('_xsrf'),
	  dataType:"json",
	  async:"false",
	  success: function(out, textStatus, jqXHR){
	      $.extend($.eucaData, {context:out.data.context, text:out.data.text, image:out.data.image});
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
	       $.extend($.eucaData, {context:out.data.context, text:out.data.text, image:out.data.image});
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
