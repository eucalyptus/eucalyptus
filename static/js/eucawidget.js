/*
  This widget is the base (in the sense of oop) from which other eucalyptus widgets (dashboard, images, instances, etc) inherit. 
  Note that the inheritence using widget-factory isn't really the same as subclassing. If we need full-fledged inheritence, we should apply more complex patterns.
*/
(function($, eucalyptus) {
  $.widget('eucalyptus.eucawidget', {
    options : { },

    _init : function() {
    },

    _create : function() { 
    },

    _destroy : function() {
    },
    
    close : function() {
      this.element.children().detach();       
    },

    _help_flipped : false,

    _flipToHelp : function(evt, helpHeader, helpContent ) {
       var thisObj  = this;
       var $helpWrapper = $('<div>').addClass('help-page-wrapper');
       $helpWrapper.append(helpHeader, helpContent);

       thisObj.element.children().flip({
         direction : 'lr',
         speed : 300,
         color : '#ffffff',
         bgColor : '#ffffff',
         content : $helpWrapper,
         onEnd : function() {
            thisObj.element.find('.help-revert-button a').click( function(evt) {
              thisObj.element.children().revertFlip();
            });
            if(!thisObj._help_flipped){
               thisObj._help_flipped = true;
            }else{
               thisObj._help_flipped = false;
               var $container = $('html body').find(DOM_BINDING['main']);
               $container.maincontainer("clearSelected");
               $container.maincontainer("changeSelected",evt, {selected:thisObj.widgetName});
            }
          }
        });
    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
