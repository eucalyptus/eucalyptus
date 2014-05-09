package com.eucalyptus.objectstorage

public class OsgBucketFactory {
	private static final BucketFactory factory = new BucketFactoryImpl();
	
	public static BucketFactory getFactory() {
		return factory;
	}
}

public class OsgObjectFactory {
	private static final ObjectFactory factory = new ObjectFactoryImpl();
	
	public static ObjectFactory getFactory() {
		return factory;
	}
}

