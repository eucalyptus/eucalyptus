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

(function($, eucalyptus) {
  $.widget('eucalyptus.header', {
    options : {
       logoUrl : 'images/eucalyptus_top.png',
       show_logo : true,
       show_navigation : false,
       show_help : false,
       show_user : false,
       show_search : false
    },
    _init : function(){
        var widget = this;
        this.element.show();
        this.element.children().each(function(idx) {
          if ($(this).attr('id')==='euca-navigator'){
            if(widget.options.show_navigation)
              $(this).show();
            else
              $(this).hide();
          }
          if ($(this).attr('id')==='euca-user'){
            if(widget.options.show_user){
              $(this).show();
            }
            else
              $(this).hide();
          }
          if ($(this).attr('id')==='euca-help'){
            if(widget.options.show_help){
              $(this).show();
            }
            else
              $(this).hide();
          }
          if ($(this).attr('id')=='euca-search'){
            if(widget.options.show_search)
              $(this).show(); 
            else
              $(this).hide();
          }
        });
    },

    _create : function(){
       var thisObj = this;
       // navigation area   
       var $naviArea = this.element.find('#euca-navigator');
       $naviArea.append($('<a>').attr('href','#').attr('id','resources-link').text(button_explorer));
       
       //help area 
       var $helpArea = this.element.find('#euca-help');

       var help_menus = {'documentation':menu_help_documentation,'forum':menu_help_forum,'report':menu_help_report}
       var $help_menus = $('<ul>');
       $.each(help_menus, function(k, v){
         $('<li>').append(
           $('<a>').attr('href','#').text(v).click(function(e,src){
             if(src!=='triggered')
               thisObj._trigger('select',e, {selected:k});
            })).appendTo($help_menus);
       });
       $helpArea.append(
         $('<ul>').addClass('header-nav').append(
           $('<li>').append(
             $('<a>').attr('href','#').text(menu_help).click(function(evt, src){ 
               	$helpArea.find('.header-nav ul').slideToggle('fast'); 
	        $(this).toggleClass('toggle-on');
                $('html body').trigger('click','help');
                if ($(this).hasClass('toggle-on'))
                  $('html body').eucaevent('add_click', 'help', evt);
                else
                  $('html body').eucaevent('del_click', 'help');
                return false;
             }),
             $help_menus)));
     

       //user area
       var user_menus = {'preference':menu_user_preferences,'aboutcloud':menu_user_aboutcloud,'logout':menu_user_logout}

       var uname =$.eucaData.u_session['account']+'/'+ $.eucaData.u_session['username'];
       var $userArea = this.element.find('#euca-user');
      
       var $user_menus = $('<ul>');
       $.each(user_menus, function(k, v){
         $('<li>').append(
           $('<a>').attr('href','#').text(v).click(function(e,src){
             if(src!=='triggered')
               thisObj._trigger('select',e, {selected:k});
            })).appendTo($user_menus);
       });
 
       $userArea.append(
         $('<ul>').addClass('header-nav').append(
           $('<li>').append(
             $('<a>').attr('href','#').text(uname).click(function(evt, src){
               $userArea.find('.header-nav ul').slideToggle('fast');
               $(this).toggleClass('toggle-on');
               $('html body').trigger('click', 'user');
               if ($(this).hasClass('toggle-on'))
                 $('html body').eucaevent('add_click', 'user', evt);
               else
                 $('html body').eucaevent('del_click', 'user');
               return false;
             }),
             $user_menus)));

        // event handlers
        var $navigator = $('#euca-navigator');
        $navigator.click(function (evt, src){
          $('#euca-explorer').slideToggle('fast'); 
          $navigator.toggleClass('toggle-on');
          $('html body').trigger('click','navigator');
          if ($navigator.hasClass('toggle-on')){
            $('html body').find('.euca-explorer-container .inner-container').explorer('onSlide');
            $('html body').eucaevent('add_click', 'navigator', evt);
          }
          else
            $('html body').eucaevent('del_click', 'navigator');
          return false;
        });
    },
   

   _destroy : function(){
    }
  });    
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
