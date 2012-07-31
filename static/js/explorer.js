(function($, eucalyptus) {
  eucalyptus.explorer = function(){ 
    $.widget("eucalyptus.explorer", {
      options : {
        menus : [ {key:'dashboard', text:menu_dashboard, imgUrl:'images/dashboard.png'}, 
                {key:'images', text:menu_images, imgUrl:'images/images.png'},
                {key:'instances', text:menu_instances, imgUrl:'images/instances.png'},
                {key:'storage', text:menu_storage, imgUrl:'images/storage.png'},
                {key:'netsec', text:menu_netsec, imgUrl:'images/netsec.png'},
                {key:'support', text:menu_support, imgUrl:'images/support.png'}],
        submenus : {dashboard: [{key:'dashboard', text:menu_dashboard_dashboard}],
                  images: [{key:'images', text:menu_images_images}],
                  instances: [{key:'instances', text:menu_instances_instances}],
                  storage: [{key:'volume', text:menu_storage_volumes}, {key:'snapshot', text:menu_storage_snapshots},{key:'bucket',text:menu_storage_buckets}],
                  netsec: [{key:'eip',text:menu_netsec_eip},{key:'sgroup',text:menu_netsec_sgroup},{key:'keypair',text:menu_netsec_keypair}],
                  support: [{key:'guide', text:menu_support_guide},{key:'forum', text: menu_support_forum},{key:'report', text: menu_support_report}]}
      },

      _init : function() { },

      _create : function() {
        for(i=0; i<this.options.menus.length; i++){
          this.element.append(this.createMenu(i, this.options.menus[i]));
        }
      },

      /*
       args = {idx, menu, imgUrl, submenu}
      */
      createMenu : function (idx, menu) {
        var $newitem = $('<div>').addClass('euca-container euca-explorer euca-explorer-items').attr('id','explorer-item-'+menu.text).append(
  	    $('<table>').append(
		$('<tr>').append(
		   $('<td>').append(
	   		$('<img>').attr('src', menu.imgUrl).attr('id','explorer-item-'+menu.text+'-img')),
		   $('<td>').text(menu.text))));
        if (idx == 0){
          $newitem.css('clear','left');
          $newitem.css('margin-left','10px');
        }
        // create submenu
        if (menu.key in this.options.submenus){
          // submenus should be split by ','
          var $table=$('<table>');
          var header = this;
          $.each(this.options.submenus[menu.key], function (idx, submenu){
              var $td = $('<td>').append($('<a>').attr('href','#').click(
                function(evtObj){ 
                  header._trigger("select", evtObj, {selected:submenu.key});
                }
              ).text(submenu.text));
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
          $sub_explorer = $('<div>').addClass('euca-container euca-explorer-sub').attr('id','explorer-subitem-'+menu.text).append(
              $table);
          $newitem.append($sub_explorer);
        }
        return $newitem;
      },
      _destroy : function() { } 
     }); // end of widget()
   } // end of eucalyptus.explorer()
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
