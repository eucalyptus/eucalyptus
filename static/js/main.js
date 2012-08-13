/*
 * License
 */
(function($, eucalyptus) {
  eucalyptus.main= function(args) {
    $('html body').eucaevent();
    eucalyptus.explorer();
    $('html body').find(DOM_BINDING['header']).header({select: function(evt, ui){
                                                                      $container.maincontainer("changeSelected",evt,ui);}, show_logo:true,show_navigation:true,show_user:true,show_help:true});

    $('html body').find(DOM_BINDING['notification']).notification();
    var $container = $('html body').find(DOM_BINDING['main']);
    $container.maincontainer();
    $('html body').find(DOM_BINDING['explorer']).explorer({select: function(evt, ui){ 
                                                                      $container.maincontainer("changeSelected",evt, ui);
                                                                   }});
    $('html body').find(DOM_BINDING['footer']).footer();
  } // end of main
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
