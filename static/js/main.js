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
  eucalyptus.main= function(args) {
    $('html body').eucaevent();

    // TODO: is this the right place to check online status?
    $('html body').eucadata();
    $.when( 
      (function(){ 
        var dfd = $.Deferred();
        var waitSec = 30;
        var intervalMs = 100;
        var numCheck = 0;
        var token = null; 
        token = runRepeat( function (){
          if($('html body').eucadata('getStatus') === 'online'){
            cancelRepeat(token);
            dfd.resolve();
          }else{
            if((numCheck++)*intervalMs > waitSec*1000){
              cancelRepeat(token);
              dfd.reject();
            }
          }
        }, intervalMs, true);
        return dfd.promise();
      })()
    ).done(function(){
      eucalyptus.explorer();
      $('html body').find(DOM_BINDING['header']).header({select: function(evt, ui){
         $container.maincontainer("changeSelected",evt,ui);}, show_logo:true,show_navigation:true,show_user:true,show_help:true});

      $('html body').find(DOM_BINDING['notification']).notification();
      var $container = $('html body').find(DOM_BINDING['main']);
      $container.maincontainer();
      $('html body').find(DOM_BINDING['explorer']).explorer({select: function(evt, ui){ 
                                                                      $container.maincontainer("changeSelected",evt, ui);
                                                                   }});
      $('html body').find(DOM_BINDING['footer']).footer();

      $('html body').keypress(function(e){
        if (!(e.ctrlKey && e.shiftKey))
          return true;
        if($(e.target).is('input'))
          return true;
        if($(e.target).is('select'))
          return true;
        if($(e.target).is('textarea'))
          return true;

        var key = e.which;
          /*dashboard: D, d (68, 100)
          images: I, i (73, 105)
          instances: N, n (78, 110)
          volumes: V, v (86, 118)
          snapshots: S, s (83, 115)
          security groups: G, g (71, 103)
          key pairs: K, k (75, 107)
          address: A, a (65, 97)          
          launch new instance: L, l (76, 108) */
        $('html body').eucaevent('unclick_all'); // this will close menus that's pulled down
        switch(key){
          case 68:
          case 100:
            $container.maincontainer("changeSelected", e, {selected:'dashboard'});
          break;
          case 73:
          case 105:
            $container.maincontainer("changeSelected", e, {selected:'image'});
          break;
          case 78:
          case 110:
            $container.maincontainer("changeSelected", e, {selected:'instance'});
          break;
          case 86:
          case 118:
            $container.maincontainer("changeSelected", e, {selected:'volume'});
          break;
          case 83:
          case 115:
            $container.maincontainer("changeSelected", e, {selected:'snapshot'});
          break;
          case 71:
          case 103:
            $container.maincontainer("changeSelected", e, {selected:'sgroup'});
          break;
          case 75:
          case 107:
            $container.maincontainer("changeSelected", e, {selected:'keypair'});
          break;
          case 65:
          case 97:
            $container.maincontainer("changeSelected", e, {selected:'eip'});
          break;
          case 76:
          case 108:
            $container.maincontainer("changeSelected", e, {selected:'launcher'});
          break;
        }
      });
    }).fail(function(){
        //TODO: what's the appropriate error message and the popup?
        errorAndLogout();
      }
    );
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
