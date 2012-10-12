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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 * 
 * the original code -- http://tutorialzine.com/2011/05/generating-files-javascript-php/
 * the license term can be found -- http://tutorialzine.com/license/
 ************************************************************************/

(function($){
  // Creating a jQuery plugin:
  $.generateFile = function(options){
    options = options || {};

    if(!options.script || !options.keyname || !options._xsrf){
      throw new Error("Please enter all the required config options!");
    }

    // Creating a 1 by 1 px invisible iframe:
    var iframe = $('<iframe>',{
      width:1,
      height:1,
      frameborder:0,
      css:{
        display:'none'
      }
    }).appendTo('body');

    var formHTML = '<form action="" method="post">'+
                   '<input type="hidden" name="KeyName" />'+
                   '<input type="hidden" name="_xsrf" />'+
                   '</form>';

    // Giving IE a chance to build the DOM in
    // the iframe with a short timeout:

    setTimeout(function(){
    // The body element of the iframe document:

      var body = (iframe.prop('contentDocument') !== undefined) ?
      iframe.prop('contentDocument').body :
      iframe.prop('document').body;	// IE
			
      body = $(body);
      // Adding the form to the body:
      body.html(formHTML);

      var form = body.find('form');

      form.attr('action',options.script);
      form.find('input[name=KeyName]').val(options.keyname);
      form.find('input[name=_xsrf]').val(options._xsrf);

      // Submitting the form to server. This will
      // cause the file download dialog box to appear.
      form.submit();
    },50);
  };
})(jQuery);
