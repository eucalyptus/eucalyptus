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
        this.updateSelected(ui.selected);
    },

    updateSelected : function (selected) {
      if(this._curSelected === selected)
          return;
      if(this._curSelected !== null){
        var $curInstance = this.element.data(this._curSelected);
        if($curInstance !== undefined){
          $curInstance.close();
        }
      }
      $('html body').eucaevent('unclick_all'); // this will close menus that's pulled down
      switch(selected){
        case 'dashboard':
          this.element.dashboard();
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
        case 'logout':
          $.cookie('session-id','');
          location.href = '/';
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
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
