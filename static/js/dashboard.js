(function($, eucalyptus) {
  $.widget('eucalyptus.dashboard', $.eucalyptus.eucawidget, {
    options : { },

    _init : function() {
      $div = $('html body div.templates').find('#dashboard').clone();       
      $div.find('.dashboard .instances').append($('<p>').text('instances'));
      $div.find('.dashbooard .storage').append($('<p>').text('storage'));
      $div.find('.dashboard .netsec').append($('<p>').text('network & security'));
      $div.appendTo(this.element); 
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
