package com.ub.buffalo;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.ub.buffalo.GeneralMsgFormat.MsgType;

/**
 * This class acts as background server
 * @author vikram
 *
 */
public class AcceptTask extends AsyncTask<MySocket, String , String>{

	public static final String SERVER_TASK_TAG = "server task";
	private ServerSocket serverSocket = null;
	private Socket clientSocket = null;
	private TextView text = null;
	private Object line;
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;
	private ObjectOutputStream serverOut = null;
	private GeneralMsgFormat msgFormat ;
	private Context context;
	private List<String> prefList;
	private KeyId localKeyId;
	private String [] tokens;
	public AcceptTask(Handler handler,TextView txt,Context acontext) {
		try {
			serverSocket = new ServerSocket(Util.SERVER_PORT);
			this.text = txt;
			this.context = acontext;

		} catch (IOException e) {
			Log.d(SERVER_TASK_TAG,"Failed to create the server socket");
			e.printStackTrace();
		}
	}

	public AcceptTask(Context acontext) {
		try {
			serverSocket = new ServerSocket(Util.SERVER_PORT);
			this.context = acontext;

		} catch (IOException e) {
			Log.d(SERVER_TASK_TAG,"Failed to create the server socket");
			e.printStackTrace();
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected String doInBackground(MySocket... mySockets) {

		if(serverSocket != null) {
			try {
				while(true) {

					clientSocket = serverSocket.accept();
					//clientSocket.setSoTimeout(Util.TIME_OUT);
					in = new ObjectInputStream(clientSocket.getInputStream());
					serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
					line = new Object();
					Log.d(SERVER_TASK_TAG, "client info :"+clientSocket.getLocalPort()
							+"normal port "+clientSocket.getPort());
					try {
						int count = 0;
						while((line = in.readObject()) != null) {
							if(line instanceof KeyId) {
								localKeyId = (KeyId)line;
								acceptKeyId();
							}
							else if(line instanceof GeneralMsgFormat) {
								acceptMsg((GeneralMsgFormat)line);
							}
						}
						in.close();
						line = null;

					}catch (EOFException e) {
						Log.d(SERVER_TASK_TAG, "EOF exception occured");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			} catch (IOException e) {
				Log.d(SERVER_TASK_TAG, "Could not connect to the client");
				e.printStackTrace();
			}
		}

		return "Server connected";
	}

	private void sendDataReplication() {

		for(String str: prefList) {
			try {
				//TODO : check if it closed or not
				Socket socket = new Socket(Util.INET_ADDRESS,Integer.parseInt(str));
				KeyId lKeyId = new KeyId(tokens[1], 
						new GeneralMsgFormat(MsgType.INSERT,tokens[2]));
				OutputStream os = socket.getOutputStream();
				out = new ObjectOutputStream(os);
				out.writeObject(lKeyId);
				out.close();
				os.close();
				socket.close();

			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}


	private void sendRequestToCoOrdinator() {
		GeneralMsgFormat msgFormat = 
				new GeneralMsgFormat();
		msgFormat.setMsgType(MsgType.REPLICATE);
		msgFormat.setMsg(localKeyId.getKey()+ Util.DELI_REQ_CO+
				Integer.parseInt(Util.MY_PORT)*2 + Util.DELI_REQ_CO +
				localKeyId.getMsgFormat().getMsg());
		try {
			//TODO : check if it closed or not
			Socket socket = new Socket(Util.INET_ADDRESS,5554*2);
			OutputStream os = socket.getOutputStream();
			out = new ObjectOutputStream(os);
			out.writeObject(msgFormat);
			out.close();
			os.close();
			socket.close();

		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void sendReplicationToOther() {
		sendDataDummyReplication();
	}

	private void sendDataDummyReplication() {

		for(int i= 0;i< Util.WRITE_QUORAM + 1;i++) {		//Do write quoram 1 in this case
			try {
				//TODO : check if it closed or not
				Socket socket = new Socket(Util.INET_ADDRESS,Integer.parseInt(
						prefList.get(i)));
				Log.d(SERVER_TASK_TAG, "send to port : "+prefList.get(i));
				KeyId lKeyId = new KeyId(localKeyId.getKey(), 
						new GeneralMsgFormat(MsgType.REPLICATE,localKeyId.getMsgFormat().getMsg()));
				OutputStream os = socket.getOutputStream();
				out = new ObjectOutputStream(os);
				out.writeObject(lKeyId);
				out.close();
				os.close();
				socket.close();
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
				Log.d(SERVER_TASK_TAG, "got update");
				context.getContentResolver().update(queryUri, cv, null, null);
			}
		}
		else {
			Uri uri = context.getContentResolver().
					insert(MyContentProvider.contentUri, cv);
			Log.d(SERVER_TASK_TAG,"URI :: "+uri);
		}

	}
	private void acceptKeyId() {
		Log.d(SERVER_TASK_TAG, "Key id "+localKeyId.getKey() + "For port "+Util.MY_PORT);
		//send request to co-ordinator 
		//sendRequestToCoOrdinator();
		if(!localKeyId.getMsgFormat().getMsgType().equals(MsgType.REPLICATE)) {
			populatePrefList();
			sendReplicationToOther();
		}
		InsertDataInProvider();
		
		//send message to 
		try {
			serverOut.writeObject(new String("ACK"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void populatePrefList() {
		//check wheather it is co-ordinator
		int nbrIndex = 0;
		prefList = new ArrayList<String>();
		prefList.clear();
		String myPort = String.valueOf(Integer.parseInt(Util.MY_PORT) *2);
		if(localKeyId.getMsgFormat().getMsgType().equals(MsgType.INSERT)) {
			prefList.add(myPort);
		}		
		for(Node node:Util.nodeList) {
			//update nbrIndex
			if(node.getPortNumber().equalsIgnoreCase(myPort)) {		
				break;
			}
			nbrIndex++;		
		}

		for(int i = 0;i < Util.WRITE_QUORAM + 1;i++) {
			nbrIndex += 1;
			if(nbrIndex > Util.nodeList.size()-1) {
				nbrIndex = 0;
			}		
			prefList.add(Util.nodeList.get(nbrIndex).getPortNumber());		
			//	Log.d(SERVER_TASK_TAG,"pref list size :"+prefList.size()+"with port number :"+tokens[1]);
		}


	}
	private void acceptMsg(GeneralMsgFormat gMsgFormat) {

		List<KeyId> keyIdList = new ArrayList<KeyId>();
		if(gMsgFormat.getMsgType().equals(MsgType.RECOVER)) {
			//dump all data
			Cursor cursor = context.getContentResolver().query(MyContentProvider.contentUri,new String []{
					ProviderMetaData.ProviderTable.PROVIDER_KEY,
					ProviderMetaData.ProviderTable.PROVIDER_VALUE
			},null, null, null);

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


		}

		Log.d(SERVER_TASK_TAG, "message :"+gMsgFormat.getMsg());
		//select 2 nodes from pref list
		//make the pref list of 3
		int nbrIndex = 0;
		prefList = new ArrayList<String>();
		prefList.clear();
		String msg = gMsgFormat.getMsg();
		tokens = msg.split(Util.DELI_REQ_CO);
		for(Node node:Util.nodeList) {
			//update nbrIndex
			if(node.getPortNumber().equalsIgnoreCase(tokens[1])) {		
				break;
			}
			nbrIndex++;		
		}

		for(int i = 1;i < Util.WRITE_QUORAM + 1;i++) {
			nbrIndex += 1;
			if(nbrIndex > Util.nodeList.size()-1) {
				nbrIndex = 0;
			}		
			prefList.add(Util.nodeList.get(nbrIndex).getPortNumber());		
			Log.d(SERVER_TASK_TAG,"pref list size :"+prefList.size()+"with port number :"+tokens[1]);
		}

		//send data
		sendDataReplication();
	}
}
