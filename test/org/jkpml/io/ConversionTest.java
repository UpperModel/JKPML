package org.jkpml.io;

import java.io.File;

import org.jkpml.dto.SflResource;
import org.junit.Test;

public class ConversionTest {

	@Test
	public final void testUam2Gam() {
		File resourceDirectory = new File("TestResources");
		UamXmlImporter importer = new UamXmlImporter(resourceDirectory);
		SflResource resource = importer.importResource();
		GamXmlExporter exporter = new GamXmlExporter(resourceDirectory);
		exporter.exportResource(resource);
	}

	@Test
	public final void testUam2Kpml() {
		File resourceDirectory = new File("TestResources");
		UamXmlImporter importer = new UamXmlImporter(resourceDirectory);
		SflResource resource = importer.importResource();
		KpmlXmlExporter exporter = new KpmlXmlExporter(resourceDirectory);
		exporter.exportResource(resource);
	}

	@Test
	public final void testUam2Uam() {
		File resourceDirectory = new File("TestResources");
		UamXmlImporter importer = new UamXmlImporter(resourceDirectory);
		SflResource resource = importer.importResource();
		UamXmlExporter exporter = new UamXmlExporter(resourceDirectory);
		exporter.exportResource(resource);
	}

	@Test
	public final void testGam2Uam() {
		File resourceDirectory = new File("TestResources");
		GamXmlImporter importer = new GamXmlImporter(resourceDirectory);
		SflResource resource = importer.importResource();
		UamXmlExporter exporter = new UamXmlExporter(resourceDirectory);
		exporter.exportResource(resource);
	}

	@Test
	public final void testKpml2Uam() {
		File resourceDirectory = new File("TestResources");
		KpmlXmlImporter importer = new KpmlXmlImporter(resourceDirectory);
		SflResource resource = importer.importResource();
		UamXmlExporter exporter = new UamXmlExporter(resourceDirectory);
		exporter.exportResource(resource);
	}

}
