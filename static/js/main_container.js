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

    _init : function() {
      this.updateSelected(this.options.default_selected);
      this.element.show();
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    // event receiver for menu selection
    changeSelected : function (evt, ui) { 
        this.updateSelected(ui.selected, ui.filter);
    },

    updateSelected : function (selected, filter) {
      if(this._curSelected === selected){
        //$('html body').trigger('click'); // Manage resources --> uncomment this line
        return;
      }

      if(this._curSelected !== null){
        var $curInstance = this.element.data(this._curSelected);
        if($curInstance !== undefined){
          $curInstance.close();
        }
      }
      var $container = $('html body').find(DOM_BINDING['main']);
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
        default:
          $('html body').find(DOM_BINDING['notification']).notification('error', 'internal error', selected+' not yet implemented', 1);
      }
      this._curSelected = selected;
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
