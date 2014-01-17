package com.eucalyptus.objectstorage;

/**
 * Resource state for objects and buckets
 *
 */
public enum ResourceState {
	creating, extant, deleting, deleted, reaping, transitioning
}
