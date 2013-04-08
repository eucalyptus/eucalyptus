// tag model
//

define([
  'backbone'
], function(Backbone) {
  var model = Backbone.Model.extend({
    idAttribute: 'uid',
    validation: {
      // ====================
      // API Reference: 
      // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-CreateTags.html
      // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DeleteTags.html
      // http://docs.aws.amazon.com/AWSEC2/latest/APIReference/ApiReference-query-DescribeTags.html
      // ====================
        
      res_id: {
        pattern: /^\w{1,3}-\w{8}/,
        required: true
      },
      resource_type: {
        oneOf: [ 'customer-gateway', 'dhcp-options', 'image', 'instance', 'internet-gateway', 'network-acl', 'network-interface', 'reserved-instances', 'route-table', 'security-group', 'snapshot', 'spot-instances-request', 'subnet', 'volume', 'vpc', 'vpn-connection', 'vpn-gateway' ],
        required: false
      },
      name: {
        rangeLength: [1, 128],
        required: true
      },
      value: {
        rangeLength: [0, 256],
        required: false
      },
    },
    sync: function(method, model, options) {
      if (method == 'create') {
        var data = "_xsrf="+$.cookie('_xsrf');
        data += "&ResourceId.1="+model.get('resource_id');
        data += "&Tag.1.Key="+model.get('name');
        data += "&Tag.1.Value="+model.get('value');
        $.ajax({
          type:"POST",
          url:"/autoscaling?Action=CreateTags",
          data:data,
          dataType:"json",
          async:true,
          success:
            function(data, textStatus, jqXHR){
              if ( data.results ) {
                notifySuccess(null, $.i18n.prop('tag_create_success', DefaultEncoder().encodeForHTML(model.get('name'))));
              } else {
                notifyError($.i18n.prop('tag_create_error', DefaultEncoder().encodeForHTML(model.get('name'))), undefined_error);
              }
            },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('tag_create_error', DefaultEncoder().encodeForHTML(model.get('name')), getErrorMessage(jqXHR)));
            }
          });
      }
      else if (method == 'delete') {
        var data = "_xsrf="+$.cookie('_xsrf');
        data += "&ResourceId.1="+model.get('resource_id');
        data += "&Tag.1.Key="+model.get('name');
        data += "&Tag.1.Value="+model.get('value');
        $.ajax({
          type:"POST",
          url:"/autoscaling?Action=DeleteTags",
          data:data,
          dataType:"json",
          async:true,
          success:
            function(data, textStatus, jqXHR){
              if ( data.results ) {
                notifySuccess(null, $.i18n.prop('tag_delete_success', DefaultEncoder().encodeForHTML(model.get('name'))));
              } else {
                notifyError($.i18n.prop('tag_delete_error', DefaultEncoder().encodeForHTML(model.get('name'))), undefined_error);
              }
            },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('tag_delete_error', DefaultEncoder().encodeForHTML(model.get('name'))), getErrorMessage(jqXHR));
            }
        });
      }
    }
  });
  return model;
});
