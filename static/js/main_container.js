(function($, eucalyptus) {
  $.widget('eucalyptus.maincontainer', {
    options : { 
        default_selected : 'dashboard',
    },

    _curSelected : null,

    _init : function() {
   
     },

    _create : function() { 
       this._curSelected = this.options.default_selected;
    },

    _destroy : function() {
    },

    // event receiver
    changeSelected : function (evt, ui) { 
      this._curSelected=ui.selected;
    },
   
    ec2Completed : function (args) {

    },
   
    ec2Error : function (args) {

    },
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
