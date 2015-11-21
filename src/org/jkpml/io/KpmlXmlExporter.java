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
 * An exporter of KpmlLisp resources.
 * 
 * @author Daniel Couto-Vale <danielvale@icloud.com>
 */
public class KpmlXmlExporter implements ResourceExporter {

	private static final String EMPTY = "".intern();
	private File resourceDirectory;

	/**
	 * Constructor
	 * 
	 * @param resourceDirectory the resource directory
	 */
	public KpmlXmlExporter(File resourceDirectory) {
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
			Document document = makeNetworkDocument(systemMap.get(regionName), regionName);
			File file = new File(resourceDirectory, regionName + ".system.kpml.xml");
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

	private final Document makeNetworkDocument(List<SflNetworkNode> nodes, String regionName) throws ParserConfigurationException {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element systemsElm = document.createElement("SYSTEMS");
		systemsElm.setAttribute("xmlns", "http://www.fb10.uni-bremen.de/anglistik/langpro/kpml/README.html");
		systemsElm.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		systemsElm.setAttribute("xsi:schemaLocation", "http://www.fb10.uni-bremen.de/anglistik/langpro/kpml/README.html kpml.system.xsd");
		document.appendChild(systemsElm);
		makeElementAttribute(document, systemsElm, "COMMENTS", EMPTY);
		makeElementAttribute(document, systemsElm, "IN-REGION", regionName);
		makeElementAttribute(document, systemsElm, "IN-LANGUAGE", EMPTY);
		for (SflNetworkNode node : nodes) {
			if (node instanceof SflSystem) {
				SflSystem system = (SflSystem)node;
				Element systemElm = makeSystemElm(document, system, regionName);
				systemsElm.appendChild(systemElm);
			} else {
				SflGate gate = (SflGate)node;
				Element gateElm = makeGateElm(document, gate, regionName);
				systemsElm.appendChild(gateElm);
			}
		}
		return document;
	}

	private final void makeElementAttribute(Document document, Element networkElm, String tagName, String textContext) {
		Element inRegionElm = document.createElement(tagName);
		inRegionElm.setTextContent(textContext);
		networkElm.appendChild(inRegionElm);
	}

	private final Element makeGateElm(Document document, SflGate gate, String regionName) {
		Element gateElm = document.createElement("SYSTEM");
		makeElementAttribute(document, gateElm, "SYSTEM-TYPE", "GATE");
		makeElementAttribute(document, gateElm, "NAME", gate.name);
		Element inputsElm = document.createElement("INPUTS");
		inputsElm.appendChild(makeEntryConditionElm(document, gate.entryCondition));
		gateElm.appendChild(inputsElm);
		Element outputsElm = document.createElement("OUTPUTS");
		outputsElm.appendChild(makeOutElm(document, gate.feature));
		gateElm.appendChild(outputsElm);
		makeElementAttribute(document, gateElm, "REGION", regionName);
		return gateElm;
	}

	private final Element makeSystemElm(Document document, SflSystem system, String regionName) {
		Element systemElm = document.createElement("SYSTEM");
		makeElementAttribute(document, systemElm, "SYSTEM-TYPE", "SYSTEM");
		makeElementAttribute(document, systemElm, "NAME", system.name);
		Element inputsElm = document.createElement("INPUTS");
		inputsElm.appendChild(makeEntryConditionElm(document, system.entryCondition));
		systemElm.appendChild(inputsElm);
		Element outputsElm = document.createElement("OUTPUTS");
		for (SflFeature feature : system.features) {
			outputsElm.appendChild(makeOutElm(document, feature));
		}
		systemElm.appendChild(outputsElm);
		makeElementAttribute(document, systemElm, "CHOOSER", system.name + "-CHOOSER");
		makeElementAttribute(document, systemElm, "REGION", regionName);
		makeElementAttribute(document, systemElm, "METAFUNCTION", system.metafunction.toUpperCase());
		return systemElm;
	}

	private final Element makeEntryConditionElm(Document document, SflEntryCondition entryCondition) {
		if (entryCondition instanceof SflFeatureCondition) {
			Element featureElm = document.createElement("s");
			featureElm.setTextContent(((SflFeatureCondition)entryCondition).feature.name);
			return featureElm;
		} else if (entryCondition instanceof SflUnionCondition) {
			Element unionElm = document.createElement("or");
			List<SflEntryCondition> conditions = ((SflUnionCondition)entryCondition).conditions;
			for (SflEntryCondition condition : conditions) {
				unionElm.appendChild(makeEntryConditionElm(document, condition));
			}
			return unionElm;
		} else if (entryCondition instanceof SflIntersectionCondition) {
			Element intersectionElm = document.createElement("and");
			List<SflEntryCondition> conditions = ((SflIntersectionCondition)entryCondition).conditions;
			for (SflEntryCondition condition : conditions) {
				intersectionElm.appendChild(makeEntryConditionElm(document, condition));
			}
			return intersectionElm;
		} else if (entryCondition instanceof SflComplementCondition) {
			Element complementElm = document.createElement("not");
			SflEntryCondition condition = ((SflComplementCondition)entryCondition).condition;
			complementElm.appendChild(makeEntryConditionElm(document, condition));
			return complementElm;
		} else return null;
	}

	private final Element makeOutElm(Document document, SflFeature feature) {
		Element outElm = document.createElement("out");
		Element probabilityElm = document.createElement("probability");
		probabilityElm.setTextContent(Double.toString(feature.probability));
		outElm.appendChild(probabilityElm);
		Element featureElm = document.createElement("feature");
		featureElm.setTextContent(feature.name);
		outElm.appendChild(featureElm);
		if (feature.statements.size() > 0) {
			Element realizationElm = document.createElement("realization");
			outElm.appendChild(realizationElm);
			for (SflStatement statement : feature.statements) {
				Element statementElm = makeStatementElm(document, statement);
				realizationElm.appendChild(statementElm);
			}
		}
		return outElm;
	}

	private Element makeStatementElm(Document document, SflStatement statement) {
		Element statementElm = document.createElement(statement.operator.toLowerCase());
		for (int i = 0; i < statement.arguments.size(); i++) {
			String value = statement.arguments.get(i);
			String name = EMPTY + (char)(((int)'a') + i);
			makeElementAttribute(document, statementElm, name, value);
		}
		return statementElm;
	}
}
