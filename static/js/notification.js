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
  $.widget('eucalyptus.notification', {
    options : {
    },
    _init : function() {
      var thisObj = this;
      // the 'x' box should be an image. text is added here for testing
      this.element.find('#euca-notification-close').find('a').remove();
      this.element.find('#euca-notification-close').append(
        $('<a>').attr('href','#').text('X').click( function(evt) {
          if(thisObj.element.hasClass('toggle-on')){
            thisObj.element.slideToggle('fast');
            thisObj.element.toggleClass('toggle-on');
          }
          return false;
        }));
    },

    _create : function() {},
    
    _destroy : function() {}, 

    success : function(title, desc) {
      this.notify({success:true, title:title, desc:desc});
    },

    error : function(title, desc, code) {
      this.notify({success:false, title:title, desc:desc, code:code});
    },

    /*
       args.success : boolean 
       args.title : title of the notification 
       args.desc : textual description of the notification
       args.code : error code (optional)
    */ 
    notify : function(args) {
      var thisObj = this;
      if(args.title){
        this.element.find('#euca-notification-title').html(args.title);
        this.element.find('#euca-notification-title').show();
      }
      else
        this.element.find('#euca-notification-title').hide();
      
      var desc = args.code ? args.desc + ' (code: '+args.code+')' : args.desc;
      if(args.desc){
        this.element.find('#euca-notification-desc').html(desc);
        this.element.find('#euca-notification-desc').show();
      }
      else
        this.element.find('#euca-notification-desc').hide();
      if(!this.element.hasClass('toggle-on')){ 
        this.element.slideToggle('fast');
        this.element.toggleClass('toggle-on');
      }
       
      if (args.success){
        this.element.removeClass('error-msg').addClass('success-msg');
        setTimeout(function(){ 
           //TODO: is this enough?; no unique ID necessary?
           if(thisObj.element.find('#euca-notification-desc').text() === args.desc)
             thisObj.element.find('#euca-notification-close a').trigger('click');
           else{
             var $title = thisObj.element.find('#euca-notification-title');
             if($title.text() === args.title)
               thisObj.element.find('#euca-notification-close a').trigger('click');
           }
        }, 5000);
      }else{
        this.element.removeClass('success-msg').addClass('error-msg');
      }
    },
   
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
