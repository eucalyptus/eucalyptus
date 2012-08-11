(function($, eucalyptus) {
  $.widget('eucalyptus.eucadialog', {
    options : { 
       id : null, // e.g., keys-add, keys-delete, etc
       title : null,
       buttons :  {},
       // e.g., add : { domid: keys-add-btn, text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
    },
    
    _init : function() {
      var thisObj = this;
      this.element.dialog({
         autoOpen: false,  // assume the three params are fixed for all dialogs
         modal: true,
         width: 600,
         title: thisObj.options.title,
         open: function(event, ui) {
             $titleBar = thisObj.element.parent().find('.ui-dialog-titlebar');
             if ( $titleBar.find("div.help-link").length == 0 ) {
               // first time opened
               $titleBar.append($('<div>').addClass('help-link').append($('<a>').attr('href','#').text('?')));
             }

             $('.ui-widget-overlay').live("click", function() {
               thisObj.element.dialog("close");
             });

             $.each(thisObj.options.buttons, function (btn_id, btn_prop){ 
               // random ID will cause trouble with selenium
               var $btn = $(":button:contains('"+ btn_id +"')");
               if(btn_prop.domid !== undefined)
                 $btn.attr('id',btn_prop.domid);
               else
                 $btn.attr('id', 'btn-'+thisObj.options.id);
               $btn.find('span').text(btn_prop.text);
               if(btn_prop.focus !== undefined && btn_prop.focus)
                 $btn.focus();    
               if(btn_prop.disabled !== undefined && btn_prop.disabled)
                 $btn.prop("disabled", true).addClass("ui-state-disabled");
             });

         },
         buttons: thisObj._makeButtons(),
      });
    },
    
    _create : function() {  
    },

    _destroy : function() {
    },

    _makeButtons : function() {
      var btnArr = [];
       // e.g., add : { text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
      $.each(this.options.buttons, function (btn_id, btn_prop){
        var btn = {}
        btn.text = btn_id; // this will be replaced in open()
        $.each(btn_prop, function(key, value){
          if (typeof(value) == "function")
            btn[key] = value;
        });
        btnArr.push(btn);  
      });
      return btnArr; 
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
