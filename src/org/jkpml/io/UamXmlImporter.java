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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An importer of UamXml resource
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class UamXmlImporter implements ResourceImporter {

	/**
	 * The file filter
	 */
	private final static FilenameFilter filter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.split("[.]").length == 2 && name.endsWith(".xml");
		}
	};

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
	public UamXmlImporter(File resourceDirectory) {
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
		Element featureElm = (Element)xPath.evaluate("/NETWORK/ROOT/FEATURE", document, XPathConstants.NODE);
		SflFeature feature = makeFeature(resource, featureElm);
		resource.setRootFeature(feature);

		// Make network nodes
		String region = file.getName().split("[.]")[0];
		NodeList networkNodeNodes = (NodeList) xPath.evaluate("/NETWORK/SYSTEMS/SYSTEM", document, XPathConstants.NODESET);
		for (int i = 0; i < networkNodeNodes.getLength(); i++) {
			makeNetworkNode(resource, (Element)networkNodeNodes.item(i), region);
		}
	}

	private final void makeNetworkNode(SflResource resource, Element elm, String region) throws XPathExpressionException {
		NodeList featureNodes = (NodeList)xPath.evaluate("FEATURES/FEATURE", elm, XPathConstants.NODESET);
		if (featureNodes.getLength() == 1) {
			makeGate(resource, elm, region);
		} else {
			makeSystem(resource, elm, region);
		}
	}

	private final void makeGate(SflResource resource, Element elm, String region) throws XPathExpressionException {
		String name = xPath.evaluate("NAME", elm);
		SflGate gate = resource.makeGate(name);
		gate.region = region;
		Element entryConditionElm = (Element)xPath.evaluate("EC", elm, XPathConstants.NODE);
		gate.entryCondition = makeEntryCondition(resource, entryConditionElm);
		Element featureElm = (Element)xPath.evaluate("FEATURES/FEATURE", elm, XPathConstants.NODE);
		gate.feature = makeFeature(resource, featureElm);
	}

	private final void makeSystem(SflResource resource, Element elm, String region) throws XPathExpressionException {
		String name = xPath.evaluate("NAME", elm);
		SflSystem system = resource.makeSystem(name);
		system.region = region.intern();
		Element entryConditionElm = (Element)xPath.evaluate("EC", elm, XPathConstants.NODE);
		system.entryCondition = makeEntryCondition(resource, entryConditionElm);
		NodeList featureNodes = (NodeList)xPath.evaluate("FEATURES/FEATURE", elm, XPathConstants.NODESET);
		for (int i = 0; i < featureNodes.getLength(); i++) {
			SflFeature feature = makeFeature(resource, (Element)featureNodes.item(i));
			feature.probability = 1.0 / (double)featureNodes.getLength();
			system.features.add(feature);
		}
	}

	private final SflFeature makeFeature(SflResource resource, Element elm) throws XPathExpressionException {
		String name = xPath.evaluate("NAME", elm);
		SflFeature feature = resource.makeFeature(name);
		feature.probability = 0;
		feature.active = xPath.evaluate("STATE", elm).equals("active");
		NodeList realisationNodes = (NodeList)xPath.evaluate("REALISATIONS/REALISATION", elm, XPathConstants.NODESET);
		for (int i = 0; i < realisationNodes.getLength(); i++) {
			feature.statements.add(makeStatement(resource, (Element)realisationNodes.item(i)));
		}
		return feature;
	}

	private final SflStatement makeStatement(SflResource resource, Element elm) throws XPathExpressionException {
		String op = xPath.evaluate("OP", elm);
		SflStatement statement = new SflStatement(op.intern());
		String args = xPath.evaluate("ARGS", elm);
		for (String argument : args.split(" ")) {
			statement.arguments.add(argument.intern());
		}
		return statement;
	}

	private final SflEntryCondition makeEntryCondition(SflResource resource, Element elm) throws XPathExpressionException {
		if (elm.getChildNodes().getLength() == 1) {
			String name = elm.getTextContent();
			SflFeature feature = resource.makeFeature(name);
			return new SflFeatureCondition(feature);
		} else if (elm.getNodeName().equals("FEATURE")) {
			String name = xPath.evaluate("NAME", elm);
			SflFeature feature = resource.makeFeature(name);
			return new SflFeatureCondition(feature);
		} else {
			String op = xPath.evaluate("OP", elm);
			if (op.equals("and")) {
				SflIntersectionCondition condition = new SflIntersectionCondition();
				NodeList featureNodes = (NodeList)xPath.evaluate("FEATURE", elm, XPathConstants.NODESET);
				for (int i = 0; i < featureNodes.getLength(); i++) {
					condition.conditions.add(makeEntryCondition(resource, (Element)featureNodes.item(i)));
				}
				return condition;
			}
			if (op.equals("or")) {
				SflUnionCondition condition = new SflUnionCondition();
				NodeList featureNodes = (NodeList)xPath.evaluate("FEATURE", elm, XPathConstants.NODESET);
				for (int i = 0; i < featureNodes.getLength(); i++) {
					condition.conditions.add(makeEntryCondition(resource, (Element)featureNodes.item(i)));
				}
				return condition;
			}
			return null;
		}
	}

}
