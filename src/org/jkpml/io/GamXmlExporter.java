package org.jkpml.io;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jkpml.dto.SflComplementCondition;
import org.jkpml.dto.SflEntryCondition;
import org.jkpml.dto.SflFeature;
import org.jkpml.dto.SflFeatureCondition;
import org.jkpml.dto.SflGate;
import org.jkpml.dto.SflIntersectionCondition;
import org.jkpml.dto.SflNetworkNode;
import org.jkpml.dto.SflResource;
import org.jkpml.dto.SflStatement;
import org.jkpml.dto.SflSystem;
import org.jkpml.dto.SflUnionCondition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An exporter of GamXml resources.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class GamXmlExporter implements ResourceImporter {

	private static final String EMPTY = "".intern();
	private static final String NONE = "None".intern();
	private File resourceDirectory;

	/**
	 * Constructor
	 * 
	 * @param resourceDirectory the resource directory
	 */
	public GamXmlExporter(File resourceDirectory) {
		this.resourceDirectory = resourceDirectory;
	}

	public final void exportResource(SflResource resource) {
		try {
			exportResourceUnsafe(resource);
		} catch (TransformerFactoryConfigurationError | TransformerException | ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	private final void exportResourceUnsafe(SflResource resource) throws TransformerFactoryConfigurationError, TransformerException, ParserConfigurationException {
		Map<String, List<SflNetworkNode>> systemMap = resource.getSystemMapByRegion();
		for (String regionName : systemMap.keySet()) {
			Document document = makeNetworkDocument(systemMap.get(regionName));
			Element regionElm = document.getDocumentElement();
			regionElm.setAttribute("name", regionName);
			File file = new File(resourceDirectory, regionName + ".system.gam.xml");
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			Result output = new StreamResult(file);
			Source input = new DOMSource(document);
			transformer.transform(input, output);
		}
	}

	private final Document makeNetworkDocument(List<SflNetworkNode> nodes) throws ParserConfigurationException {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element networkElm = document.createElement("Region");
		networkElm.setAttribute("xmlns", "http://www.uppermodel.org/gam/systems");
		networkElm.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		networkElm.setAttribute("xsi:schemaLocation", "http://www.uppermodel.org/gam/systems gam.system.xsd");
		networkElm.setAttribute("stratum", "Lexicogrammar");
		document.appendChild(networkElm);
		for (SflNetworkNode node : nodes) {
			if (node instanceof SflSystem) {
				SflSystem system = (SflSystem)node;
				Element systemElm = makeSystemElm(document, system);
				networkElm.appendChild(systemElm);
			} else {
				SflGate gate = (SflGate)node;
				Element gateElm = makeGateElm(document, gate);
				networkElm.appendChild(gateElm);
			}
		}
		return document;
	}

	private final Element makeGateElm(Document document, SflGate gate) {
		Element gateElm = document.createElement("Gate");
		gateElm.setAttribute("name", gate.name);
		Element entryConditionElm = document.createElement("EntryCondition");
		entryConditionElm.appendChild(makeEntryConditionElm(document, gate.entryCondition));
		gateElm.appendChild(entryConditionElm);
		Element featureElm = makeFeatureElm(document, gate.feature);
		gateElm.appendChild(featureElm);
		return gateElm;
	}

	private final Element makeSystemElm(Document document, SflSystem system) {
		Element systemElm = document.createElement("System");
		systemElm.setAttribute("name", system.name);
		if (system.metafunction != NONE) {
			systemElm.setAttribute("metafunction", system.metafunction);
		}
		Element entryConditionElm = document.createElement("EntryCondition");
		entryConditionElm.appendChild(makeEntryConditionElm(document, system.entryCondition));
		systemElm.appendChild(entryConditionElm);
		for (SflFeature feature : system.features) {
			Element featureElm = makeFeatureElm(document, feature);
			systemElm.appendChild(featureElm);
		}
		return systemElm;
	}

	private final Element makeEntryConditionElm(Document document, SflEntryCondition entryCondition) {
		if (entryCondition instanceof SflFeatureCondition) {
			Element featureElm = document.createElement("Feature");
			featureElm.setAttribute("name", ((SflFeatureCondition)entryCondition).feature.name);
			return featureElm;
		} else if (entryCondition instanceof SflUnionCondition) {
			Element unionElm = document.createElement("Or");
			List<SflEntryCondition> conditions = ((SflUnionCondition)entryCondition).conditions;
			for (SflEntryCondition condition : conditions) {
				unionElm.appendChild(makeEntryConditionElm(document, condition));
			}
			return unionElm;
		} else if (entryCondition instanceof SflIntersectionCondition) {
			Element intersectionElm = document.createElement("And");
			List<SflEntryCondition> conditions = ((SflIntersectionCondition)entryCondition).conditions;
			for (SflEntryCondition condition : conditions) {
				intersectionElm.appendChild(makeEntryConditionElm(document, condition));
			}
			return intersectionElm;
		} else if (entryCondition instanceof SflComplementCondition) {
			Element complementElm = document.createElement("Not");
			SflEntryCondition condition = ((SflComplementCondition)entryCondition).condition;
			complementElm.appendChild(makeEntryConditionElm(document, condition));
			return complementElm;
		} else return null;
	}

	private final Element makeFeatureElm(Document document, SflFeature feature) {
		Element featureElm = document.createElement("Feature");
		featureElm.setAttribute("name", feature.name);
		if (!feature.active) {
			featureElm.setAttribute("active", Boolean.toString(feature.active));
		}
		if (0.0 != feature.probability) {
			featureElm.setAttribute("probability", Double.toString(feature.probability));
		}
		if (feature.gloss != EMPTY) {
			featureElm.setAttribute("gloss", feature.gloss);
		}
		if (feature.comment != EMPTY) {
			featureElm.setAttribute("comment", feature.comment);
		}
		for (SflStatement statement : feature.statements) {
			Element statementElm = makeStatementElm(document, statement);
			featureElm.appendChild(statementElm);
		}
		return featureElm;
	}

	private Element makeStatementElm(Document document, SflStatement statement) {
		Element statementElm = document.createElement(statement.operator);
		for (int i = 0; i < statement.arguments.size(); i++) {
			String value = statement.arguments.get(i);
			String name = EMPTY + (char)(((int)'a') + i);
			statementElm.setAttribute(name, value);
		}
		return statementElm;
	}
}
