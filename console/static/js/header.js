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
  $.widget('eucalyptus.header', {
    options : {
       logoUrl : 'images/eucalyptus_top.png',
       show_logo : true,
       show_navigation : false,
       show_help : false,
       show_user : false,
       show_search : false
    },
    _init : function(){
        var widget = this;
        this.element.show();
        this.element.children().each(function(idx) {
          if ($(this).attr('id')==='euca-navigator'){
            if(widget.options.show_navigation)
              $(this).show();
            else
              $(this).hide();
          }
          if ($(this).attr('id')==='euca-user'){
            if(widget.options.show_user){
              $(this).show();
            }
            else
              $(this).hide();
          }
          if ($(this).attr('id')==='euca-help'){
            if(widget.options.show_help){
              $(this).show();
            }
            else
              $(this).hide();
          }
          if ($(this).attr('id')=='euca-search'){
            if(widget.options.show_search)
              $(this).show(); 
            else
              $(this).hide();
          }
          if ($(this).attr('id')=='euca-regions'){
            if(widget.options.show_regions)
              $(this).show(); 
            else
              $(this).hide();
          }
        });
    },

    _create : function(){
       var thisObj = this;
       require(['app'], function(app) {
         thisObj.element.find('a#euca-logo').click(function(e){
           location.hash = '';
           location.reload();
         });

         // regions area
         // TODO, currently not fetching regions when it should. Should key off
         // an aws login, not aws login enablement
         if(app.aws.aws_login_enabled) {
           console.log("fetching regions");
           app.data.regions.fetch({merge: true, add: true, remove: true,
             success: function(col, resp, options) {
               var regions = {};
               $.each(resp, function(idx) {
                 console.log("region: "+JSON.stringify(resp[idx]));
                 var name = resp[idx].name;
                 var key = name.replace('-', '_').replace('-', '_');
                 regions[name] = {text:app.msg('region_'+key+'_name')+'('+app.msg('region_'+key+'_loc')+')',
                                  value:name,
                                  endpoint:resp[idx].endpoint};
               });
               var $reg_menus = $('<ul>');
               $.each(regions, function(k, v){
                 $('<li>').append(
                   $('<a>').attr('href','#').text(v.text).click(function(e,src){
                     if(src!=='triggered')
                       thisObj._trigger('select',e, {selected:k, options:v.options});
                    })).appendTo($reg_menus);
               });
               var $regArea = thisObj.element.find('#euca-regions');
               var regSelected = app.aws.region.replace('-', '_').replace('-', '_');
               var menu_title = app.msg('region_'+regSelected+'_loc');
               $regArea.append(
                 $('<ul>').addClass('header-nav').addClass(' header-nav-region').append(
                   $('<li>').append(
                     $('<a>').attr('href','#').text(menu_title).click(function(evt, src){
                       $regArea.find('.header-nav ul').slideToggle('fast');
                       $(thisObj).toggleClass('toggle-on');
                       $('html body').trigger('click', 'reg');
                       if ($(thisObj).hasClass('toggle-on'))
                         $('html body').eucaevent('add_click', 'reg', evt);
                       else
                         $('html body').eucaevent('del_click', 'reg');
                       return false;
                     }),
                     $reg_menus)));
             }
           });
         }

         //user area
         var user_menus = {'help':{text:menu_help, options:KEEP_VIEW},
                           'changepwd':{text:menu_user_changepwd, options:KEEP_VIEW},
                           'aboutcloud':{text:menu_user_aboutcloud, options:KEEP_VIEW},
                           'logout':{text:menu_user_logout}}

         var uname = $.eucaData.u_session['username'] + '@' + $.eucaData.u_session['account'];
         if ($.eucaData.u_session['fullname']) {
           uname = $.eucaData.u_session['fullname'];
         }
         var $userArea = thisObj.element.find('#euca-user');
        
         var $user_menus = $('<ul>');
         $.each(user_menus, function(k, v){
           $('<li>').append(
             $('<a>').attr('href','#').text(v.text).click(function(e,src){
               if(src!=='triggered')
                 thisObj._trigger('select',e, {selected:k, options:v.options});
              })).appendTo($user_menus);
         });
   
         $userArea.append(
           $('<ul>').addClass('header-nav').append(
             $('<li>').append(
               $('<a>').attr('href','#').text(uname).click(function(evt, src){
                 $userArea.find('.header-nav ul').slideToggle('fast');
                 $(thisObj).toggleClass('toggle-on');
                 $('html body').trigger('click', 'user');
                 if ($(thisObj).hasClass('toggle-on'))
                   $('html body').eucaevent('add_click', 'user', evt);
                 else
                   $('html body').eucaevent('del_click', 'user');
                 return false;
               }),
               $user_menus)));

          // event handlers
          var $navigator = $('#euca-navigator');
          $navigator.click(function (evt, src){
            $('#euca-explorer').slideToggle('fast'); 
            $navigator.toggleClass('toggle-on');
            $('html body').trigger('click','navigator');
            if ($navigator.hasClass('toggle-on')){
              $('html body').find('.euca-explorer-container .inner-container').explorer('onSlide');
              $('html body').eucaevent('add_click', 'navigator', evt);
            }
            else
              $('html body').eucaevent('del_click', 'navigator');
            return false;
          });
      })
    },
   

   _destroy : function(){
    }
  });    
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
