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
       this.element.find('#euca-user-text').text(username);
    },
    _create : function(){
       // navigation area   
       var $naviArea = this.element.find('#euca-navigator');
       $naviArea.append($('<div>').attr('id','euca-navigator-text').append(
		          $('<a>').attr('href','#').attr('id','top-button').addClass('ex-navigator').css('display','block').text(button_explorer)),
                        $('<div>').attr('id','euca-navigator-icon').append(
                          $('<img>').attr('src','images/triangle.gif')));
       
       //help area 
       var $helpArea = this.element.find('#euca-help');
       $helpArea.append(
         $('<div>').attr('id','euca-help-text').text('Help'),
         $('<div>').attr('id','euca-help-icon').append(
           $('<img>').attr('src','images/triangle.gif'))); 

       //user area
       var $userArea = this.element.find('#euca-user');
       $userArea.append(
         $('<div>').attr('id','euca-user-text').text('null'),
         $('<div>').attr('id','euca-user-icon').append(
           $('<img>').attr('src','images/triangle.gif'))); 

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
         var $explorer = $('html body').find('.euca-explorer-container');
         if($(this).hasClass('mouseon')){
             $explorer.toggle('blind', {}, 300 );
         }else{
             $explorer.toggle('blind', {}, 300 );
         }
       });
    },
    _destroy : function(){
    }
  });    
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
