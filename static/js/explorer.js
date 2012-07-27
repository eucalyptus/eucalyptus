(function($, eucalyptus) {
  /*
     args = {idx, menu, imgUrl, submenu}
  */
  var createMenu = function(args) {
      var name = args.menu;
      var imgUrl = args.imgUrl;
      var $newitem = $('<div>').addClass('euca-container euca-explorer euca-explorer-items').attr('id','explorer-item-'+name).append(
	   $('<table>').append(
		$('<tr>').append(
		   $('<td>').append(
	   		$('<img>').attr('src', imgUrl).attr('id','explorer-item-'+name+'-img')),
		   $('<td>').text(name))));
      if (args.idx == 0){
          $newitem.css('clear','left');
          $newitem.css('margin-left','10px');
      }
      // create submenu
      if (args.submenu){
          // submenus should be split by ','
          var $table=$('<table>');
          $.each(args.submenu, function (idx, val){
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
      return $newitem;
  }

  eucalyptus.explorer= function(args) {
      var $container = args.container;
     //args = {idx, menu, imgUrl, submenu}
      //dashboard
      $container.append(
           createMenu({'idx':0, 'menu':menu_dashboard, 'imgUrl':'images/dashboard.png', 'submenu':[menu_dashboard_dashboard]}));
      $container.append(
           createMenu({'idx':1, 'menu':menu_images, 'imgUrl':'images/images.png', 'submenu':[menu_images_images]}));
      $container.append(
           createMenu({'idx':2, 'menu':menu_instances, 'imgUrl':'images/instances.png', 'submenu':[menu_instances_instances]}));
      $container.append(
           createMenu({'idx':3, 'menu':menu_storage, 'imgUrl':'images/storage.png', 'submenu':[menu_storage_volumes, menu_storage_snapshots, menu_storage_buckets]}));
      $container.append(
           createMenu({'idx':4, 'menu':menu_netsec, 'imgUrl':'images/netsec.png', 'submenu':[menu_netsec_eip, menu_netsec_sgroup, menu_netsec_keypair]}));
      $container.append(
           createMenu({'idx':5, 'menu':menu_support, 'imgUrl':'images/support.png', 'submenu':[menu_support_guide, menu_support_forum, menu_support_report]}));
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
