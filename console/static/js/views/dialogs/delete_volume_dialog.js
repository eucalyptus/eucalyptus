define([
  './eucadialogview',
  'text!./delete_volume_dialog.html!strip',
  'app',
], function(EucaDialogView, template, App) {
  return EucaDialogView.extend({
    initialize : function(args) {
      var self = this;
      this.template = template;

      this.scope = {
        status: '',
        items: args.items, 

        cancelButton: {
          click: function() {
            self.close();
          }
        },
        deleteButton: {
          click: function() {
              // FOR EACH VOLUME IN THE LIST 'items'
              var done = 0;
              var all = self.scope.items.length;
              var error = [];
              doMultiAjax(self.scope.items, function(item, dfd) {
                console.log("Volume to be deleted: "+item);
                var volumeId = item;
                var volume = App.data.volumes.get(item);
                volume.destroy({
                  wait: true,
                  success:
                    function(model, response, options) {
                      console.log("response = "+JSON.stringify(response));
                      if (response.results && data.results == true) {
                        ;
                      } else {
                        error.push({id:volumeId, reason: undefined_error});
                      }
                      options.complete(model, response);
                    },
                  error:
                    function(model, xhr, options) {
                      console.log("response = "+JSON.stringify(xhr));
                      error.push({id:volumeId, reason: getErrorMessage(xhr)});
                      options.complete(model, xhr);
                    },
                  complete:
                    function(model, response) {
                      done++;
                      if (done < all) {
                        notifyMulti(100*(done/all), $.i18n.prop('volume_delete_progress', all));
                      }
                      else {
                        var $msg = $('<div>').addClass('multiop-summary').append(
                                   $('<div>').addClass('multiop-summary-success').
                                       html($.i18n.prop('volume_delete_done', (all-error.length), all)));
                        if (error.length > 0)
                            $msg.append($('<div>').addClass('multiop-summary-failure').
                                       html($.i18n.prop('volume_delete_fail', error.length)));
                        notifyMulti(100, $msg.html(), error);
                      }
                      dfd.resolve();
                    }
                });
              });
              self.close();
          }
        },
      }
      this._do_init();
    },
  });
});
