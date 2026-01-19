package com.stilog.analysevpi.utils;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public class MethodUtil {

	/**
	 * Transforme un Node (xml) en String 
	 * @param node
	 * @return
	 * @throws Exception
	 */
	public static String nodeToString(Node node) throws Exception {
	    TransformerFactory tf = TransformerFactory.newInstance();
	    Transformer transformer = tf.newTransformer();

	    // Options de formatage (facultatif)
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	    StringWriter writer = new StringWriter();
	    transformer.transform(new DOMSource(node), new StreamResult(writer));

	    return writer.toString().trim();
	}
}
