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
  $.widget('eucalyptus.launcher', $.eucalyptus.eucawidget, {
    options : { },
    _create : function() { },
    _init : function() {
      // read template and attach to the widget element
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #launchWizardTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_launcher)));
      var $launcher = $wrapper.children().first();
      var $launcherHelp = $wrapper.children().last();

      // make section headers
      thisObj._makeSectionHeader($launcher);
 
      // make section buttons 
      thisObj._makeSectionButton($launcher);
      // make sections (image, type, security, advanced options)

      thisObj._makeImageSection($launcher.find('#launch-wizard-image'));
      thisObj._makeTypeSection($launcher.find('#launch-wizard-type'));
      thisObj._makeSecuritySection($launcher.find('#launch-wizard-security'));
      thisObj._makeAdvancedSection($launcher.find('#launch-wizard-advanced'));
      $launcher.appendTo(thisObj.element);
     },
    _destroy : function() { },

    _makeSectionHeader : function($launcher) { },
 
    _makeSectionButton : function($launcher) { },

    _makeImageSection : function($section){ },
   
    _makeTypeSection : function($section){ },

    _makeSecuritySection : function($section) { },

    _makeAdvancedSection : function($section) { },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
