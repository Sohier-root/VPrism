package com.stilog.prism.vpimodel.reader;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.stilog.prism.comparevpi.utils.MethodUtil;

/**
 * Ré-extrait, à partir du XML brut d'une entité ({@code Entity.getAssociatedXml()}),
 * les fragments XML des nœuds répétés (headings, niveaux de hiérarchie), afin de
 * continuer à alimenter {@code Parameters.initialXml}/{@code associatedXml} — requis
 * par {@code Entity.generateXml()} pour la fusion — même quand les *valeurs* affichées
 * proviennent désormais de VPIReader plutôt que de ce même parsing DOM.
 */
public class RawFragmentIndexer {

	private RawFragmentIndexer() {
	}

	/** Fragments XML des enfants directs du premier nœud {@code containerTag}, dans l'ordre du document. */
	public static List<String> fragmentsByOrder(String entityXml, String containerTag) {
		List<String> fragments = new ArrayList<>();
		Element container = findContainer(entityXml, containerTag);
		if (container == null)
			return fragments;

		NodeList children = container.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			fragments.add(MethodUtil.nodeToString(child));
		}
		return fragments;
	}

	/**
	 * Fragments XML des enfants directs du premier nœud {@code containerTag}, indexés par le
	 * contenu texte du premier descendant nommé {@code idTag} de chaque enfant.
	 */
	public static Map<String, String> fragmentsById(String entityXml, String containerTag, String idTag) {
		Map<String, String> fragments = new LinkedHashMap<>();
		Element container = findContainer(entityXml, containerTag);
		if (container == null)
			return fragments;

		NodeList children = container.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element childElement = (Element) child;
			NodeList idNodes = childElement.getElementsByTagName(idTag);
			if (idNodes.getLength() == 0)
				continue;
			fragments.put(idNodes.item(0).getTextContent(), MethodUtil.nodeToString(child));
		}
		return fragments;
	}

	private static Element findContainer(String entityXml, String containerTag) {
		try {
			Document doc = parse(entityXml);
			NodeList containers = doc.getElementsByTagName(containerTag);
			return containers.getLength() > 0 ? (Element) containers.item(0) : null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static Document parse(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}
}
