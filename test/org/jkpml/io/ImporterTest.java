package org.jkpml.io;

import static org.junit.Assert.*;

import java.io.File;

import org.jkpml.dto.SflFeature;
import org.jkpml.dto.SflFeatureCondition;
import org.jkpml.dto.SflGate;
import org.jkpml.dto.SflIntersectionCondition;
import org.jkpml.dto.SflResource;
import org.jkpml.dto.SflSystem;
import org.junit.Before;
import org.junit.Test;

public class ImporterTest {

	private static final String POSSESSIVE_TYPE = "POSSESSIVE-TYPE";
	private static final String RELATIONAL_TYPE = "RELATIONAL-TYPE";
	private static final String ATTRIBUTIVE_BELONGING_TYPE_GATE = "ATTRIBUTIVE-BELONGING-TYPE-GATE";
	private SflResource resource;

	public final void prepareUam() {
		File resourceDirectory = new File("TestResources");
		UamXmlImporter importer = new UamXmlImporter(resourceDirectory);
		resource = importer.importResource();
	}
	public final void prepareKpml() {
		File resourceDirectory = new File("TestResources");
		KpmlXmlImporter importer = new KpmlXmlImporter(resourceDirectory);
		resource = importer.importResource();
	}
	public final void prepareGam() {
		File resourceDirectory = new File("TestResources");
		GamXmlImporter importer = new GamXmlImporter(resourceDirectory);
		resource = importer.importResource();
	}

	@Test
	public final void testUamRootFeature() {
		prepareUam();
		testRootFeature();
	}
	@Test
	public final void testKpmlRootFeature() {
		prepareKpml();
		testRootFeature();
	}
	@Test
	public final void testGamRootFeature() {
		prepareGam();
		testRootFeature();
	}
	private final void testRootFeature() {
		SflFeature feature = resource.getRootFeature();
		assertNotNull(feature);
		assertTrue(feature.name == "wording".intern());
		assertTrue(feature.active);
		assertTrue(feature.statements.size() == 0);
	}

	@Test
	public final void testUamPossessiveType() {
		prepareUam();
		SflSystem system = resource.getSystem(POSSESSIVE_TYPE);
		assertTrue(system.name == POSSESSIVE_TYPE.intern());
		assertTrue(system.features.size() == 2);
		assertTrue(system.features.get(0).name == "ownership&belonging".intern());
		assertTrue(system.features.get(0).active);
		assertTrue(system.features.get(1).name == "part&part-of".intern());
		assertTrue(system.features.get(1).active);
		assertTrue(system.entryCondition instanceof SflFeatureCondition);
		assertTrue(((SflFeatureCondition)system.entryCondition).feature.name == "possessive".intern());
	}

	@Test
	public final void testUamRelationalType() {
		prepareUam();
		testRelationalType();
	}
	@Test
	public final void testKpmlRelationalType() {
		prepareKpml();
		testRelationalType();
	}
	@Test
	public final void testGamRelationalType() {
		prepareGam();
		testRelationalType();
	}
	private final void testRelationalType() {
		SflSystem system = resource.getSystem(RELATIONAL_TYPE);
		assertTrue(system.name == RELATIONAL_TYPE.intern());
		assertTrue(system.features.size() == 3);
		assertTrue(system.features.get(0).name == "intrinsic".intern());
		assertTrue(system.features.get(0).active);
		assertTrue(system.features.get(0).statements.size() == 0);
		assertTrue(system.features.get(1).name == "incidental".intern());
		assertTrue(system.features.get(1).active);
		assertTrue(system.features.get(1).statements.size() == 0);
		assertTrue(system.features.get(2).name == "possessive".intern());
		assertTrue(system.features.get(2).active);
		assertTrue(system.features.get(2).statements.size() == 2);
		assertTrue(system.features.get(2).statements.get(0).operator == "Insert".intern());
		assertTrue(system.features.get(2).statements.get(0).arguments.size() == 1);
		assertTrue(system.features.get(2).statements.get(0).arguments.get(0) == "Possessor".intern());
		assertTrue(system.features.get(2).statements.get(1).operator == "Insert".intern());
		assertTrue(system.features.get(2).statements.get(1).arguments.size() == 1);
		assertTrue(system.features.get(2).statements.get(1).arguments.get(0) == "Possessed".intern());
		assertTrue(system.entryCondition instanceof SflFeatureCondition);
		assertTrue(((SflFeatureCondition)system.entryCondition).feature.name == "relational".intern());
	}

	@Test
	public final void testUamAttributiveHavingTypeGate() {
		prepareUam();
		testAttributiveHavingTypeGate();
	}
	@Test
	public final void testKpmlAttributiveHavingTypeGate() {
		prepareKpml();
		testAttributiveHavingTypeGate();
	}
	@Test
	public final void testGamAttributiveHavingTypeGate() {
		prepareGam();
		testAttributiveHavingTypeGate();
	}
	private void testAttributiveHavingTypeGate() {
		SflGate gate = resource.getGate(ATTRIBUTIVE_BELONGING_TYPE_GATE);
		assertTrue(gate.name == ATTRIBUTIVE_BELONGING_TYPE_GATE.intern());
		assertTrue(gate.feature.name == "attributive-belonging-type".intern());
		assertTrue(gate.feature.active);
		assertTrue(gate.feature.statements.size() == 2);
		assertTrue(gate.feature.statements.get(0).operator == "Conflate".intern());
		assertTrue(gate.feature.statements.get(0).arguments.size() == 2);
		assertTrue(gate.feature.statements.get(0).arguments.get(0) == "Carrier".intern());
		assertTrue(gate.feature.statements.get(0).arguments.get(1) == "Possessed".intern());
		assertTrue(gate.feature.statements.get(1).arguments.size() == 2);
		assertTrue(gate.feature.statements.get(1).arguments.get(0) == "Attribute".intern());
		assertTrue(gate.feature.statements.get(1).arguments.get(1) == "Possessor".intern());
		assertTrue(gate.entryCondition instanceof SflIntersectionCondition);
		assertTrue(((SflIntersectionCondition)gate.entryCondition).conditions.size() == 2);
		assertTrue(((SflIntersectionCondition)gate.entryCondition).conditions.get(0) instanceof SflFeatureCondition);
		assertTrue(((SflFeatureCondition)((SflIntersectionCondition)gate.entryCondition).conditions.get(0)).feature.name == "belonging-type".intern());
		assertTrue(((SflIntersectionCondition)gate.entryCondition).conditions.get(1) instanceof SflFeatureCondition);
		assertTrue(((SflFeatureCondition)((SflIntersectionCondition)gate.entryCondition).conditions.get(1)).feature.name == "attributive".intern());
	}

}
