package edu.ucsb.eucalyptus.admin.client.extensions.store;


public interface ImageState {

    /**
     * Return the URI for the image which this state refers to.
     *
     * @return the URI for the image this state refers to.
     */
    String getImageUri();

    /**
     * Return the last error message for this image, or null.
     *
     * @return the last error related to requested changes in this image,
     *         or null if no error is known.
     */
    String getErrorMessage();

    /**
     * Return the current status of the image.  Some statuses are transient
     * and in some cases may have their progress tracked via the
     * getProgressPercentage() method.
     *
     * @return constant from the ImageState.Status enum.
     */
    Status getStatus();

    /**
     * Return progress percentage.
     *
     * @return the completion percentage for the on going task, or null
     *         if there's no task in progress.
     */
    Integer getProgressPercentage();

    /**
     * Return the URI to perform the given action.
     *
     * @return URI to perform the requested action.
     */
    String getActionUri(Action action);
    
    /**
     * Return true if the action is available in this state.
     *
     * @return true if this state has the given action URI.
     */
    boolean hasAction(Action action);

    /**
     * Return whether this image is an upgrade to an already installed
     * image.
     *
     * @return true if an older version of this image is installed.
     */
    boolean isUpgrade();

    enum Status {
        UNINSTALLED(false),
        DOWNLOADING(true),
        INSTALLING(true),
        INSTALLED(false),
        UNKNOWN(false);

        private final boolean isTransient;

        Status(boolean isTransient) {
            this.isTransient = isTransient;
        }

        boolean isTransient() {
            return this.isTransient;
        }
    }

    enum Action {
        INSTALL,
        CANCEL,
        CLEAR_ERROR;
    }
}
