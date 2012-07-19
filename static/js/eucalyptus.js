(function($, eucalyptus) {
  if (! $.eucaData){
	$.eucaData = {};
  }
  $(document).ready(function() {
  // check cookie
    if ($.cookie('session-id')) {
      eucalyptus.main($.eucaData);
    } else {
      eucalyptus.login({
        doLogin : function(args) {
	  $.ajax({
	    type:"POST",
 	    data:"username="+args.param.username+"&password="+args.param.password,
    	    dataType:"json",
	    async:"false",
	    success: function(data, textStatus, jqXHR){
	       $.extend($.eucaData, {context:data.context, text:data.text, image:data.image});
               args.onSuccess($.eucaData); // call back to login UI
            },
            error: function(jqXHR, textStatus, errorThrown){
	       args.onError();
            }
 	  });
        }
      });
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
