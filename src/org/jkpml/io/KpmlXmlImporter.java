package org.jkpml.io;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jkpml.dto.SflComplementCondition;
import org.jkpml.dto.SflEntryCondition;
import org.jkpml.dto.SflFeature;
import org.jkpml.dto.SflFeatureCondition;
import org.jkpml.dto.SflGate;
import org.jkpml.dto.SflIntersectionCondition;
import org.jkpml.dto.SflResource;
import org.jkpml.dto.SflStatement;
import org.jkpml.dto.SflSystem;
import org.jkpml.dto.SflUnionCondition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An importer of KpmlLisp resources
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class KpmlXmlImporter implements ResourceImporter {

	private static final String SYSTEM = "SYSTEM".intern();
	private static final String GATE = "GATE".intern();

	/**
	 * The file filter
	 */
	private final static FilenameFilter filter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.split("[.]").length == 4 && name.endsWith(".system.kpml.xml");
		}
	};

	private static final String EMPTY = "".intern();

	/**
	 * Files
	 */
	private final File[] files;

	/**
	 * An xPath.
	 */
	private final XPath xPath;

	/**
	 * Constructor
	 * 
	 * @param resourceDirectory the resource directory
	 */
	public KpmlXmlImporter(File resourceDirectory) {
		files = resourceDirectory.listFiles(filter);
		xPath = XPathFactory.newInstance().newXPath();
	}

	public final SflResource importResource() {
		SflResource resource = new SflResource();
		for (File file : files) {
			importResource(resource, file); 
		}
		return resource;
	}

	private final void importResource(SflResource resource, File file) {
		try {
			importResourcesUnsafe(resource, file);
		} catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private final void importResourcesUnsafe(SflResource resource, File file) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(file);

		// Make root feature
		// FIXME
		SflFeature rootFeature = resource.makeFeature("wording");
		rootFeature.active = true;
		resource.setRootFeature(rootFeature);

		// Make network nodes
		String region = file.getName().split("[.]")[0];
		NodeList networkNodeNodes = (NodeList) xPath.evaluate("/SYSTEMS/SYSTEM", document, XPathConstants.NODESET);
		for (int i = 0; i < networkNodeNodes.getLength(); i++) {
			makeNetworkNode(resource, (Element)networkNodeNodes.item(i), region);
		}
	}

	private final void makeNetworkNode(SflResource resource, Element elm, String region) throws XPathExpressionException {
		String type = xPath.evaluate("SYSTEM-TYPE/text()", elm).intern();
		if (type == GATE) {
			makeGate(resource, elm, region);
		} else if (type == SYSTEM) {
			makeSystem(resource, elm, region);
		}
	}

	private final void makeGate(SflResource resource, Element elm, String region) throws XPathExpressionException {
		String name = xPath.evaluate("NAME", elm);
		SflGate gate = resource.makeGate(name);
		gate.region = region.intern();
		Element entryConditionElm = (Element)xPath.evaluate("INPUTS/*", elm, XPathConstants.NODE);
		gate.entryCondition = makeEntryCondition(resource, entryConditionElm);
		Element featureElm = (Element)xPath.evaluate("OUTPUTS/out", elm, XPathConstants.NODE);
		gate.feature = makeFeature(resource, featureElm);
	}

	private final void makeSystem(SflResource resource, Element elm, String region) throws XPathExpressionException {
		String name = xPath.evaluate("NAME", elm);
		SflSystem system = resource.makeSystem(name);
		system.region = region.intern();
		Element entryConditionElm = (Element)xPath.evaluate("INPUTS/*", elm, XPathConstants.NODE);
		system.entryCondition = makeEntryCondition(resource, entryConditionElm);
		NodeList featureNodes = (NodeList)xPath.evaluate("OUTPUTS/out", elm, XPathConstants.NODESET);
		for (int i = 0; i < featureNodes.getLength(); i++) {
			SflFeature feature = makeFeature(resource, (Element)featureNodes.item(i));
			feature.probability = 1.0 / (double)featureNodes.getLength();
			system.features.add(feature);
		}
	}

	private final SflFeature makeFeature(SflResource resource, Element elm) throws XPathExpressionException {
		String name = xPath.evaluate("feature", elm);
		SflFeature feature = resource.makeFeature(name);
		String probability = xPath.evaluate("probability", elm).intern();
		feature.probability = probability != EMPTY ? Double.parseDouble(probability) : 0.0;
		feature.active = true;
		NodeList realisationNodes = (NodeList)xPath.evaluate("realization/*", elm, XPathConstants.NODESET);
		for (int i = 0; i < realisationNodes.getLength(); i++) {
			feature.statements.add(makeStatement(resource, (Element)realisationNodes.item(i)));
		}
		return feature;
	}

	private final SflStatement makeStatement(SflResource resource, Element elm) throws XPathExpressionException {
		String op = elm.getNodeName();
		op = toTitleCase(op);
		SflStatement statement = new SflStatement(op.intern());
		String a = xPath.evaluate("a", elm).intern();
		statement.arguments.add(a);
		String b = xPath.evaluate("b", elm).intern();
		if (b != EMPTY) {
			statement.arguments.add(b);
		}
		return statement;
	}

	private final String toTitleCase(String token) {
		String[] parts = token.split("[-]");
		StringBuffer buffer = new StringBuffer();
		for (String part : parts) {
			buffer.append(part.substring(0, 1).toUpperCase());
			buffer.append(part.substring(1));
		}
		return buffer.toString();
	}

	private final SflEntryCondition makeEntryCondition(SflResource resource, Element elm) throws XPathExpressionException {
		if (elm.getNodeName().equals("s")) {
			String name = elm.getTextContent();
			SflFeature feature = resource.makeFeature(name);
			return new SflFeatureCondition(feature);
		} else if (elm.getNodeName().equals("and")) {
			SflIntersectionCondition condition = new SflIntersectionCondition();
			NodeList featureNodes = (NodeList)xPath.evaluate("*", elm, XPathConstants.NODESET);
			for (int i = 0; i < featureNodes.getLength(); i++) {
				condition.conditions.add(makeEntryCondition(resource, (Element)featureNodes.item(i)));
			}
			return condition;
		} else if (elm.getNodeName().equals("or")) {
			SflUnionCondition condition = new SflUnionCondition();
			NodeList featureNodes = (NodeList)xPath.evaluate("*", elm, XPathConstants.NODESET);
			for (int i = 0; i < featureNodes.getLength(); i++) {
				condition.conditions.add(makeEntryCondition(resource, (Element)featureNodes.item(i)));
			}
			return condition;
		} else if (elm.getNodeName().equals("not")) {
			SflComplementCondition condition = new SflComplementCondition();
			Node featureNode = (Node)xPath.evaluate("*", elm, XPathConstants.NODE);
			condition.condition = makeEntryCondition(resource, (Element)featureNode);
			return condition;
		} else return null;
	}

}
