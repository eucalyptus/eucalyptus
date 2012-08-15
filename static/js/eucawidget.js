/*************************************************************************
 * Copyright 2011-2012 Eucalyptus Systems, Inc.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    
    close : function() {
      this.element.children().detach();       
    },

    _help_flipped : false,

    _flipToHelp : function(evt, helpHeader, helpContent ) {
       var thisObj  = this;
       var $helpWrapper = $('<div>').addClass('help-page-wrapper');
       $helpWrapper.append(helpHeader, helpContent);

       thisObj.element.children().flip({
         direction : 'lr',
         speed : 300,
         color : '#ffffff',
         bgColor : '#ffffff',
         content : $helpWrapper,
         onEnd : function() {
            thisObj.element.find('.help-revert-button a').click( function(evt) {
              thisObj.element.children().revertFlip();
            });
            if(!thisObj._help_flipped){
               thisObj._help_flipped = true;
            }else{
               thisObj._help_flipped = false;
               var $container = $('html body').find(DOM_BINDING['main']);
               $container.maincontainer("clearSelected");
               $container.maincontainer("changeSelected",evt, {selected:thisObj.widgetName});
            }
          }
        });
    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
