(function($, eucalyptus) {
  eucalyptus.explorer= function(args) {
      var $container = args.container;
      var name = args.item;
      // create div
      var imgUrl = $.eucaData.image['navi_'+name];
      var textDesc = $.eucaData.text['navi_'+name];
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
      if ($.eucaData.context.explorers_sub[name] && $.eucaData.context.explorers_sub[name].length>0){
          var $table=$('<table>');
          $.each($.eucaData.context.explorers_sub[name], function(idx, val){
              var $td = $('<td>').append($('<a>').attr('href','#').text(val));
              $td.hover(
  	          function () {
                     $(this).addClass('euca-explorer-sub mouseon'); //css('color','#86B237');
                  },
                  function () {
                     $(this).removeClass('euca-explorer-sub mouseon'); // .css('color','#444444');
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
