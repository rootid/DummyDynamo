package com.ub.buffalo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import android.content.Context;
import android.util.Log;

/**
 * 
 * @author vikram
 *
 */
public class ServerThread implements Runnable{

	private ServerSocket serverSocket = null;
	private Socket clientSocket;
	private String SERVER_THREAD = "Server thread";
	private Context context;
	public ServerThread() {

	}
	public ServerThread(Context aContext) {
		context = aContext;
	}

	public void run() {
		try {
			serverSocket = new ServerSocket(Util.SERVER_PORT);

			if(serverSocket != null) {
				try {
					while(true) {
						clientSocket = serverSocket.accept();
						//Accept thread
						Thread acceptThread = new Thread(new AcceptThread(clientSocket,context),"Accept thread");
						acceptThread.start();
						try {
							acceptThread.join();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					Log.d(SERVER_THREAD, "Could not connect to the client");
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
