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

import android.util.Log;

import com.ub.buffalo.GeneralMsgFormat.MsgType;

public class RecoveryThread implements Runnable{

	private static final String RECOVERY_THREAD = "Recovery Thread";
	private String contactPort;
	private GeneralMsgFormat msgFormat;
	private List<KeyId> recKeyIdList;
	private String myPort = String.valueOf(Integer.parseInt(Util.MY_PORT) * 2);
	public RecoveryThread(String aPort) {
		contactPort = aPort;
		msgFormat = new GeneralMsgFormat(MsgType.RECOVER, myPort);
	}

	public void run() {
		try {
			Socket socket = new Socket();
			SocketAddress remoteAddr;
			try {
				remoteAddr = new InetSocketAddress(Util.INET_ADDRESS, 
						Integer.parseInt(contactPort));
				socket.connect(remoteAddr, Util.TIME_OUT);
				Log.d(RECOVERY_THREAD, "send to port : "+contactPort);
				OutputStream os = socket.getOutputStream();
				InputStream ips = socket.getInputStream();
				ObjectOutputStream out = new ObjectOutputStream(os);
				ObjectInputStream in = new ObjectInputStream(ips);
				Object line = new Object();
				out.writeObject(msgFormat);
				try {
					line = in.readObject();
					recKeyIdList = (List<KeyId>) line;
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				out.close();
				os.close();
				socket.close();

			}catch(SocketTimeoutException e) {
				Log.d(RECOVERY_THREAD, "Time out during recovery occured");
			}
			catch(ConnectException e) {
				Log.d(RECOVERY_THREAD, "Unable to connect to port : "+contactPort);
			}
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

	public List<KeyId> getRecKeyIdList() {
		return recKeyIdList;
	}

}
