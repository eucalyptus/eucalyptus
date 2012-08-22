/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
    loadHtml('helps/'+arg.language+'/console_create_keypair.html', function(data){help_keypair.dialog_add_content=data})
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
    loadHtml('helps/'+arg.language+'/console_create_volume.html', function(data){help_volume.landing_content=data})
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
    loadHtml('helps/'+arg.language+'/console_create_security_group.html', function(data){help_volume.landing_content=data})
  },
  revert_button: "Back to security group",
  landing_title: "",
  landing_content: "",
  dialog_add_title: "",
  dialog_add_content: "",
  dialog_delete_title: "",
  dialog_delete_content: "",
}
