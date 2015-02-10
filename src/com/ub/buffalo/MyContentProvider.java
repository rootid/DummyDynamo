package com.ub.buffalo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.ub.buffalo.DHTThread.DHTThreadType;
import com.ub.buffalo.GeneralMsgFormat.MsgType;

//This should implement minimum of provider_key and provider_value
//And use the insert() and query()
public class MyContentProvider extends ContentProvider {

	public static final String MY_CONTENT_PROVIDER = "My_Content_Provider";
	public static final Uri contentUri = Uri.parse("content://" +
			""+ProviderMetaData.AUTHORITY+"/" +
			""+ProviderMetaData.DB_NAME);

	public static final String CREATE_QUERY =
			"CREATE TABLE " + ProviderMetaData.ProviderTable.TABLE_NAME + " (" + 
					ProviderMetaData.ProviderTable.PROVIDER_KEY + " TEXT NOT NULL, " +
					ProviderMetaData.ProviderTable.PROVIDER_VALUE + " TEXT NOT NULL" +
					");";
	public static final String DROP_QUERY =
			"DROP TABLE IF EXISTS " + ProviderMetaData.ProviderTable.TABLE_NAME + ";";
	private static final String CONTENT_PROVIDER = "Content provider";
	private HashGenerator hashGen = HashGenerator.getHashInstance(Util.ALGORITHM_TYPE);
	String deleteSQL = "DELETE FROM " + ProviderMetaData.ProviderTable.TABLE_NAME + ";";
	private MyDBHelper myDBHelper;
	public static int INSERT;
	private static Context providerContext;
	private static UriMatcher uriMatcher;
	private List<List<Quorum>> queryQuorumList = 
			new ArrayList<List<Quorum>>();
	private static final int TYPE_LIST = 1;
	private static final int TYPE_ONE = 2;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(ProviderMetaData.AUTHORITY,ProviderMetaData.DB_NAME,
				TYPE_LIST);
		uriMatcher.addURI(ProviderMetaData.AUTHORITY, ProviderMetaData.DB_NAME+"/#", 
				TYPE_ONE);
	}
	private List<KeyId> keyIdList;
	private Map<String,KeyId> retryMap = 
			new HashMap<String, KeyId>();
	private Map<String,GeneralMsgFormat> retryQueryMap = 
			new HashMap<String, GeneralMsgFormat>();
	public static Map<String,List<KeyId>> recoveryMap = 
			new HashMap<String, List<KeyId>>();

	public boolean onCreate() {
		providerContext = getContext();
		myDBHelper = new MyDBHelper(providerContext);		
		initAllTasks();
		return true;
	}

	private void initAllTasks() {

		Util.MY_PORT = Util.getlineNumber(getContext());

		//Initialize all lists and maps
		InitThread init = new InitThread();

		//Start Server thread		
		Thread serverThread = new Thread(new ServerThread(getContext()),"Server thread");
		serverThread.start();

		//TODO use this recovery thread
		//Contact recovery nodes
		recoveryMap.clear();
		for(int i=0;i<Util.contactList.size();i++) {
			RecoveryThread recovery = new RecoveryThread(Util.contactList.get(i));
			Thread recoveryThread = new Thread(recovery,"Recovery thread");
			recoveryThread.start();
			try {
				recoveryThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			List<KeyId> tempList = recovery.getRecKeyIdList();
			if(tempList != null) {
				//if it is successor
				if(i == 1) {
					List<KeyId> recvTempList = new ArrayList<KeyId>();
					String hashMyport = hashGen.genHash(Util.MY_PORT);
					String upperHash = Util.nodeList.get
							(Util.nodeList.size()-1).getNodeId();
					for(KeyId keyId1:tempList) {
						//If it is 0th node
						if(Util.nodeList.get(0).getPortNumber().equals(Util.MY_PORT)){
							//values greater than successor and less the self
							if(keyId1.getKey().compareTo(upperHash) > 0) {
								recvTempList.add(keyId1);
							}
						}
						if(keyId1.getKey().compareTo(hashMyport) < 0) {
							recvTempList.add(keyId1);
						}
					}
					recoveryMap.put(Util.contactList.get(i), recvTempList);
				}
				else {
					recoveryMap.put(Util.contactList.get(i), tempList);
				}
			}
		}
		//		if(!recoveryMap.isEmpty()) {
		//			performRecovery();
		//		}	
		//recoveryMap.clear();

		Log.d(CONTENT_PROVIDER, Util.MY_PORT);

	}

	public static void performRecovery() {

		Set<String> recKey = recoveryMap.keySet();
		Set<KeyId> keySet = new HashSet<KeyId>();
		for(String customKey:recKey) {
			List<KeyId> tempkeyList = recoveryMap.get(customKey);
			keySet.addAll(tempkeyList);
		}
		for(KeyId keyID : keySet) {
			ContentValues cv = new ContentValues();		
			String key = keyID.getKey();
			String value = keyID.getMsgFormat().getMsg();
			cv.put(ProviderMetaData.ProviderTable.PROVIDER_KEY, key);
			cv.put(ProviderMetaData.ProviderTable.PROVIDER_VALUE,value);
			//do query 
			Uri queryUri = Uri.parse("content://"+
					ProviderMetaData.AUTHORITY+ "/" +ProviderMetaData.DB_NAME+ "/"+ Util.keyMap.get(key));
			Cursor cout = providerContext.getContentResolver().query(queryUri , new String []{
					ProviderMetaData.ProviderTable.PROVIDER_KEY,
					ProviderMetaData.ProviderTable.PROVIDER_VALUE
			}, null, null, null);
			if(cout != null && cout.getCount() > 0) {
				cout.moveToFirst();
				//update the same uri
				//if same content already exist then don't update
				int colIndex =  cout.getColumnIndex(ProviderMetaData.ProviderTable.PROVIDER_VALUE);
				if(!cout.getString(colIndex).equalsIgnoreCase(value)) {
					Log.d(MY_CONTENT_PROVIDER, "got update");
					providerContext.getContentResolver().update(queryUri, cv, null, null);
				}
			}
			else {
				Uri uri = providerContext.getContentResolver().
						insert(MyContentProvider.contentUri, cv);
				Log.d(MY_CONTENT_PROVIDER,"URI :: "+uri);
			}
		}
		
		MainActivity.textView.setText("Recovered ....");
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		SQLiteDatabase db = myDBHelper.getWritableDatabase();
		int count = 0;
		count = db.delete(ProviderMetaData.ProviderTable.TABLE_NAME
				, arg1, arg2);
		return count ;
	}

	@Override
	public String getType(Uri uri) {

		switch(uriMatcher.match(uri)) {
		case TYPE_LIST:
			return ProviderMetaData.CONTENT_TYPE_LIST;

		case TYPE_ONE:
			return ProviderMetaData.CONTENT_TYPE_ONE;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}		
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = myDBHelper.getWritableDatabase();
		long rowId = 0;
		rowId = db.insert(ProviderMetaData.ProviderTable.TABLE_NAME,
				null, values);
		try {
			Thread.sleep(1*1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(rowId > 0) {
			Uri providerUri = ContentUris.withAppendedId(contentUri, 
					Long.valueOf(Util.keyMap.get(values.get(ProviderMetaData.ProviderTable.
							PROVIDER_KEY))));
			getContext().getContentResolver().notifyChange(providerUri, null); 
			return providerUri;
		}
		else {
			return null;
		}

	}

	//TODO :check @ projection map
	private static final HashMap<String, String> providerProjectionMap;
	static {
		providerProjectionMap = new HashMap<String, String>();
		providerProjectionMap.put(ProviderMetaData.ProviderTable.PROVIDER_KEY, ProviderMetaData.ProviderTable.PROVIDER_KEY);
		providerProjectionMap.put(ProviderMetaData.ProviderTable.PROVIDER_VALUE, ProviderMetaData.ProviderTable.PROVIDER_VALUE);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		SQLiteDatabase db = myDBHelper.getReadableDatabase();
		builder.setTables(ProviderMetaData.ProviderTable.TABLE_NAME);
		builder.setProjectionMap(providerProjectionMap);
		int showAll = 0;
		switch (uriMatcher.match(uri)) {
		case TYPE_LIST:
			if(sortOrder != null && sortOrder.equals(Util.ORDER)) {
				showAll = 1;
			}else if(sortOrder.equals(Util.DISTRIBUTED_GET)){
				showAll = 2;
			}
			else if(sortOrder.equals(Util.DISTRIBUTED_RECOVER)){
				showAll = 3;
			}
			break;
		case TYPE_ONE:
			builder.appendWhere(ProviderMetaData.ProviderTable.PROVIDER_KEY
					+" = '"+hashGen.genHash(uri.getPathSegments().get(1)) +"'");
			Log.d(MY_CONTENT_PROVIDER, "Queried for :"+uri.getPathSegments().get(1));
			break;

		default:
			break;
		}

		Cursor queryCursor = builder.query(db, projection, 
				selection, selectionArgs, null, null, null);
		//		queryCursor.moveToPosition(0);

		queryCursor.setNotificationUri(getContext().getContentResolver(), uri);

		if(showAll == 1) {
			MainActivity.textView.setText("");
			String out = "";
			if(queryCursor != null && queryCursor.getCount()!= 0) {
				//queryCursor.moveToFirst();
				while (queryCursor.moveToNext()) {
					out += "<";
					out += Util.keyMap.get(queryCursor.getString(queryCursor.getColumnIndexOrThrow(
							ProviderMetaData.ProviderTable.PROVIDER_KEY)));
					out+= "\t,";
					out += queryCursor.getString(queryCursor.getColumnIndexOrThrow(
							ProviderMetaData.ProviderTable.PROVIDER_VALUE));
					out+= ">\n";
					MainActivity.textView.append(out);
					out = "";
				}
			}
		}
		else if(showAll == 2){
			//distributed get
			queryAll();
			displayAllQuery();
		}


		return queryCursor;
	}



	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		int count = 0;
		switch (uriMatcher.match(uri)) {
		case TYPE_LIST:
			//Start key-id insertion
			generateKeyHash();
			retryMap.clear();
			for(int i=0;i<keyIdList.size();i++) {
				String input = String.valueOf(i);
				KeyId aKeyId = keyIdList.get(i);
				List<String> portList = Util.CoOrdinatorMap.get(input);
				DHTThread dht = new DHTThread(getContext(),
						aKeyId,Integer.valueOf(portList.get(0)),
						null,DHTThreadType.INSERT);
				Thread dhtThread = new Thread((dht),"DHT thread");
				dhtThread.start();		
				try {
					dhtThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(dht.isGotQuorum() == false) {
					Log.d(MY_CONTENT_PROVIDER, "failed to get quorum");
					retryMap.put(input, aKeyId);
				}
				else {
					Log.d(MY_CONTENT_PROVIDER, "able to get quorum");
				}

			}
			//Retry after failure
			if(!retryMap.isEmpty()) {
				Set<String> aKeySet = retryMap.keySet();
				for(String key :aKeySet) {
					KeyId bkeyId = retryMap.get(key);
					bkeyId.getMsgFormat().setMsgType(MsgType.RETRY);
					List<String> portList = Util.CoOrdinatorMap.get(key);
					DHTThread dht = new DHTThread(getContext(),
							bkeyId,Integer.valueOf(portList.get(1)),
							null,DHTThreadType.INSERT);
					Thread dhtThread = new Thread((dht),"DHT thread");
					dhtThread.start();		
					try {
						dhtThread.join();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
			break;
		case TYPE_ONE :
			//sleep for 1 sec
			SQLiteDatabase db = myDBHelper.getWritableDatabase();
			db.update(ProviderMetaData.ProviderTable.TABLE_NAME, values,
					ProviderMetaData.ProviderTable.PROVIDER_KEY + " = '"+ 
							hashGen.genHash(uri.getPathSegments().get(1)) +"'", 
							selectionArgs);
			break;
		default:
			break;
		}
		return count;
	}


	private void displayAllQuery() {
		String out = "";	
		MainActivity.textView.setText("");
		for(List<Quorum>qList : queryQuorumList) {

			if(qList.size() >= Util.READ_QUORAM) {
				//TODO :Verify all votes in quorum
				out += "<";
				out += Util.keyMap.get(qList.get(0).getKeyId().getKey());
				out+= "\t,";
				out += qList.get(0).getKeyId().getMsgFormat().getMsg();
				out+= ">\n";
				MainActivity.textView.append(out);
				out = "";

			}

		}

	}


	private void queryAll() {
		retryQueryMap.clear();
		queryQuorumList.clear();
		GeneralMsgFormat msgFormat;
		for(int i=0;i<Util.TOTAL_KEYS;i++) {
			String input = String.valueOf(i);
			msgFormat = new GeneralMsgFormat(MsgType.QUERY, hashGen.genHash(input));
			List<String> portList = Util.CoOrdinatorMap.get(input);
			DHTThread dht = new DHTThread(getContext(),
					null,Integer.valueOf(portList.get(0)),
					msgFormat,DHTThreadType.QUERY);
			Thread dhtThread = new Thread((dht),"DHT thread");
			dhtThread.start();		
			Log.d(CONTENT_PROVIDER,"query originated from :"+Util.MY_PORT);
			try {
				dhtThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(dht.isGotQuorum() == false) {
				Log.d(MY_CONTENT_PROVIDER, "failed to get quorum");
				retryQueryMap.put(input, msgFormat);
			}
			else {
				Log.d(MY_CONTENT_PROVIDER, "Able to get Query qurum:"+dht.getQuorumList());
				queryQuorumList.add(dht.getQuorumList());
			}

		}
		//Retry after failure
		if(!retryMap.isEmpty()) {
			Set<String> aKeySet = retryQueryMap.keySet();
			for(String key :aKeySet) {
				GeneralMsgFormat bMsgFormat = retryQueryMap.get(key);
				bMsgFormat.setMsgType(MsgType.RETRY_QUERY);
				List<String> portList = Util.CoOrdinatorMap.get(key);
				DHTThread dht = new DHTThread(getContext(),
						null,Integer.valueOf(portList.get(1)),
						bMsgFormat,DHTThreadType.QUERY);
				Thread dhtThread = new Thread((dht),"DHT thread");
				dhtThread.start();		
				try {
					dhtThread.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Log.d(MY_CONTENT_PROVIDER, "Able to get Retry Query qurum:"+dht.getQuorumList());
				queryQuorumList.add(dht.getQuorumList());
			}

		}


	}

	private void generateKeyHash() {
		int i = 0;
		keyIdList = new ArrayList<KeyId>();
		keyIdList.clear();
		switch(INSERT) {
		case 10:
			for(i = 0;i< Util.TOTAL_KEYS ;i++) {
				String input = String.valueOf(i);
				keyIdList.add(
						new KeyId(hashGen.genHash(input)
								,new GeneralMsgFormat(MsgType.INSERT,"PUT1"+input)));
			}
			break;
		case 20:
			for(i = 0;i < Util.TOTAL_KEYS ;i++) {
				String input = String.valueOf(i);
				keyIdList.add(
						new KeyId(hashGen.genHash(input)
								,new GeneralMsgFormat(MsgType.INSERT,"PUT2"+input)));
			}	

			break;
		case 30:
			for(i = 0; i< Util.TOTAL_KEYS ;i++) {
				String input = String.valueOf(i);
				keyIdList.add(
						new KeyId(hashGen.genHash(input)
								,new GeneralMsgFormat(MsgType.INSERT,"PUT3"+input)));			}

			break;
		}
	}

	/**
	 * Database helper class to add,delete,update query
	 * @author vikram
	 *
	 */
	private static class MyDBHelper extends SQLiteOpenHelper {


		public MyDBHelper(Context localContext) {
			super(localContext, ProviderMetaData.DB_NAME, null, ProviderMetaData.DB_VERSION);

		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DROP_QUERY);
			db.execSQL(CREATE_QUERY);
			Log.d(MY_CONTENT_PROVIDER, "database created");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(DROP_QUERY);
			onCreate(db);
		}

	}


}
