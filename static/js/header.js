(function($, eucalyptus) {
  $.widget('eucalyptus.header', {
    options : {
       logoUrl : 'images/eucalyptus_top.png'
    },
    _init : function(){
        this.element.show();
        this.element.children().each(function(idx) {
          $(this).show();
        });
    },
    _create : function(){
       // logo area
       var $logoArea = this.element.find('#euca-logo');
       $logoArea.addClass('euca-header logo');
       $('<img>').attr('src',this.options.logoUrl).appendTo($logoArea);

       // navigation area   
       var $naviArea = this.element.find('#euca-navigator');
       $naviArea.addClass('euca-header navigator');
       $naviArea.append($('<table>').attr('width','auto').attr('align','center').append(
                          $('<tbody>').append(
	                    $('<tr>').append(
                              $('<td>').attr('valign','middle').css('width','auto').append(
		                $('<a>').attr('href','#').attr('id','top-button').addClass('euca-header navigator').css('display','block').text(button_explorer)),
				  $('<td>').attr('valign','middle').css('width','auto').append(
				     $('<img>').attr('src','images/triangle.gif'))))));
    
       // search box
       
       //user area
       var $userArea = this.element.find('#euca-user');
       $userArea.addClass('euca-header user');
       $('<span>').addClass('euca-header user').attr('id','name').text($.eucaData.u_session['username']+' ').append($('<img>').attr('src','images/triangle.gif')).appendTo($userArea);

       //help area 
       var $helpArea = this.element.find('#euca-help');
       $helpArea.addClass('euca-header help');
       $('<span>').addClass('euca-header help').attr('id','help').text('help ').append($('<img>').attr('src','images/triangle.gif')).appendTo($helpArea);

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
         var $explorer = $('html body').find('.euca-container .euca-explorer');
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
