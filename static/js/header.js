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
       $helpArea.append(
         $('<ul>').addClass('header-nav').append(
           $('<li>').append(
             $('<a>').attr('href','#').text(menu_help).click(function(e){ 
               	$helpArea.find('.header-nav ul').slideToggle('fast'); 
		            $(this).toggleClass('toggle-on');
             }),
             $('<ul>').append(
               $('<li>').append(
                 $('<a>').attr('href','#').text(menu_help_documentation).click(function(e){
                   thisObj._trigger('select',e, {selected:'documentation'});})),
               $('<li>').append(
                 $('<a>').attr('href','#').text(menu_help_forum).click(function(e) {
                   thisObj._trigger('select',e, {selected:'forum'});})),
               $('<li>').append(
                 $('<a>').attr('href','#').text(menu_help_report).click(function(e) {
                   thisObj._trigger('select',e, {selected:'report'});}))))));

       //user area
       var uname =$.eucaData.u_session['account']+'/'+ $.eucaData.u_session['username'];
       var $userArea = this.element.find('#euca-user');
       $userArea.append(
         $('<ul>').addClass('header-nav').append(
           $('<li>').append(
             $('<a>').attr('href','#').text(uname).click(function(e){
               $userArea.find('.header-nav ul').slideToggle('fast');
               $(this).toggleClass('toggle-on');
             }),
             $('<ul>').append(
               $('<li>').append(
                 $('<a>').attr('href','#').text(menu_user_preferences).click(function(e) {
                   thisObj._trigger('select',e, {selected:'preference'});})),
               $('<li>').append(
                 $('<a>').attr('href','#').text(menu_user_logout).click(function(e) {
                   thisObj._trigger('select',e, {selected:'logout'});}))))));

        // event handlers
        var $navigator = $('#euca-navigator');
        $navigator.click(function (){
          $('#euca-explorer').slideToggle('fast'); 
          $navigator.toggleClass('toggle-on');
        });
    },
   
   // OK to delete this ?
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
 
   // OK to delete this ?
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
