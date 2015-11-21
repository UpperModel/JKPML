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
 * An importer of GamXml resources
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class GamXmlImporter implements ResourceExporter {

	/**
	 * The file filter
	 */
	private final static FilenameFilter filter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.split("[.]").length == 4 && name.endsWith(".system.gam.xml");
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
	public GamXmlImporter(File resourceDirectory) {
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
		} catch (IOException | SAXException | ParserConfigurationException
				| XPathExpressionException e) {
			e.printStackTrace();
		}
	}

	private final void importResourcesUnsafe(SflResource resource, File file) throws SAXException,
			IOException, ParserConfigurationException, XPathExpressionException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(file);

		// Make root feature
		// FIXME this should be read from the network file
		SflFeature rootFeature = resource.makeFeature("wording");
		rootFeature.active = true;
		resource.setRootFeature(rootFeature);

		// Make network nodes
		String region = file.getName().split("[.]")[0];
		NodeList systemNodes = (NodeList) xPath.evaluate("/Region/System", document,
				XPathConstants.NODESET);
		for (int i = 0; i < systemNodes.getLength(); i++) {
			makeSystem(resource, (Element) systemNodes.item(i), region);
		}
		NodeList gateNodes = (NodeList) xPath.evaluate("/Region/Gate", document,
				XPathConstants.NODESET);
		for (int i = 0; i < gateNodes.getLength(); i++) {
			makeGate(resource, (Element) gateNodes.item(i), region);
		}
	}

	private final void makeGate(SflResource resource, Element elm, String region)
			throws XPathExpressionException {
		String name = elm.getAttribute("name");
		SflGate gate = resource.makeGate(name);
		gate.region = region;
		Element entryConditionElm = (Element) xPath.evaluate("EntryCondition/*", elm,
				XPathConstants.NODE);
		gate.entryCondition = makeEntryCondition(resource, entryConditionElm);
		Element featureElm = (Element) xPath.evaluate("Feature", elm, XPathConstants.NODE);
		gate.feature = makeFeature(resource, featureElm);
	}

	private final void makeSystem(SflResource resource, Element elm, String region)
			throws XPathExpressionException {
		String name = elm.getAttribute("name");
		SflSystem system = resource.makeSystem(name);
		system.region = region.intern();
		Element entryConditionElm = (Element) xPath.evaluate("EntryCondition/*", elm,
				XPathConstants.NODE);
		system.entryCondition = makeEntryCondition(resource, entryConditionElm);
		NodeList featureNodes = (NodeList) xPath.evaluate("Feature", elm, XPathConstants.NODESET);
		for (int i = 0; i < featureNodes.getLength(); i++) {
			SflFeature feature = makeFeature(resource, (Element) featureNodes.item(i));
			system.features.add(feature);
		}
	}

	private final SflFeature makeFeature(SflResource resource, Element elm)
			throws XPathExpressionException {
		String name = elm.getAttribute("name");
		SflFeature feature = resource.makeFeature(name);
		String probability = elm.getAttribute("probability").intern();
		feature.probability = probability != EMPTY ? Double.parseDouble(probability) : 0.0;
		String active = elm.getAttribute("active");
		feature.active = active != EMPTY ? Boolean.parseBoolean(active) : true;
		NodeList realisationNodes = (NodeList) xPath.evaluate("*", elm,
				XPathConstants.NODESET);
		for (int i = 0; i < realisationNodes.getLength(); i++) {
			feature.statements.add(makeStatement(resource, (Element) realisationNodes.item(i)));
		}
		return feature;
	}

	private final SflStatement makeStatement(SflResource resource, Element elm)
			throws XPathExpressionException {
		String operation = elm.getTagName();
		SflStatement statement = new SflStatement(operation.intern());
		String a = elm.getAttribute("a");
		statement.arguments.add(a.intern());
		String b = elm.getAttribute("b");
		if (b != EMPTY) {
			statement.arguments.add(b.intern());
		}
		return statement;
	}

	private final SflEntryCondition makeEntryCondition(SflResource resource, Element elm)
			throws XPathExpressionException {
		if (elm.getNodeName().equals("Feature")) {
			String name = elm.getAttribute("name");
			SflFeature feature = resource.makeFeature(name);
			return new SflFeatureCondition(feature);
		} else if (elm.getNodeName().equals("And")) {
			SflIntersectionCondition condition = new SflIntersectionCondition();
			NodeList conditionNodes = (NodeList) xPath.evaluate("*", elm,
					XPathConstants.NODESET);
			for (int i = 0; i < conditionNodes.getLength(); i++) {
				condition.conditions.add(makeEntryCondition(resource,
						(Element) conditionNodes.item(i)));
			}
			return condition;
		} else if (elm.getNodeName().equals("Or")) {
			SflUnionCondition condition = new SflUnionCondition();
			NodeList conditionNodes = (NodeList) xPath.evaluate("*", elm,
					XPathConstants.NODESET);
			for (int i = 0; i < conditionNodes.getLength(); i++) {
				condition.conditions.add(makeEntryCondition(resource,
						(Element) conditionNodes.item(i)));
			}
			return condition;
		} else if (elm.getNodeName().equals("Not")) {
			SflComplementCondition condition = new SflComplementCondition();
			Node conditionNode = (Node) xPath.evaluate("element()", elm,
					XPathConstants.NODE);
			condition.condition = makeEntryCondition(resource,
						(Element) conditionNode);
		}
		return null;
	}

}
