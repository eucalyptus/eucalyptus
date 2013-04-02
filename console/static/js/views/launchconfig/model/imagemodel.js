define(['models/image'], function(image) {
    return image.extend({

      validation: {
        id: {
            required: true,
            msg: 'Image selection is required.'
        }
      }

    });
});
