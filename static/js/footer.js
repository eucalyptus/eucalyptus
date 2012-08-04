(function($, eucalyptus) {
  /* jQuery widget factory */
  $.widget("eucalyptus.footer", {
     options: { 
       footer_items: {
         home : $('<a>').attr('href', '/#').text('Home'),
         contact : $('<a>').attr('href','mailto:admin@example.com').text('Contact us'),
         licenses : null,
         privacy_policy: null,
         terms: null,
         logout: $('<a>').attr('href','/').text('Logout').click( function() {  
                  $.cookie('session-id','');
               })
       }
     }, 
     _init: function () {
       $ul = this.element.find('ul').addClass('footer-nav');
       $.each(this.options.footer_items, function (key,val) { 
         if(val !== null)
           $ul.append($('<li>').append(val)); 
       });
       this.element.show(); 
     },

     // jQuery widget method 
     _create: function () {
       this.element.append($('<ul>'));
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
