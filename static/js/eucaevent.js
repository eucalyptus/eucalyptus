(function($, eucalyptus) {
  $.widget('eucalyptus.eucaevent', {
    options : {
    },

    _clicked : {},

    _init : function(){
      var thisObj = this;
      this.element.click( function (evt, src) {
        $.each(thisObj._clicked, function (k, v){
           // $(evt.target).parent()[0]
            // e.g., src='navigator:storage:volumes', k = 'storage'
          if (!src || src.indexOf(k) == -1)
            $(v.target).trigger('click', ['triggered']);
        });
//        console.log('num events: '+Object.keys(thisObj._clicked).length);
      });
    },

    _create : function(){
    },

    _destroy : function() {
    },

    add_click : function(src, evtObj) {
      this._clicked[src] = evtObj;
    },

    del_click : function(src) {
      delete this._clicked[src];  
    },

    unclick_all : function(src) {
      $.each(this._clicked, function (k, v){
        $(v.target).trigger('click');
      });
    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
