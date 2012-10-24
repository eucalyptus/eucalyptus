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
  /* jQuery widget factory */
  $.widget("eucalyptus.footer", {
     options: { } , 
     _init: function () {
       var $tmpl = $('html body').find('.templates #footerTmpl');
       var $wrapper = $($tmpl.render($.i18n.map));
       this.element.append($wrapper);
       this.element.show();
     },

     // jQuery widget method 
     _create: function () {
     },

     _setOption: function () {
     },     

     // jQuery widget method 
     _destroy: function () {
       //
       this.element.hide();
     }
   });
}(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {}));
