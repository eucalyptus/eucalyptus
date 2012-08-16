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
  eucalyptus.helps = function(args){
    language = args['language']; // not used yet
    help_keypair.load({language:language});
    help_volume.load({language:language});
    help_sgroup.load({language:language}); 
  }
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});

function loadHtml(url, handler){
    $.ajax(url, {
        type:"GET",
        data:"",
        dataType:"html",
        async:"false", // async option deprecated as of jQuery 1.8
        success: function (data){ 
          handler(data);
        },
        error : function (){
          handler("Error - cannot load the help page");
        }
    });
}
  //[keypair]
var help_keypair = {
  load : function(arg){
    loadHtml('helps/'+arg.language+'/describe_keypairs.html', function(data){help_keypair.landing_content=data})
    loadHtml('helps/'+arg.language+'/create_keypairs.html', function(data){help_keypair.dialog_add_content=data})
  },
  revert_button: "Back to key pair",
  landing_title: "Key pairs -- help",
  landing_content: "",
  dialog_add_title: "Creating new key pair?",
  dialog_add_content: "",
  dialog_delete_title: "Deleting key pair?",
  dialog_delete_content: ""
};

var help_volume = {
  load : function(arg){
    loadHtml('helps/'+arg.language+'/create_volumes.html', function(data){help_volume.landing_content=data})
  },
  revert_button: "Back to volumes",
  landing_title: "Volume -- help",
  landing_content: "",
  dialog_add_title: "Creating new volume?", 
  dialog_add_content: "",
  dialog_delete_title: "Deleting volumes", 
  dialog_delete_content: ""
};

var help_sgroup = {
  load : function(arg){
    ;
  },
  revert_button: "Back to security group",
  landing_title: "",
  landing_content: "",
  dialog_add_title: "",
  dialog_add_content: "",
  dialog_delete_title: "",
  dialog_delete_content: "",
}
