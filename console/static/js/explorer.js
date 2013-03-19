/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

(function($, eucalyptus) {
  eucalyptus.explorer = function(){ 
    $.widget("eucalyptus.explorer", {
      options : {
        menus : [ {key:'dashboard', text:menu_dashboard}, 
                {key:'image', text:images_label},
                {key:'instance', text:instances_label},
                {key:'storage', text:storage_label},
                {key:'netsec', text:netsec_label},
                {key:'monitoring', text:monitoring_label}],
		submenus : { storage: [{key:'volume', text:volumes_label}, {key:'snapshot', text:snapshots_label}, {key:'bucket', text:buckets_label}, {key:'jp_volume', text:'JP Volumes'}],
                  netsec: [{key:'balancing', text:balancers_label}, {key:'sgroup',text:sgroups_label},{key:'keypair',text:keypairs_label},{key:'eip',text:eips_label}],
                  instance: [{key:'instance', text:instances_label}, {key:'scaling', text:scaling_label}, {key:'eantest', text: 'Ean Test'}, {key:'rivetstest', text:'Rivets Test'}]},
      },

      _init : function() { },

      _create : function() {
        // resources-explorer
        var $ul = $('<ul>').addClass('resources-nav');
        for(i=0; i<this.options.menus.length; i++){
          $ul.append(this.createResourceMenu(i, this.options.menus[i]));
        }
        this.element.append($ul);
 
      },

      createUserMenu : function () {
        user_menu = { preference : menu_user_preferences,
                      logout : menu_user_logout,
        }
        var arr = [];
        var header = this;
        $.each(user_menu, function (key, val) {
          $.extend(arr, $('<li>').append(
             $('<a>').attr('href','#').text(val).click(function(evt) {
               header._trigger("select", evt, {selected:key});
             })));
        });
        return arr;
      },

      createResourceMenu : function (idx, menu) {
        var thisObj = this;
        var $submenu = $('<ul>');
        var header = this;
        if(menu.key in this.options.submenus){
          $.each(this.options.submenus[menu.key], function (idx, submenu){
            $submenu.append($('<li>').append(
                            $('<a>').attr('href','#').text(submenu.text).click(
                              function (evt, src){
                                if(src!=='triggered')
                                  //location.hash = submenu.key;
                                  header._trigger("select", evt, {selected:submenu.key}); 
                              })));
          });
        }
        var clsName = 'lnk-'+menu.key.toLowerCase();
        var $menu = $('<li>').append(
                         $('<a>').addClass(clsName).attr('href','#').text(menu.text));
        if(menu.key in this.options.submenus){
          $menu.append($submenu);
          $menu.find('a').click(function(evt, src) {
            $submenu.slideToggle('fast');
            $menu.toggleClass('toggle-on'); 
            $('html body').trigger('click','navigator:'+menu.key);
            if ($menu.hasClass('toggle-on'))
              $('html body').eucaevent('add_click', 'navigator:'+menu.key, evt);
            else
              $('html body').eucaevent('del_click', 'navigator:'+menu.key);
            return false;
          });
        }
        else {
          $menu.find('a').click( 
            function (evt, src) {
              if(src!=='triggered') {
                //  location.hash = menu.key;
                header._trigger("select", evt, {selected:menu.key});
                $('html body').trigger('click', 'navigator:'+menu.key);
                return false;
              }
            }
          );
        }
        return $menu;
      },
      _destroy : function() { },

      // called when the explorer is slide down
      onSlide : function() { 
        // make sure no menus has 'toggle-on' class set
        this.element.find('.toggle-on').each( function() {
          $(this).removeClass('toggle-on');   
        }); 
 
        this.element.find('.resources-nav').each(function() {
          $(this).find('li ul').hide();
        });
      },
     }); // end of widget()
   } // end of eucalyptus.explorer()
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
