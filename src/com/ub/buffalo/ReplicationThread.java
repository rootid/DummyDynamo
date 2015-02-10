package com.ub.buffalo;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import android.util.Log;
import com.ub.buffalo.GeneralMsgFormat.MsgType;

/**
 * 
 * @author vikram
 *
 */
public class ReplicationThread implements Runnable {

	private static final String REPLICATION_THREAD = "Replication Thread";
	private int contactPort;
	private Socket socket;
	private KeyId keyId;
	private ObjectOutputStream out;
	private Quorum quorum = null;
	private ReplicationType replicaType;
	private GeneralMsgFormat msgFormat;
	public enum ReplicationType {
		INSERT,
		QUERY
	}
	public ReplicationThread() {

	}

//	public ReplicationThread(int destPort, KeyId akeyId) {
//		contactPort = destPort;
//		keyId = akeyId;
//	}

	public ReplicationThread(int destPort, KeyId akeyId,GeneralMsgFormat amsgFormat,
			ReplicationType aReplicaType) {
		contactPort = destPort;
		keyId = akeyId;
		replicaType = aReplicaType;
		msgFormat = amsgFormat;
	}

	public void run() {
		try {
			// TODO : check if it closed or not
			socket = new Socket(Util.INET_ADDRESS, contactPort);
			Log.d(REPLICATION_THREAD, "send to port : " + contactPort +
					" from port: "+Util.MY_PORT);
			OutputStream os = socket.getOutputStream();
			out = new ObjectOutputStream(os);
			InputStream inp = socket.getInputStream();
			ObjectInputStream in = new ObjectInputStream(inp);
			Object line = new Object();
			if(replicaType.equals(ReplicationType.INSERT)) {
				KeyId lKeyId = new KeyId(keyId.getKey(), new GeneralMsgFormat(
						MsgType.REPLICATE, keyId.getMsgFormat().getMsg()));
				out.writeObject(lKeyId);
			}
			else if(replicaType.equals(ReplicationType.QUERY)){
				msgFormat.setMsgType(MsgType.REPLICATE_QUERY);
				out.writeObject(msgFormat);
			}
			try {
				line = in.readObject();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Log.d(REPLICATION_THREAD, line.toString());
			quorum = (Quorum)line;
			//quorum = new Quorum(lKeyId, 1);
			out.close();
			os.close();
			socket.close();
		} catch (SocketTimeoutException e) {
			Log.d(REPLICATION_THREAD, "Time out during recovery occured");
		} catch (ConnectException e) {
			// If emulator crashed during replication
			Log.d(REPLICATION_THREAD, "Unable to connect to port : "
					+ contactPort);
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

	public Quorum getQuorum() {
		return quorum;
	}
}
