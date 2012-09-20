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
  eucalyptus.help = function(args){
    language = args['language']; // not used yet
    help_keypair.load({language:language});
    help_volume.load({language:language});
    help_sgroup.load({language:language}); 
    help_instance.load({language:language});
    help_snapshot.load({language:language});
    help_image.load({language:language}); 
    help_eip.load({language:language});
    help_launcher.load({language:language});
    help_dashboard.load({language:language});
  }
})(jQuery, 
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});

function getLandingHelpHeader(args){
  var $helpHeader = $('<div>').addClass('euca-table-header').append(
                     $('<div>').addClass('help-link').append(
                       $('<a>').attr('href','#').html('&larr;')));
  return $helpHeader;
}

function loadHtml(url, handler){
    $.ajax(url, {
        type:"GET",
        data:"",
        dataType:"html",
        async:false, // async option deprecated as of jQuery 1.8
        success: function (data){ 
          handler(data);
        },
        error : function (){
          handler("Error - cannot load the help page");
        }
    });
}

var help_dashboard = {
   load : function(arg){
    loadHtml('help/'+arg.language+'/console_dashboard.html', function(data){help_dashboard.landing_content=data})
   },
   landing_content: "",
}
  //[keypair]
var help_keypair = {
  load : function(arg){
    loadHtml('help/'+arg.language+'/console_manage_keypairs.html', function(data){help_keypair.landing_content=data})
    loadHtml('help/'+arg.language+'/console_create_keypair.html', function(data){help_keypair.dialog_add_content=data})
    loadHtml('help/'+arg.language+'/console_delete_keypair.html', function(data){help_keypair.dialog_delete_content=data})
  },
  landing_content: "",
  dialog_add_content: "",
  dialog_delete_content: ""
};

var help_image = {
  load : function(arg){
    loadHtml('help/'+arg.language+'/console_manage_images.html', function(data){help_image.landing_content=data})
  },
  landing_content: "",
};

var help_snapshot = {
  load : function(arg){
    loadHtml('help/'+arg.language+'/console_manage_snapshots.html', function(data){help_snapshot.landing_content=data})
    loadHtml('help/'+arg.language+'/console_create_snapshot.html', function(data){help_snapshot.dialog_create_content=data})
    loadHtml('help/'+arg.language+'/console_delete_snapshot.html', function(data){help_snapshot.dialog_delete_content=data})
  },
  landing_content: "",
  dialog_delete_content: "",
  dialog_create_content: ""
};

var help_eip = {
  load : function(arg){
    loadHtml('help/'+arg.language+'/console_manage_eips.html', function(data){help_eip.landing_content=data})
    loadHtml('help/'+arg.language+'/console_allocate_eip.html', function(data){help_eip.dialog_allocate_content=data})
    loadHtml('help/'+arg.language+'/console_release_eip.html', function(data){help_eip.dialog_release_content=data})
    loadHtml('help/'+arg.language+'/console_associate_eip.html', function(data){help_eip.dialog_associate_content=data})
    loadHtml('help/'+arg.language+'/console_disassociate_eip.html', function(data){help_eip.dialog_disassociate_content=data})
  },
  landing_content: "",
  dialog_release_content: "",
  dialog_allocate_content: "",
  dialog_associate_content: "",
  dialog_disassociate_content: ""
};

var help_volume = {
  load : function(arg){
    loadHtml('help/'+arg.language+'/console_manage_volumes.html', function(data){help_volume.landing_content=data})
    loadHtml('help/'+arg.language+'/console_create_volume.html', function(data){help_volume.dialog_add_content=data})
  },
  landing_content: "",
  dialog_add_content: "",
  dialog_delete_content: "",
};

var help_sgroup = {
  load : function(arg){
    loadHtml('help/'+arg.language+'/console_manage_security_groups.html', function(data){help_sgroup.landing_content=data})
    loadHtml('help/'+arg.language+'/console_create_security_group.html', function(data){help_sgroup.dialog_add_content=data})
    loadHtml('help/'+arg.language+'/console_edit_security_group.html', function(data){help_sgroup.dialog_edit_content=data})
    loadHtml('help/'+arg.language+'/console_delete_security_group.html', function(data){help_sgroup.dialog_delete_content=data})
  },
  landing_content: "",
  dialog_add_content: "",
  dialog_edit_content: "",
  dialog_delete_content: "",
}

var help_instance = {
  load : function(arg){
    loadHtml('help/'+arg.language+'/console_manage_instances.html', function(data){help_instance.landing_content=data})
  },
  landing_content: "",
  dialog_add_content: "",
  dialog_delete_content: "",
  dialog_reboot_content: "",
  dialog_start_content: "",
  dialog_stop_content: "",
  dialog_connect_content: "",
  dialog_console_content: "",
  dialog_password_content: "",
}

var help_launcher = {
  load : function(arg) {
    loadHtml('help/'+arg.language+'/console_create_instances.html', function(data){help_launcher.landing_content=data})
  },
  landing_content: "", 
}
