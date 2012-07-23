(function($, eucalyptus) {
  eucalyptus.explorer= function(args) {
      var $container = args.container;
      var name = args.item;
      // create div
      var imgUrl = $.eucaData.image['navi_'+name];
      var $newitem = $('<div>').addClass('euca-container euca-explorer explorer-item').attr('id','explorer-item-'+name).append(
	   $('<img>').attr('src', imgUrl));
      if (args.idx == 0){
          $newitem.css('clear','left');
          $newitem.css('margin-left','20px');
      }
      $container.append($newitem);
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
