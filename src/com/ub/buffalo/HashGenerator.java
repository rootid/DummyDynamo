package com.ub.buffalo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import android.util.Log;


/**
 * singleton class to generate hash function
 * @author vikram
 *
 */
final class HashGenerator {

	private static HashGenerator hashGenerator = null;
	private MessageDigest msgDigest;
	private static final String HASH_GENERATOR = "hash Generator";
	
	/**
	 * 
	 * @param algorithm
	 * @return
	 */
	public static HashGenerator getHashInstance(String algorithm) {
		if(hashGenerator == null) {
			try {
				hashGenerator = new HashGenerator(MessageDigest.getInstance(algorithm));
			} catch (NoSuchAlgorithmException e) {

				e.printStackTrace();
			}
		}
		return hashGenerator;
	}

	/**
	 * 
	 * @param aMsgDigest
	 */
	private HashGenerator(MessageDigest aMsgDigest) {
		if(aMsgDigest == null) {
			Log.d(HASH_GENERATOR, "Please provide the digest parameter");
		}
		else {
			this.msgDigest = aMsgDigest;
		}
	}

	/**
	 * 
	 * @param input : given any input generate hash value
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	final String genHash(String input){
		byte[] sha1Hash = msgDigest.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}


}
