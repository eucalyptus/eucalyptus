(function($, eucalyptus) {
  eucalyptus.i18n = function(args) {
    // i18n properties will be loaded before log-in
    jQuery.i18n.properties({
      name:'Messages', 
      path:'custom/', 
      mode:'both',
      language: args.language, 
      callback: function() {
        // We specified mode: 'both' so translated values will be
        // available as JS vars/functions and as a map
        jQuery.i18n.prop('text_footer');
     
        jQuery.i18n.prop('button_explorer');

        jQuery.i18n.prop('menu_dashboard');
        jQuery.i18n.prop('menu_images');
        jQuery.i18n.prop('menu_instances');
        jQuery.i18n.prop('menu_storage');
        jQuery.i18n.prop('menu_netsec');
        jQuery.i18n.prop('menu_support');

        jQuery.i18n.prop('menu_dashboard_dashboard');
        jQuery.i18n.prop('menu_images_images');
        jQuery.i18n.prop('menu_instances_instances');
        jQuery.i18n.prop('menu_storage_volumes');
        jQuery.i18n.prop('menu_storage_snapshots');
        jQuery.i18n.prop('menu_storage_buckets');
        jQuery.i18n.prop('menu_netsec_eip');
        jQuery.i18n.prop('menu_netsec_sgroup');
        jQuery.i18n.prop('menu_netsec_keypair');
        jQuery.i18n.prop('menu_support_guide');
        jQuery.i18n.prop('menu_support_forum');
        jQuery.i18n.prop('menu_support_report');

        jQuery.i18n.prop('help_dashboard_01');
        jQuery.i18n.prop('help_dashboard_02');
        jQuery.i18n.prop('help_dashboard_03');
      }
    });
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
