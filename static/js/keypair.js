(function($, eucalyptus) {
  $.widget('eucalyptus.keypair', $.eucalyptus.eucawidget, {
    options : { },

    _init : function() {
      $('<p>').css('font-size','20px').css('text-align','center').text('keypair').appendTo(this.element);  
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    close: function() {
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
