package com.ub.buffalo;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

import android.content.Context;
import android.util.Log;

/**
 * This thread in responsible for DHT insert and query
 * @author vikram
 *
 */
public class DHTThread implements Runnable{

	private static final String DHT_THREAD = "DHT thread";
	private KeyId keyId;
	private ObjectOutputStream out;
	private int contactPort;
	private boolean isGotQuorum = false;
	private List<Quorum> quorumList = null;
	private DHTThreadType threadType;
	private GeneralMsgFormat msgFormat;
	
	public enum DHTThreadType {
		QUERY,
		INSERT
	}

//	public DHTThread(Context aContext,KeyId aKeyId,int port) {
//		keyId = aKeyId;
//		contactPort = port;
//	}

	public DHTThread(Context aContext,KeyId aKeyId,int port,
			GeneralMsgFormat amsgFormat,
			DHTThreadType aThreadType) {
		keyId = aKeyId;
		contactPort = port;
		threadType = aThreadType;
		msgFormat = amsgFormat;
	}


	private void sendToCoOrdinator() {
		Socket socket = new Socket();
		SocketAddress remoteAddr;
		try{
			socket = new Socket();
			remoteAddr = new InetSocketAddress(Util.INET_ADDRESS,contactPort);
			socket.connect(remoteAddr, Util.TIME_OUT);				
			OutputStream os = socket.getOutputStream();
			out = new ObjectOutputStream(os);
			InputStream inp = socket.getInputStream();			
			ObjectInputStream in = new ObjectInputStream(inp);
			Object line = new Object();
			if(threadType.equals(DHTThreadType.INSERT)) {
				out.writeObject(keyId);
			}
			else if(threadType.equals(DHTThreadType.QUERY)) {
				out.writeObject(msgFormat);
			}
			//Log.d(DHT_THREAD, "Key id "+ keyId.getKey() + "Sent To For port "+ prefList.get(0));
			//wait for timeout 
			try {
				line = in.readObject();
				quorumList = (List<Quorum>) line;
				//				Log.d(DHT_THREAD, "GOT quorum" +line.toString());
				Log.d(DHT_THREAD, "GOT quorum" +quorumList.size());
				if(quorumList.size() >= Util.WRITE_QUORAM) {
					//successful write
					isGotQuorum =  true;
				}
				else {
					//unsuccessful write

				}

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			out.close();
			os.close();
			inp.close();
			in.close();
			socket.close();
		} catch (ConnectException e) {
			Log.d(DHT_THREAD, "Socket down unable to insert");
		}catch (SocketTimeoutException e) {
			Log.d(DHT_THREAD, "time out exception");
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

	public void run() {
		sendToCoOrdinator();
	}


	public boolean isGotQuorum() {
		return isGotQuorum;
	}

	public List<Quorum> getQuorumList() {
		return quorumList;
	}
	
}
