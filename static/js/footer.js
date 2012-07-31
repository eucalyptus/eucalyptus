(function($, eucalyptus) {
  /* jQuery widget factory */
  $.widget("eucalyptus.footer", {
     options: {
       home : $('<a>').attr('href', '/#').text('Home'),
       contact : $('<a>').attr('href','mailto:admin@example.com').text('Contact us'),
       licenses : null,
       privacy_policy: null,
       terms: null,
       logout: $('<a>').attr('href','/').text('Logout').click( function() {  
                  $.cookie('session-id','');
               })
     }, 
     _init: function () {
     },

     // jQuery widget method 
     _create: function () {
       var $footer = this.element;
       var $table = $('<table>').append('<tbody>').append('<tr>');
       $footer.append($table);
       this.refresh();
     },

     _setOption: function () {
     },     

     refresh: function () {
       var $tr = this.element.find('table tbody tr').remove('td');
       $.each(this.options, function (key, val) {
         if(key != null){
            $tr.append($('<td>').append(val));
         }
       });
       this.element.show(); 
     },

     // jQuery widget method 
     _destroy: function () {
       //
       this.element('table').remove();
       this.element.hide();
     }
   });
}(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {}));
