(function($, eucalyptus) {
  /* jQuery widget factory */
  $.widget("eucalyptus.footer", {
     options: { } , 
     _init: function () {
       var $tmpl = $('html body').find('.templates #footerTmpl');
       var $wrapper = $($tmpl.render($.i18n.map));
       this.element.append($wrapper);
       this.element.show();
     },

     // jQuery widget method 
     _create: function () {
     },

     _setOption: function () {
     },     

     // jQuery widget method 
     _destroy: function () {
       //
       this.element.hide();
     }
   });
}(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {}));
