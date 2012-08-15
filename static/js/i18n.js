/*************************************************************************
 * Copyright 2011-2012 Eucalyptus Systems, Inc.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

(function($, eucalyptus) {
  eucalyptus.i18n = function(args) {
    // i18n properties will be loaded before log-in
    $.i18n.properties({
      name:'Messages', 
      path:'custom/', 
      mode:'both',
      language: args.language, 
      callback: function() {
        // when jsrender is used, the variables should be propped to make it available as $.i18n.map 
        $.i18n.prop('text_footer');
        $.i18n.prop('login_title');
        $.i18n.prop('login_acct');
        $.i18n.prop('login_uname');
        $.i18n.prop('login_pwd');
        $.i18n.prop('login_pwd_help');
        $.i18n.prop('login_pwd_link');
        $.i18n.prop('login_btn');

        $.i18n.prop('kp_tbl_hdr_name');
        $.i18n.prop('kp_tbl_hdr_fingerprint');
        $.i18n.prop('table_loading');

        // keypair
        $.i18n.prop('keypair_dialog_del_text');
        $.i18n.prop('keypair_dialog_add_text');
        $.i18n.prop('keypair_dialog_add_name');
        $.i18n.prop('keypair_dialog_add_help');
        $.i18n.prop('keypair_dialog_delete_help');
        $.i18n.prop('keypair_table_help');

        // sgroup
        $.i18n.prop('sgroup_dialog_del_text');
        $.i18n.prop('sgroup_dialog_add_text');
        $.i18n.prop('sgroup_dialog_add_name');
        $.i18n.prop('sgroup_dialog_add_description');

        // volume
        $.i18n.prop('volume_dialog_del_text');
        $.i18n.prop('volume_dialog_add_text');

        // dashboard
        $.i18n.prop('dashboard_title');
        $.i18n.prop('dashboard_az_all');
        $.i18n.prop('dashboard_instance_running');
        $.i18n.prop('dashboard_instance_stopped');
        $.i18n.prop('dashboard_volumes');
        $.i18n.prop('dashboard_snapshots');
        $.i18n.prop('dashboard_buckets');
        $.i18n.prop('dashboard_sgroup');
        $.i18n.prop('dashboard_eip');
        $.i18n.prop('dashboard_keypair');


        // help
        $.i18n.prop('page_help_backto_page');
      }
    });
  }
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
