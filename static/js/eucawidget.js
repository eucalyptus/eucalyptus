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

    subscribe : function() {
      //alert('widget subscribe');
    },

    publish : function() {
      //alert('widget publish');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
