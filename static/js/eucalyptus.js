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
          eucalyptus.helps({'language':out.language}); // loads help files
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
