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
       args.percent : percentage of the progress
       args.desc : message after progress (e.g., 95/100 volumes deleted. Failed to delete 5 vollumes)
       args.error: : [ {id:.., reason:.., code: }, {id:.., reason: .., code: } ]
    */
    multi : function(args) {
       var thisObj = this;
       var percent = args.percent;
       var desc = args.desc;
       var error = args.error;

      //  if (id !== thisObj.element.attr('progress-id'))
      //   return; 

       percent = Math.min(100, percent);
       percent = Math.max(0, percent);
       thisObj.element.find('#euca-notification-progress').progressbar({value: percent});
       thisObj.element.find('#euca-notification-progress').show();
       if(desc)
         thisObj.element.find('#euca-notification-desc').html(desc);

       // check if the notification is in progress-mode and the id is for this progress report
         // toggle and show the progress
       if( !this.element.hasClass('toggle-on') && !thisObj.element.hasClass('in-progress') ){ 
         this.element.slideToggle('fast');
         this.element.toggleClass('toggle-on');
       }

       this.element.removeClass('success-msg');
       this.element.removeClass('error-msg');
       // set the percentage
       if( percent === 100){
         // now set the description and error
         thisObj.element.find('#euca-notification-desc a').click(function(e) {
           if(thisObj.element.find('#euca-notification-desc .euca-notification-error').length <=0){
             var errorMsg = '';
             $.each(error, function(idx, err){
               if(!err.code)
                 errorMsg += err.id+': '+err.reason+'\n';
               else
                 errorMsg += err.id+': '+err.reason+'('+err.code+')\n';
             });
             thisObj.element.find('#euca-notification-desc').append(
               $('<div>').addClass('euca-notification-error').append(
                 $('<textarea>').attr('id','euca-notification-error-list').attr('readonly','true').html(errorMsg)));
           }else
             thisObj.element.find('#euca-notification-desc .euca-notification-error').detach();
         });
         thisObj.element.removeClass('in-progress');
         if(error && error.length > 0)
           this.element.removeClass('success-msg').addClass('error-msg');
         else{
           this.element.removeClass('error-msg').addClass('success-msg');
           setTimeout(function(){ 
             thisObj.element.find('#euca-notification-close a').trigger('click');
           }, 5000);
         }
         // check if the notification is in progress-mode and the id is for this progress report
         // toggle and show the progress
         if( !this.element.hasClass('toggle-on') ){
           this.element.slideToggle('fast');
           this.element.toggleClass('toggle-on');
         }
       }else{
         if( !this.element.hasClass('toggle-on')  && !thisObj.element.hasClass('in-progress') ){ 
           this.element.slideToggle('fast');
           this.element.toggleClass('toggle-on');
         }
         thisObj.element.addClass('in-progress');
       }  
    },

    /*
       args.success : boolean 
       args.title : title of the notification 
       args.desc : textual description of the notification
       args.code : error code (optional)
    */ 
    notify : function(args) {
      var thisObj = this;
      // override notifyMulti if invoked
      thisObj.element.removeClass('in-progress');
      thisObj.element.find('#euca-notification-progress').hide();

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
