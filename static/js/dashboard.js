(function($, eucalyptus) {
  $.widget('eucalyptus.dashboard', $.eucalyptus.eucawidget, {
    options : { },

    _init : function() {
    },

    _create : function() { 
      alert('create of dashboard');
      this.publish();
    },

    _destroy : function() {
    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
