(function($, eucalyptus) {
  $.widget('eucalyptus.notification', {
    options : {
    },
    _init : function() {
      var thisObj = this;
      // the 'x' box should be an image. text is added here for testing
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
        this.element.find('#euca-notification-title').text(args.title);
        this.element.find('#euca-notification-title').show();
      }
      else
        this.element.find('#euca-notification-title').hide();
      
      var desc = args.code ? args.desc + ' (code: '+args.code+')' : args.desc;
      if(args.desc){
        this.element.find('#euca-notification-desc').text(desc);
        this.element.find('#euca-notification-desc').show();
      }
      else
        this.element.find('#euca-notification-desc').hide();
      if(!this.element.hasClass('toggle-on')){ 
        this.element.slideToggle('fast');
        this.element.toggleClass('toggle-on');
      }
       
      if (args.success){
        setTimeout(function(){ 
           //TODO: is this enough?; no unique ID necessary?
           if(thisObj.element.find('#euca-notification-title').text() === args.title)
             thisObj.element.find('#euca-notification-close a').trigger('click');
        }, 3000);
      }
    },
   
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
