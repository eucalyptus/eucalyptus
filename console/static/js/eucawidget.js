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

/*
  This widget is the base (in the sense of oop) from which other eucalyptus widgets (dashboard, images, instances, etc) inherit. 
  Note that the inheritence using widget-factory isn't really the same as subclassing. If we need full-fledged inheritence, we should apply more complex patterns.
*/
(function($, eucalyptus) {
  $.widget('eucalyptus.eucawidget', {
    options : { },

    _init : function() {
    },

    _create : function() { 
    },

    _destroy : function() {
    },
    

    _help_flipped : false,
    _flipToHelp : function(evt, help, $target) {
       var thisObj  = this;
       var $helpWrapper = $('<div>'); // this one gets stripped off
       var $helpContainer = $('<div>').addClass('help-page-wrapper clearfix').append(getLandingHelpHeader(), help.content);
       $helpWrapper.append($helpContainer);
       
       if(! $target)
         $target = thisObj.element.children();

       $target.flip({
         direction : 'lr',
         speed : 300,
         bgColor : 'white',
         color : 'white',
         easingIn: 'easeInQuad',
         easingOut: 'easeOutQuad',
         content : $helpWrapper,
         onEnd : function() {
            thisObj.element.find('.help-revert-button a').click( function(evt) {
              $target.revertFlip();
            });
            thisObj.element.find('.help-link a').click( function(evt) {
              $target.revertFlip();
            });       
            if(!thisObj._help_flipped){
               thisObj._help_flipped = true;
               thisObj.element.find('.help-link').removeClass().addClass('help-return').before(
                $('<div>').addClass('help-popout').append(
                  $('<a>').attr('href','#').text('popout').click(function(e){
                    if(help.url){
                      if(help.pop_height)
                        popOutPageHelp(help.url, help.pop_height);
                      else
                        popOutPageHelp(help.url);
                    }
                    thisObj.element.find('.help-revert-button a').trigger('click');
                  })));
               thisObj.element.find('.help-page-wrapper').parent().removeAttr('style'); // flip plugin adds unnecessary style
            }else{
               thisObj._help_flipped = false;
               thisObj.element.find('.help-return').removeClass().addClass('help-link');
               thisObj.element.find('.help-popout').detach();
               var $container = $('html body').find(DOM_BINDING['main']);
               $container.maincontainer("clearSelected");
               $container.maincontainer("changeSelected",evt, {selected:thisObj.widgetName});
            }
          }
        });
    },
/****** Public Methods *********/
    close : function() {
      this.element.children().detach();       
    },
/*******************************/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
