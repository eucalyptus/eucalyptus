(function($, eucalyptus) {
  eucalyptus.explorer= function(args) {
      var $container = args.container;
      var name = args.item;
      // create div
      var $newitem = $('<div>').addClass('euca-container euca-explorer explorer-item').attr('id','explorer-item-'+name).text(name);
      $container.append($newitem); 
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
