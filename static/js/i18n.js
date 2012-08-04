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
        $.i18n.prop('login_title');
        $.i18n.prop('login_acct');
        $.i18n.prop('login_uname');
        $.i18n.prop('login_pwd');
        $.i18n.prop('login_pwd_help');
        $.i18n.prop('login_pwd_link');
        $.i18n.prop('login_btn');
      }
    });
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
