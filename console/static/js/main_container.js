/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

(function($, eucalyptus) {
  $.widget('eucalyptus.maincontainer', {
    options : { 
        default_selected : 'dashboard',
    },

    _curSelected : null,
    _aboutDialog : null,

    _init : function() {
      var hash = location.hash;
      if (hash)
        hash = hash.replace(/^#/, '');
      if (hash !== '')
        this.updateSelected(hash);
      else
        this.updateSelected(this.options.default_selected);
      this.element.show();
    },

    _create : function() {
      var thisObj = this;
      // about cloud dialog
      $tmpl = $('html body').find('.templates #aboutCloundDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_about)));
      var $dialog = $rendered.children().first();
      var $dialog_help = $rendered.children().last();
      this._aboutDialog = $dialog.eucadialog({
         id: 'about-cloud',
         title: about_dialog_title,
         buttons: {
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $dialog.eucadialog("close"); } }
         },
         afterHelpFlipped : function() {
           $scrollable = thisObj._aboutDialog.find(".animated");
           $scrollable.css('overflow-y', 'hidden');
           $scrollable.stop();
           $scrollable.animate({scrollTop : 0}, 1);
           $scrollable.animate({scrollTop : $scrollable[0].scrollHeight}, 40*1000, undefined, function() {$scrollable.stop()});
           $scrollable.click( function() {
             $scrollable.stop();
             $scrollable.css('overflow-y', 'scroll');
           });
         },
         beforeHelpFlipped : function() {
           thisObj._aboutDialog.eucadialog('setDialogOption','position', 'top');
         },
         help: { content: $dialog_help },
         help_icon_class : 'help-euca',
       });
      this._aboutDialog.find('#version').html($.eucaData.g_session['version']);
      this._aboutDialog.find('#admin-url').attr('href', $.eucaData.g_session['admin_console_url']);

      $(window).hashchange( function(){
        thisObj._windowsHashChanged();
      });
    },

    _destroy : function() {
    },

    _windowsHashChanged : function() {
      var hash = location.hash;
      if (hash){
        hash = hash.replace(/^#/, '');
      }
      if (this._curSelected !== hash && hash !== '')
       this.updateSelected(hash);
      iamBusy();
    },

    // event receiver for menu selection
    changeSelected : function (evt, ui) {
      this.updateSelected(ui.selected, ui.filter, ui.options);
    },

    updateSelected : function (selected, filter, options) {
      var thisObj = this;
      if(this._curSelected === selected){
        //$('html body').trigger('click'); // Manage resources --> uncomment this line
        return;
      }

      if(this._curSelected !== null){
        var $curInstance = this.element.data(this._curSelected);
        if($curInstance !== undefined && options !== KEEP_VIEW){
          $curInstance.close();
        }
      }
      var $container = $('html body').find(DOM_BINDING['main']);
      if (options !== KEEP_VIEW)
        $container.children().detach();
     // $('html body').eucaevent('unclick_all'); // this will close menus that's pulled down
     // Manage resources-->uncomment above
      switch(selected){
        case 'dashboard':
          this.element.dashboard({select: function(evt, ui){$container.maincontainer("changeSelected", evt, ui)}});
          break;
        case 'instance':
          this.element.instance({'state_filter': filter});
          break;
        case 'keypair':
          this.element.keypair();
          break;
        case 'sgroup':
          this.element.sgroup();
          break;
        case 'volume':
          this.element.volume();
          break;
        case 'snapshot':
          this.element.snapshot();
          break;
        case 'eip':
          this.element.eip();
          break;
        case 'launcher':
          var option = {};
          if(filter && filter['image'])
            option['image'] = filter['image']; 
          if(filter && filter['type'])
            option['type'] = filter['type'];
          if(filter && filter['security'])
            option['security'] = filter['security'];
          if(filter && filter['advanced'])
            option['advanced'] = filter['advanced'];
          this.element.launcher(option);
          break;
        case 'image':
          this.element.image();
          break;
        case 'logout':
          logout();
          break;
        case 'help':
          window.open($.eucaData.g_session['help_url'], '_blank');
          break;
        case 'aboutcloud':
          this._aboutDialog.eucadialog("open");
          break;
        default:
          $('html body').find(DOM_BINDING['notification']).notification('error', 'internal error', selected+' not yet implemented', 1);
      }
      if (options !== KEEP_VIEW) {
        this._curSelected = selected;
        location.hash = selected;
      }
    },

    clearSelected : function (){
      var $curInstance = this.element.data(this._curSelected);
      if($curInstance !== undefined){
        $curInstance.close();
      }
      this._curSelected = null;
    },

    getSelected : function () {
      return this._curSelected;
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
