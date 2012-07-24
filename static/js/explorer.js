(function($, eucalyptus) {
  eucalyptus.explorer= function(args) {
      var $container = args.container;
      var $subContainer = args.subcontainer;
      var name = args.item;
      // create div
      var imgUrl = $.eucaData.image['navi_'+name];
      var textDesc = $.eucaData.text['navi_'+name];
      var $newitem = $('<div>').addClass('euca-container euca-explorer euca-explorer-items').attr('id','explorer-item-'+name).append(
	   $('<table>').append(
		$('<tr>').append(
		   $('<td>').append(
	   		$('<img>').attr('src', imgUrl))),
		$('<tr>').append(
		   $('<td>').text(textDesc))));
      if (args.idx == 0){
          $newitem.css('clear','left');
          $newitem.css('margin-left','20px');
      }
      $container.append($newitem);
      // create submenu
      if ($.eucaData.context.explorers_sub[name] && $.eucaData.context.explorers_sub[name].length>0){
          var $table=$('<table>');
          $.each($.eucaData.context.explorers_sub[name], function(idx, val){
              $table.append($('<tr>').append($('<td>').text(val)));
          }); 
 
          // create div
          $sub_explorer = $('<div>').addClass('euca-container euca-explorer-sub').attr('id','explorer-subitem-'+name).append(
              $table);
          $subContainer.append($sub_explorer);
      }

      // event handlers
      $container.find('#explorer-item-'+name).hover(
 	 function () {
             $(this).addClass('mouseon');
             var pos = $(this).offset();
             var height = 80;
             var left = pos.left + 20;
             var top = pos.top + height;
             $sub = $subContainer.find('#explorer-subitem-'+name);
             $sub.css('left',left);
             $sub.css('top',top);
             $sub.css('display','block');
         }, 
         function () {
             $(this).removeClass('mouseon');
             $sub = $subContainer.find('#explorer-subitem-'+name);
             $sub.css('display','none');
         });

  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
