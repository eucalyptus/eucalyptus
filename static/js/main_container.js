(function($, eucalyptus) {
  $.widget('eucalyptus.maincontainer', {
    options : { 
        default_selected : 'volume',
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

      switch(selected){
        case 'dashboard':
          this.element.dashboard();
          break;
        case 'keypair':
          this.element.keypair();
          break;
        case 'volume':
          this.element.volume();
          break;
        default:
          alert('unknown menu selected: '+selected);
      }
      this._curSelected = selected;
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
