(function($, eucalyptus) {
  eucalyptus.explorer = function(){ 
    $.widget("eucalyptus.explorer", {
      options : {
        menus : [ {key:'dashboard', text:menu_dashboard}, 
                {key:'images', text:menu_images},
                {key:'instances', text:menu_instances},
                {key:'storage', text:menu_storage},
                {key:'netsec', text:menu_netsec},
                {key:'support', text:menu_support}],
        submenus : {//dashboard: [{key:'dashboard', text:menu_dashboard_dashboard}],
                  //images: [{key:'images', text:menu_images_images}],
                  //instances: [{key:'instances', text:menu_instances_instances}],
                  storage: [{key:'volume', text:menu_storage_volumes}, {key:'snapshot', text:menu_storage_snapshots},{key:'bucket',text:menu_storage_buckets}],
                  netsec: [{key:'eip',text:menu_netsec_eip},{key:'sgroup',text:menu_netsec_sgroup},{key:'keypair',text:menu_netsec_keypair}],
                  support: [{key:'guide', text:menu_support_guide},{key:'forum', text: menu_support_forum},{key:'report', text: menu_support_report}]}
      },

      _init : function() { },

      _create : function() {
        var $ul = $('<ul>').addClass('resource-nav');
        for(i=0; i<this.options.menus.length; i++){
          $ul.append(this.createMenu(i, this.options.menus[i]));
        }
        this.element.append($ul);
      },

      createMenu : function (idx, menu) {
        var $submenu = $('<ul>');
        var header = this;
        if(menu.key in this.options.submenus){
          $.each(this.options.submenus[menu.key], function (idx, submenu){
            $submenu.append($('<li>').append(
                            $('<a>').attr('href','#').text(submenu.text).click(
                              function (evt){
                                header._trigger("select", evt, {selected:submenu.key}); 
                              })));
          });
        }
        var $menu = $('<li>').append(
                         $('<a>').addClass('lnk-'+menu.key.toLowerCase()).attr('href','#').text(menu.text));
        if(menu.key in this.options.submenus)
          $menu.append($submenu);
        else {
          $menu.find('a').click( 
            function (evt) {
              header._trigger("select", evt, {selected:menu.key}); 
            }
          );
        }
        return $menu;
      },
      _destroy : function() { } 
     }); // end of widget()
   } // end of eucalyptus.explorer()
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
