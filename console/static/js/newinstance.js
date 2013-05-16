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
  $.widget('eucalyptus.newinstance', $.eucalyptus.eucawidget, {
    options : { },
    _init : function() {
      var thisObj = this;
      thisObj.widgetName = 'launcher';
      $(thisObj.element).unbind();
      require(['views/newinstance/index'], function(wizardFactory) {
        var View = wizardFactory(thisObj.options);
        var view = new View({el: thisObj.element});
      	view.render();
        if(thisObj.options.image != null) {
          view.jump(1);
        }
        thisObj.options.image = null;
        var $tmpl = $('html body').find('.templates #launchWizardTmpl').clone();
        var $wrapper = $($tmpl.render($.extend($.i18n.map, help_launcher)));
        var $launcher = $wrapper.children().first();
        var $launcherHelp = $wrapper.children().last();
        $launcherHelp.load();
        thisObj._addHelp($launcherHelp);
      });
    },

    _addHelp : function(help){
      var thisObj = this;
      var $target = $('.wizard-wrapper');
      $('#launch-wizard-header div.help-link a').click( function(evt){
        thisObj._flipToHelp(evt,{content: help_launcher.landing_content, url: help_launcher.landing_content_url}, $target);
      });
      return $('#launch-wizard-header');
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _expandCallback : function(row){ 
       var $wrapper = $('');
      return $wrapper;
    },

/**** Public Methods ****/
    close: function() {
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
