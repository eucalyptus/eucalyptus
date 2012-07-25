(function($, eucalyptus) {
    eucalyptus.footer= function(args) {
       var $footer = $('html body').find('.euca-container .euca-footer');
       $footer.css('display','block'); 
       var $table = $('<table>').append('<tbody>').append('<tr>');
       $.each(args, function (idx, val) {
           $('<td>').append(val).appendTo($table);
       });
 
       /*$table.append(
            $('<tr>').append(
		$('<td>').append(
		   $('<p>').text(textData['footer'])),
		$('<td>').append(
       		   $('<p>').html('&nbsp;&nbsp;&nbsp;&nbsp;<a id=\'logout-button\' href=\'/\'>logout</a>'))));
       */
       
       $table.appendTo($footer);
       // logout
       $footer.find('#logout-button').click(function(){
	   $.cookie('session-id','');
       });
    }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
