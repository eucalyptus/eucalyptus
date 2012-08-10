/*
 * License
 */
(function($, eucalyptus) {
  eucalyptus.main= function(args) {
    eucalyptus.explorer();
    $('html body').find('.euca-container .euca-header-container .inner-container').header({select: function(evt, ui){
                                                                      $container.maincontainer("changeSelected",evt,ui);}, show_logo:true,show_navigation:true,show_user:true,show_help:true});
    var $container = $('html body').find('.euca-main-outercontainer .inner-container #euca-main-container');
    $container.maincontainer();
    $('html body').find('.euca-explorer-container .inner-container').explorer({select: function(evt, ui){ 
                                                                      $container.maincontainer("changeSelected",evt, ui);
                                                                   }});
    $('html body').find('.euca-container .euca-footer-container .inner-container').footer();
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
