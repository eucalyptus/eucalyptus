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
    
      thisObj.element.addClass('launch-wizard');

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

    _makeSectionHeader : function($launcher) {
      var thisObj = this;
      var $header = $launcher.find('#launch-wizard-image-header');
      $header.append($('<a>').attr('href', '#').html(launch_instance_section_header_image).click( function(e) {
        var imgSection = $launcher.find('#launch-wizard-image-contents');
        thisObj._selectedSection.slideToggle('fast');
        imgSection.slideToggle('fast');
        thisObj._selectedSection = imgSection;
      }));
      $header = $launcher.find('#launch-wizard-type-header');
      $header.append($('<a>').attr('href', '#').html(launch_instance_section_header_type).click(function(e) {
        var typeSection = $launcher.find('#launch-wizard-type-contents');
        thisObj._selectedSection.slideToggle('fast');
        typeSection.slideToggle('fast');
        thisObj._selectedSection = typeSection;
      }));
      $header = $launcher.find('#launch-wizard-security-header');
      $header.append($('<a>').attr('href', '#').html(launch_instance_section_header_security).click(function(e){
        var secSection = $launcher.find('#launch-wizard-security-contents');
        thisObj._selectedSection.slideToggle('fast');
        secSection.slideToggle('fast');
        thisObj._selectedSection = secSection;
      }));
      $header = $launcher.find('#launch-wizard-advanced-header');
      $header.append($('<a>').attr('href', '#').html(launch_instance_section_header_advanced).click(function(e){
        var advSection = $launcher.find('#launch-wizard-advanced-contents');
        thisObj._selectedSection.slideToggle('fast');
        advSection.slideToggle('fast');
        thisObj._selectedSection = advSection;
      }));
    },
 
    _makeSectionButton : function($launcher) {
      var thisObj = this;
      var $imageBtn = $launcher.find('#launch-wizard-buttons-image');
      $imageBtn.append($('<button>').attr('id', 'launch-wizard-buttons-image-next').html(launch_instance_btn_next_type).click( function(e){
        var typeSection = $launcher.find('#launch-wizard-type-contents');
        thisObj._selectedSection.slideToggle('fast');
        typeSection.slideToggle('fast');
        thisObj._selectedSection = typeSection;
      }));
      var $typeBtn = $launcher.find('#launch-wizard-buttons-type');
      $typeBtn.append($('<button>').attr('id', 'launch-wizard-buttons-type-next').html(launch_instance_btn_next_security).click( function(e){
        var secSection = $launcher.find('#launch-wizard-security-contents');
        thisObj._selectedSection.slideToggle('fast');
        secSection.slideToggle('fast');
        thisObj._selectedSection = secSection;
      }));
      var $securityBtn = $launcher.find('#launch-wizard-buttons-security');
      $securityBtn.append(
        $('<ul>').append(
          $('<li>').append($('<button>').attr('id','launch-wizard-buttons-security-launch').html(launch_instance_btn_launch).click( function(e){
        })),
          $('<li>').append($('<span>').text('Or:'), $('<a>').attr('href','#').html(launch_instance_btn_next_advanced).click( function(e){
            var advSection = $launcher.find('#launch-wizard-advanced-contents');
            thisObj._selectedSection.slideToggle('fast');
            advSection.slideToggle('fast');
            thisObj._selectedSection = advSection;
        })))); 
      var $advancedBtn = $launcher.find('#launch-wizard-buttons-advanced');
      $advancedBtn.append(
        $('<button>').attr('id', 'launch-wizard-buttons-advanced-launch').html(launch_instance_btn_launch).click( function(e){
        })); 
    },
    _selectedSection : null,
    _makeImageSection : function($section){ 
      var thisObj = this;
      var $content = $section.find('#launch-wizard-image-main-contents');
      $.each($content.children(), function(idx, child){
        $(child).html('image-contents'); 
      });

      $content = $section.find('#launch-wizard-image-contents').slideToggle('fast');
      thisObj._selectedSection = $content;
    },
   
    _makeTypeSection : function($section){
      var $content = $section.find('#launch-wizard-type-main-contents');
      $.each($content.children(), function(idx, child){
        $(child).html('type-contents'); 
      });
    },

    _makeSecuritySection : function($section) {
      var $content = $section.find('#launch-wizard-security-main-contents');
      $.each($content.children(), function(idx, child){
        $(child).html('security-contents'); 
      });
    },

    _makeAdvancedSection : function($section) { 
      var $content = $section.find('#launch-wizard-advanced-main-contents');
      $.each($content.children(), function(idx, child){
        $(child).html('advanced-contents'); 
      });
    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
