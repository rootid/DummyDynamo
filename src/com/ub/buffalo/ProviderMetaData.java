package com.ub.buffalo;

/**
 * 
 * @author vikram
 *
 */
public class ProviderMetaData {
	
	public static String DB_NAME = "project3";
	public static int DB_VERSION = 1;
	public static String AUTHORITY = "edu.buffalo.cse.cse486_586." +
			"simpledynamo.provider";
	public static final String CONTENT_TYPE_LIST = "vnd.android.cursor.dir/";
	public static final String CONTENT_TYPE_ONE = "vnd.android.cursor.item/";

	public class ProviderTable
	{
		public static final String TABLE_NAME = "provider_table";
		public static final String PROVIDER_KEY = "provider_key";
		public static final String PROVIDER_VALUE = "provider_value";
	
	}

}
