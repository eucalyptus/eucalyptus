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
    }).fail(function(){
        errorAndLogout();
      }
    );
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
