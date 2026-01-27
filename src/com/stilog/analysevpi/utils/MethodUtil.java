package com.stilog.analysevpi.utils;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public class MethodUtil {

	/**
	 * Transforme un Node (xml) en String
	 * 
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public static String nodeToString(Node node) {
		StringWriter writer = new StringWriter();
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();

			// Options de formatage (facultatif)
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			transformer.transform(new DOMSource(node), new StreamResult(writer));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return writer.toString().trim();
	}
	
	public static String decodeBase64(String encoded) {
	    try {
	        byte[] decodedBytes = Base64.getDecoder().decode(encoded);
	        return new String(decodedBytes, StandardCharsets.UTF_8);
	    } catch (IllegalArgumentException e) {
	    	e.printStackTrace();
	        return null; // ou throw new RuntimeException("Base64 invalide");
	    }
	}
	
	public static String encodeBase64(String decoded) {
	    try {
	    	byte[] decodedBytes = decoded.getBytes();
	    	byte[] encoded = Base64.getEncoder().encode(decodedBytes);
	        return new String(encoded, StandardCharsets.UTF_8);
	    } catch (IllegalArgumentException e) {
	    	e.printStackTrace();
	        return null; // ou throw new RuntimeException("Base64 invalide");
	    }
	}
}
