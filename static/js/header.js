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
            if(widget.options.show_user)
              $(this).show();
            else
              $(this).hide();
          }
          if ($(this).attr('id')==='euca-help'){
            if(widget.options.show_help)
              $(this).show();
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
       var username ='null';
       if('u_session' in $.eucaData)
         username = $.eucaData.u_session['username'];
       this.element.find('#euca-user a').text(username);
    },
    _create : function(){
       var thisObj = this;
       // navigation area   
       var $naviArea = this.element.find('#euca-navigator');
       $naviArea.append($('<a>').attr('href','#').attr('id','top-button').addClass('ex-navigator').css('display','block').text(button_explorer));
       
       //help area 
       var $helpArea = this.element.find('#euca-help');
       $helpArea.append(
         $('<a>').attr('href','#').text('Help'));

       //user area
       var $userArea = this.element.find('#euca-user');
       $userArea.append(
         $('<a>').attr('href','#').text('null'));

       // event handlers
       this.element.find('#euca-navigator').hover(
         function () {
           $(this).find('#top-button').addClass('mouseon');
           $(this).addClass('mouseon');
         }, 
         function () {
           $(this).find('#top-button').removeClass('mouseon');
           $(this).removeClass('mouseon');
         }
       );
 
       this.element.find('#euca-navigator').click(function (){
         var $explorer = $('html body').find('.euca-explorer-container .inner-container');
         if($(this).hasClass('mouseon')){
             $explorer.toggle('blind', {}, 300 );
         }else{
             $explorer.toggle('blind', {}, 300 );
         }
       });

       this.element.find('#euca-user').click(function (evt) {
         if($(this).hasClass('clicked')){
           $(this).removeClass('clicked');
           $('html body').find('.user-menu').remove();
         }
         else{
           var left = $(this).position().left;
           var top = $('html body').find('.euca-main-outercontainer').position().top; 
           $('html body').append(thisObj.createUserMenu(left, top));
           $(this).addClass('clicked');
         }
       });
       
       this.element.find('#euca-help').click(function (evt) {
         if($(this).hasClass('clicked')){
           $(this).removeClass('clicked');
           $('html body').find('.help-menu').remove();
         }
         else{
           var left = $(this).position().left;
           var top = $('html body').find('.euca-main-outercontainer').position().top; 
           $('html body').append(thisObj.createHelpMenu(left, top));
           $(this).addClass('clicked');
         }
       });
    },
   
   createUserMenu : function (left, top) {
      user_menu = { preference : menu_user_preferences,
                    logout : menu_user_logout,
      }
      var header = this;
      var $ul = $('<ul>'); 
      $.each(user_menu, function (key, val) {
        $ul.append(
           $('<li>').append(
             $('<a>').attr('href','#').text(val).click(function(evt) {
               header._trigger("select", evt, {selected:key});
           })));
      });

      return $('<div>').addClass('user-menu').css('left',left+'px').css('top',top+'px').append($ul);
    },
 
   createHelpMenu : function (left, top) {
      help_menu = { documentation : menu_help_documentation,
                    forum : menu_help_forum,
                    report : menu_help_report
                  }
      var header = this;
      var $ul = $('<ul>'); 
      $.each(help_menu, function (key, val) {
        $ul.append(
           $('<li>').append(
             $('<a>').attr('href','#').text(val).click(function(evt) {
               header._trigger("select", evt, {selected:key});
           })));
      });

      return $('<div>').addClass('help-menu').css('left',left+'px').css('top',top+'px').append($ul);
    },

   _destroy : function(){
    }
  });    
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
