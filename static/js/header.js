(function($, eucalyptus) {
    eucalyptus.header= function(args) {
       var $header = $('html body').find('.euca-container .euca-header');
      
       if(args.logo){
           var $logoArea = $header.find('#euca-logo');
           $logoArea.addClass('euca-header logo');
           var imgUrl = $.eucaData.g_session.images['logo'];

           //img width hardcoded
           $('<img>').attr('src',imgUrl).attr('height','30px').appendTo($logoArea);
       }

       //navigation area   
       if(args.navigation) {
           var $naviArea = $header.find('#euca-navigator');
           $naviArea.addClass('euca-header navigator');
           $naviArea.append($('<table>').attr('width','auto').attr('align','center').append(
                            $('<tbody>').append(
			      $('<tr>').append(
                                 $('<td>').attr('valign','middle').css('width','auto').append(
				    $('<a>').attr('href','#').attr('id','top-button').addClass('euca-header navigator').css('display','block').text('Explorer')),
				 $('<td>').attr('valign','middle').css('width','auto').append(
				    $('<img>').attr('src','images/triangle.gif'))))));
       }
    
       // search box
       if(args.search){
           ;
       }
       
       //user area
       if(args.userinfo) {
           var $userArea = $header.find('#euca-user');
           $userArea.addClass('euca-header user');
           $('<span>').addClass('euca-header user').attr('id','name').text($.eucaData.u_session['username']+' ').append($('<img>').attr('src','images/triangle.gif')).appendTo($userArea);
       }

       //help area 
       if(args.help) {
           var $helpArea = $header.find('#euca-help');
           $helpArea.addClass('euca-header help');
           $('<span>').addClass('euca-header help').attr('id','help').text('help ').append($('<img>').attr('src','images/triangle.gif')).appendTo($helpArea);

           $('html body').find('.euca-container .euca-header').css('display','block');       
       }
    
       // event handlers
       $header.find('#euca-navigator').hover(
           function () {
	       $(this).find('#top-button').addClass('mouseon');
               $(this).addClass('mouseon');
           }, 
           function () {
	       $(this).find('#top-button').removeClass('mouseon');
               $(this).removeClass('mouseon');
           }
       );
       $header.find('#euca-navigator').click(function (){
           var $explorer = $('html body').find('.euca-container .euca-explorer');
           if($(this).hasClass('mouseon')){
               $explorer.toggle('blind', {}, 300 );
           }else{
               $explorer.toggle('blind', {}, 300 );
           }
       });
    }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
