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
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An exporter of UamXml resources.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class UamXmlExporter implements ResourceExporter {

	private File resourceDirectory;

	/**
	 * Constructor
	 * 
	 * @param resourceDirectory the resource directory
	 */
	public UamXmlExporter(File resourceDirectory) {
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
			Document document = makeNetworkDocument(systemMap.get(regionName), resource.getRootFeature());
			File file = new File(resourceDirectory, regionName + ".system.uam.xml");
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

	private final Document makeNetworkDocument(List<SflNetworkNode> nodes, SflFeature rootFeature) throws ParserConfigurationException {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element networkElm = document.createElement("NETWORK");
		Element rootElm = document.createElement("ROOT");
		Element featureElm = document.createElement("FEATURE");
		makeElementAttribute(document, featureElm, "NAME", rootFeature.name);
		makeElementAttribute(document, featureElm, "STATE", rootFeature.active ? "active" : "inactive");
		rootElm.appendChild(featureElm);
		Element systemsElm = document.createElement("SYSTEMS");
		networkElm.setAttribute("xmlns", "http://www.uppermodel.org/uam/systems");
		networkElm.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		networkElm.setAttribute("xsi:schemaLocation", "http://www.uppermodel.org/uam/systems uam.system.xsd");
		networkElm.appendChild(rootElm);
		networkElm.appendChild(systemsElm);
		document.appendChild(networkElm);
		for (SflNetworkNode node : nodes) {
			if (node instanceof SflSystem) {
				SflSystem system = (SflSystem)node;
				Element systemElm = makeSystemElm(document, system);
				systemsElm.appendChild(systemElm);
			} else {
				SflGate gate = (SflGate)node;
				Element gateElm = makeGateElm(document, gate);
				systemsElm.appendChild(gateElm);
			}
		}
		return document;
	}

	private final void makeElementAttribute(Document document, Node node, String tagName, String textContext) {
		Element inRegionElm = document.createElement(tagName);
		inRegionElm.setTextContent(textContext);
		node.appendChild(inRegionElm);
	}

	private final Element makeGateElm(Document document, SflGate gate) {
		Element gateElm = document.createElement("SYSTEM");
		makeElementAttribute(document, gateElm, "NAME", gate.name);
		Element ecElm = document.createElement("EC");
		if (gate.entryCondition instanceof SflFeatureCondition) {
			SflFeatureCondition condition = (SflFeatureCondition)gate.entryCondition;
			ecElm.setTextContent(condition.feature.name);
		} else {
			ecElm.appendChild(makeEntryConditionElm(document, gate.entryCondition));
		}
		gateElm.appendChild(ecElm);
		Element featuresElm = document.createElement("FEATURES");
		featuresElm.appendChild(makeFeatureElm(document, gate.feature));
		gateElm.appendChild(featuresElm);
		return gateElm;
	}

	private final Element makeSystemElm(Document document, SflSystem system) {
		Element systemElm = document.createElement("SYSTEM");
		makeElementAttribute(document, systemElm, "NAME", system.name);
		Element ecElm = document.createElement("EC");
		if (system.entryCondition instanceof SflFeatureCondition) {
			SflFeatureCondition condition = (SflFeatureCondition)system.entryCondition;
			ecElm.setTextContent(condition.feature.name);
		} else {
			ecElm.appendChild(makeEntryConditionElm(document, system.entryCondition));
		}
		systemElm.appendChild(ecElm);
		Element featuresElm = document.createElement("FEATURES");
		for (SflFeature feature : system.features) {
			featuresElm.appendChild(makeFeatureElm(document, feature));
		}
		systemElm.appendChild(featuresElm);
		return systemElm;
	}

	private final DocumentFragment makeEntryConditionElm(Document document, SflEntryCondition entryCondition) {
		DocumentFragment frag = document.createDocumentFragment();
		if (entryCondition instanceof SflFeatureCondition) {
			SflFeatureCondition condition = (SflFeatureCondition)entryCondition;
			Element featureElm = document.createElement("FEATURE");
			makeElementAttribute(document, featureElm, "NAME", condition.feature.name);
			makeElementAttribute(document, featureElm, "STATE", condition.feature.active ? "active" : "inactive");
			frag.appendChild(featureElm);
		} else if (entryCondition instanceof SflUnionCondition) {
			SflUnionCondition condition = (SflUnionCondition)entryCondition;
			makeElementAttribute(document, frag, "OP", "or");
			for (SflEntryCondition aCondition : condition.conditions) {
				frag.appendChild(makeEntryConditionElm(document, aCondition));
			}
		} else if (entryCondition instanceof SflIntersectionCondition) {
			SflIntersectionCondition condition = (SflIntersectionCondition)entryCondition;
			makeElementAttribute(document, frag, "OP", "and");
			for (SflEntryCondition aCondition : condition.conditions) {
				frag.appendChild(makeEntryConditionElm(document, aCondition));
			}
		} else if (entryCondition instanceof SflComplementCondition) {
			SflComplementCondition condition = (SflComplementCondition)entryCondition;
			makeElementAttribute(document, frag, "OP", "not");
			frag.appendChild(makeEntryConditionElm(document, condition.condition));
		}
		return frag;
	}

	private final Element makeFeatureElm(Document document, SflFeature feature) {
		Element featureElm = document.createElement("FEATURE");
		makeElementAttribute(document, featureElm, "NAME", feature.name);
		makeElementAttribute(document, featureElm, "STATE", feature.active ? "active" : "inactive");
		if (feature.statements.size() > 0) {
			Element realizationElm = document.createElement("REALISATIONS");
			featureElm.appendChild(realizationElm);
			for (SflStatement statement : feature.statements) {
				Element statementElm = makeStatementElm(document, statement);
				realizationElm.appendChild(statementElm);
			}
		}
		return featureElm;
	}

	private Element makeStatementElm(Document document, SflStatement statement) {
		Element statementElm = document.createElement("REALISATION");
		makeElementAttribute(document, statementElm, "OP", statement.operator);
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < statement.arguments.size(); i++) {
			if (i > 0) {
				buffer.append(" ");
			}
			buffer.append(statement.arguments.get(i));
		}
		makeElementAttribute(document, statementElm, "ARGS", buffer.toString());
		return statementElm;
	}


}
