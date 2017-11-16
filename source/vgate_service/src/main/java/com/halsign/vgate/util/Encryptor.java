package com.halsign.vgate.util;

import org.apache.commons.codec.binary.Base64;

public class Encryptor {

	public static String encrypt(String texto){
		 return new String(Base64.encodeBase64(texto.getBytes()));
	}

	public static String decrypt(String texto) {
		return new String(Base64.decodeBase64(texto.getBytes()));
	}
}
