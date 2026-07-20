package com.stilog.prism.comparevpi.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MethodUtil {

	public static String decodeBase64(String encoded) {
	    try {
	        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
	        return new String(decodedBytes, StandardCharsets.UTF_8);
	    } catch (IllegalArgumentException e) {
	    	e.printStackTrace();
	        return null; // ou throw new RuntimeException("Base64 invalide");
	    }
	}
}
