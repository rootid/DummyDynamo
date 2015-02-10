package com.ub.buffalo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.telephony.TelephonyManager;


public final class Util {
	
	public static String getlineNumber(Context context) {
		TelephonyManager tmgr=(TelephonyManager)context.getSystemService
				(Context.TELEPHONY_SERVICE) ;
		
		String tempValue = tmgr.getLine1Number();
		String portStr = tempValue.substring(tempValue.length() - 4);
		return portStr;		
	}
	
	public static final String ALGORITHM_TYPE = "SHA-1";
	public static final int SERVER_PORT = 10000;
	public static String MY_PORT = "";
	public static final int TIME_OUT = 100;
	public static final String DELI_REQ_CO = "::";
	public static final int TOTAL_KEYS = 10;
	public static Map<String,String> keyMap;
	public static List<RoutingTable> routingList;
	public static List<String> contactList;
	public static List<Node> nodeList;
	public static final int WRITE_QUORAM = 1; //1 in case 3 emulators and 2 for 5 emulators
	public static final int READ_QUORAM = 1;
	public static int START = 5554;
	public static int END = 5560; //5564 for 5 emulator 5560 for 3 emulators
	public static String INET_ADDRESS = "10.0.2.2";
	public static final String ORDER = "DISP";
	public static final String DISTRIBUTED_RECOVER = "Recover";
	public static final String DISTRIBUTED_GET = "Distributed get";
	
	public List<KeyId> keyIdList;
	public static Map<String,List<String>> CoOrdinatorMap = 
			new HashMap<String, List<String>>();
	public static List<String> prefList = new ArrayList<String>();
	public static int[] tokenizeString(String line)
	{
		String[] tokens;	
		String delims = "#";
		int index = 0;
		tokens = line.split(delims);
		int[] noTokens = new int[tokens.length]; 
		for (String str:tokens) {
			noTokens[index] = Integer.parseInt(str);
			index ++;
		}
		return noTokens;
	}
	

}
