package com.ub.buffalo;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.ub.buffalo.GeneralMsgFormat.MsgType;
import com.ub.buffalo.ReplicationThread.ReplicationType;

/**
 * 
 * @author vikram
 *
 */
public class AcceptThread implements Runnable{

	private KeyId localKeyId;
	private GeneralMsgFormat localMsgFormat;
	private List<Quorum> quorumList = new ArrayList<Quorum>();
	private ObjectOutputStream serverOut;
	private Object line;
	private String ACCEPT_THREAD = "Accept thread";
	private Context context;
	private Quorum quorum = null;
	private Socket clientSocket;
	private ObjectInputStream in;
	public AcceptThread() {

	}

	public AcceptThread(Socket aSocket,Context aContext) {
		clientSocket = aSocket;
		context = aContext;
	}

	public void run() {
		try {
			in = new ObjectInputStream(clientSocket.getInputStream());
			serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
			line = new Object();
			Log.d(ACCEPT_THREAD, "client info :"+clientSocket.getLocalPort()
					+"normal port "+clientSocket.getPort());
			while((line = in.readObject()) != null) {
				if(line instanceof KeyId) {
					localKeyId = (KeyId)line;
					acceptKeyId();
				}
				else if(line instanceof GeneralMsgFormat) {
					localMsgFormat = (GeneralMsgFormat)line;
					acceptMsg();
				}
			}
			in.close();
			line = null;
		}catch (EOFException e) {
			Log.d(ACCEPT_THREAD, "EOF exception occured");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendReplicationToOther() {
		//sendDataDummyReplication();
		sendReplication();
	}

	private void sendReplication() {
//		int limit = Util.WRITE_QUORAM + 1;
		int limit = Util.WRITE_QUORAM ;
		if(localKeyId.getMsgFormat().getMsgType().equals(MsgType.RETRY)) {
			limit = Util.WRITE_QUORAM;
		}
		for(int i = 0 ;i< limit ;i++) { //Do write quoram 1 in this case
			ReplicationThread replica = new ReplicationThread(Integer.parseInt(
					Util.prefList.get(i)),localKeyId,
					null,ReplicationType.INSERT);
			Thread replicaThread = new Thread(replica,"Replication Thread");
			replicaThread.start();
			try {
				//wait for the response to come
				replicaThread.join();		
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//get the quorum //TODO :add b4 thread start
			synchronized (replica) {
				quorumList.add(replica.getQuorum());
			}
		}
	}
	
	private void sendQueryReplication() {
//		int limit = Util.READ_QUORAM + 1;
		int limit = Util.READ_QUORAM;
		if(localMsgFormat.getMsgType().equals(MsgType.RETRY_QUERY)) {
//			limit = Util.READ_QUORAM;
			limit = Util.READ_QUORAM - 1;
		}
		for(int i = 0 ;i< limit ;i++) { //Do write quoram 1 in this case
			ReplicationThread replica = new ReplicationThread(Integer.parseInt(
					Util.prefList.get(i)),null,localMsgFormat,
					ReplicationType.QUERY);
			Thread replicaThread = new Thread(replica,"Replication Thread");
			replicaThread.start();
			try {
				//wait for the response to come
				replicaThread.join();		
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//get the quorum //TODO :add b4 thread start
			synchronized (replica) {
				quorumList.add(replica.getQuorum());
			}
		}
	}


	private void InsertDataInProvider() {
		String key = localKeyId.getKey();
		String value = localKeyId.getMsgFormat().getMsg();
		ContentValues cv = new ContentValues();		
		cv.put(ProviderMetaData.ProviderTable.PROVIDER_KEY, key);
		cv.put(ProviderMetaData.ProviderTable.PROVIDER_VALUE,value);

		//do query 
		Uri queryUri = Uri.parse("content://"+
				ProviderMetaData.AUTHORITY+ "/" +ProviderMetaData.DB_NAME+ "/"+ Util.keyMap.get(key));
		Cursor cout = context.getContentResolver().query(queryUri , new String []{
				ProviderMetaData.ProviderTable.PROVIDER_KEY,
				ProviderMetaData.ProviderTable.PROVIDER_VALUE
		}, null, null, null);
		if(cout != null && cout.getCount() > 0) {
			cout.moveToFirst();
			//update the same uri
			//if same content already exist then don't update
			int colIndex =  cout.getColumnIndex(ProviderMetaData.ProviderTable.PROVIDER_VALUE);
			if(!cout.getString(colIndex).equalsIgnoreCase(value)) {
				Log.d(ACCEPT_THREAD, "got update");
				context.getContentResolver().update(queryUri, cv, null, null);
			}
		}
		else {
			Uri uri = context.getContentResolver().
					insert(MyContentProvider.contentUri, cv);
			Log.d(ACCEPT_THREAD,"URI :: "+uri);
		}

		quorum = new Quorum(localKeyId, 1);
	}

	private void acceptKeyId() {
		Log.d(ACCEPT_THREAD, "Key id "+localKeyId.getKey() + "For port "+Util.MY_PORT);
		//send request to co-ordinator 
		quorumList.clear();
		if(localKeyId.getMsgFormat().getMsgType().equals(MsgType.INSERT) ||
				localKeyId.getMsgFormat().getMsgType().equals(MsgType.RETRY)) {
			//			populatePrefList();
			sendReplicationToOther();
			InsertDataInProvider();
			//send message to 
			try {
				quorumList.add(quorum);
				serverOut.writeObject(quorumList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		else if(localKeyId.getMsgFormat().getMsgType().equals(MsgType.REPLICATE)) {
			InsertDataInProvider();
			try {
				serverOut.writeObject(quorum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	private void queryDataProvider() {
		String key = localMsgFormat.getMsg();
		KeyId qKeyId;
		Uri queryUri = Uri.parse("content://"+
				ProviderMetaData.AUTHORITY+ "/" +ProviderMetaData.DB_NAME+ "/"+ 
				Util.keyMap.get(key));
		Cursor cout = context.getContentResolver().query(queryUri , new String []{
				ProviderMetaData.ProviderTable.PROVIDER_KEY,
				ProviderMetaData.ProviderTable.PROVIDER_VALUE
		}, null, null, null);
		if(cout != null && cout.getCount() > 0) {
			cout.moveToFirst();
			//update the same uri
			//if same content already exist then don't update
			int colIndex =  cout.getColumnIndex(ProviderMetaData.ProviderTable.PROVIDER_VALUE);
			String amsg = cout.getString(colIndex);
			qKeyId  = new KeyId(key, new GeneralMsgFormat(MsgType.QUERY,
					amsg));
			quorum = new Quorum(qKeyId, 1);
		}
	}

	private void acceptMsg() {
		quorumList.clear();
		if(localMsgFormat.getMsgType().equals(MsgType.QUERY) || 
				localMsgFormat.getMsgType().equals(MsgType.RETRY_QUERY)) {
			sendQueryReplication();
			queryDataProvider();
			//send message to 
			try {
				quorumList.add(quorum);
				serverOut.writeObject(quorumList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
		else if(localMsgFormat.getMsgType().equals(MsgType.REPLICATE_QUERY)) {
			queryDataProvider();
			try {
				serverOut.writeObject(quorum);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		else if(localMsgFormat.getMsgType().equals(MsgType.RECOVER)) {
			//dump all data
			List<KeyId> keyIdList = new ArrayList<KeyId>();
			Cursor cursor = context.getContentResolver().query(MyContentProvider.contentUri,new String []{
					ProviderMetaData.ProviderTable.PROVIDER_KEY,
					ProviderMetaData.ProviderTable.PROVIDER_VALUE
			},null, null, Util.DISTRIBUTED_RECOVER);

			//send all data to other
			if(cursor != null && cursor.getCount() != 0) {
				while (cursor.moveToNext()) {
					keyIdList.add(new KeyId(cursor.getString(cursor.getColumnIndexOrThrow(
							ProviderMetaData.ProviderTable.PROVIDER_KEY))
							,new GeneralMsgFormat(MsgType.RECOVER,cursor.getString
									(cursor.getColumnIndexOrThrow(ProviderMetaData.
											ProviderTable.PROVIDER_VALUE)))));
				}
			}		
			try {
				serverOut.writeObject(keyIdList);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
}
