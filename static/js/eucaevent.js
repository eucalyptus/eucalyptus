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
  $.widget('eucalyptus.eucaevent', {
    options : {
    },

    _clicked : {},

    _init : function(){
      var thisObj = this;
      this.element.click( function (evt, src) {
        $.each(thisObj._clicked, function (k, v){
            // e.g., src='navigator:storage:volumes', k = 'storage'
           // $(evt.target).parent()[0]
          if (!src || src.indexOf(k) == -1)
            $(v.target).trigger('click', ['triggered']);
        });
        if(src && ( src.indexOf('navigator')>=0 || src.indexOf('create-new') >=0) ){ // 'src=navigator:*' when the click event is generated due to menu selection; or src=create-new when event is generated due to clicking on create-new button
          return false; // this will fix hashtag implementation
        }
        if($(evt.target).is('a') && $(evt.target).attr('href') === '#')
          return false;
//        console.log('num events: '+Object.keys(thisObj._clicked).length);
      });
    },

    _create : function(){
    },

    _destroy : function() {
    },

    add_click : function(src, evtObj) {
      this._clicked[src] = evtObj;
    },

    del_click : function(src) {
      delete this._clicked[src];  
    },

    unclick_all : function(src) {
      $.each(this._clicked, function (k, v){
        $(v.target).trigger('click');
      });
    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
