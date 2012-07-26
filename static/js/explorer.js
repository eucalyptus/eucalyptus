(function($, eucalyptus) {
  eucalyptus.explorer= function(args) {
      var $container = args.container;
      var name = args.item;
      // create div
      var imgUrl = $.eucaData.g_session.images[name];
      var textDesc = name
      var $newitem = $('<div>').addClass('euca-container euca-explorer euca-explorer-items').attr('id','explorer-item-'+name).append(
	   $('<table>').append(
		$('<tr>').append(
		   $('<td>').append(
	   		$('<img>').attr('src', imgUrl).attr('id','explorer-item-'+name+'-img')),
		   $('<td>').text(textDesc))));
      if (args.idx == 0){
          $newitem.css('clear','left');
          $newitem.css('margin-left','10px');
      }
      // create submenu
      if ($.eucaData.g_session.navigation_submenus[name] && $.eucaData.g_session.navigation_submenus[name].length>0){
          // submenus should be split by ','
          var $table=$('<table>');
          $.each($.eucaData.g_session.navigation_submenus[name].split(","), function(idx, val){
              var $td = $('<td>').append($('<a>').attr('href','#').text(val));
              $td.hover(
  	          function () {
                     $(this).addClass('euca-explorer-sub mouseon'); 
                  },
                  function () {
                     $(this).removeClass('euca-explorer-sub mouseon'); 
                  });
              $table.append($('<tr>').append($td));
          }); 
 
          // create div
          $sub_explorer = $('<div>').addClass('euca-container euca-explorer-sub').attr('id','explorer-subitem-'+name).append(
              $table);
          $newitem.append($sub_explorer);
      }
      $container.append($newitem) ;
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
