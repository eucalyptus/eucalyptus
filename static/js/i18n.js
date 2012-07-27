(function($, eucalyptus) {
  eucalyptus.i18n = function(args) {
    // i18n properties will be loaded before log-in
    $.i18n.properties({
      name:'Messages', 
      path:'custom/', 
      mode:'both',
      language: args.language, 
      callback: function() {
        // We specified mode: 'both' so translated values will be
        // available as JS vars/functions and as a map
        $.i18n.prop('text_footer');
     
        $.i18n.prop('button_explorer');

        $.i18n.prop('menu_dashboard');
        $.i18n.prop('menu_images');
        $.i18n.prop('menu_instances');
        $.i18n.prop('menu_storage');
        $.i18n.prop('menu_netsec');
        $.i18n.prop('menu_support');

        $.i18n.prop('menu_dashboard_dashboard');
        $.i18n.prop('menu_images_images');
        $.i18n.prop('menu_instances_instances');
        $.i18n.prop('menu_storage_volumes');
        $.i18n.prop('menu_storage_snapshots');
        $.i18n.prop('menu_storage_buckets');
        $.i18n.prop('menu_netsec_eip');
        $.i18n.prop('menu_netsec_sgroup');
        $.i18n.prop('menu_netsec_keypair');
        $.i18n.prop('menu_support_guide');
        $.i18n.prop('menu_support_forum');
        $.i18n.prop('menu_support_report');

        $.i18n.prop('help_dashboard_01');
        $.i18n.prop('help_dashboard_02');
        $.i18n.prop('help_dashboard_03');
      }
    });
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
