/******************************************************************************
 * Vige, Home of Professional Open Source Copyright 2010, Vige, and           *
 * individual contributors by the @authors tag. See the copyright.txt in the  *
 * distribution for a full listing of individual contributors.                *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may    *
 * not use this file except in compliance with the License. You may obtain    *
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0        *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/
package it.vige.albopretorio.demoamp.test;

import static it.vige.albopretorio.demoamp.DemoComponent.albo_name;
import static it.vige.albopretorio.ocr.OCRExtractAction.getText;
import static java.lang.Thread.currentThread;
import static org.alfresco.model.ContentModel.ASSOC_CONTAINS;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.model.ContentModel.PROP_NAME;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.repo.security.authentication.AuthenticationUtil.setFullyAuthenticatedUser;
import static org.alfresco.service.namespace.NamespaceService.CONTENT_MODEL_1_0_URI;
import static org.alfresco.service.namespace.QName.createQName;
import static org.apache.log4j.Logger.getLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;

import it.vige.albopretorio.demoamp.DemoComponent;
import it.vige.albopretorio.ocr.OCRTransformWorker;

/**
 * A simple class demonstrating how to run out-of-container tests loading
 * Alfresco application context.
 * 
 * This class uses the RemoteTestRunner to try and connect to localhost:4578 and
 * send the test name and method to be executed on a running Alfresco. One or
 * more hostnames can be configured in the @Remote annotation.
 * 
 * If there is no available remote server to run the test, it falls back on
 * local running of JUnits.
 * 
 * For proper functioning the test class file must match exactly the one
 * deployed in the webapp (either via JRebel or static deployment) otherwise
 * "incompatible magic value XXXXX" class error loading issues will arise.
 * 
 * @author Gabriele Columbro
 * @author Maurizio Pillitu
 *
 */
@RunWith(RemoteTestRunner.class)
@Remote(runnerClass = SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public class DemoComponentTest {

	private static final String ADMIN_USER_NAME = "admin";

	static Logger log = getLogger(DemoComponentTest.class);

	@Autowired
	protected DemoComponent demoComponent;

	@Autowired
	protected OCRTransformWorker ocrTransformWorker;

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;

	@Autowired
	@Qualifier("ContentService")
	protected ContentService contentService;

	@Autowired
	@Qualifier("VersionService")
	protected VersionService versionService;

	@Test
	public void testOCRConversion() {
		setFullyAuthenticatedUser(ADMIN_USER_NAME);
		NodeRef companyHome = demoComponent.getCompanyHome();
		NodeRef albo = nodeService.getChildByName(companyHome, ASSOC_CONTAINS, albo_name);
		ClassLoader classLoader = currentThread().getContextClassLoader();
		String fileName = "Pub_matr_061_02_03_2016.pdf";
		InputStream doc = classLoader.getResourceAsStream("docs/" + fileName);
		createContentNode(albo, fileName, doc);

		NodeRef pub = nodeService.getChildByName(albo, ASSOC_CONTAINS, "Pub_matr_061_02_03_2016.pdf");
		Version version = versionService.getCurrentVersion(pub);
		assertEquals("1.1", version.getVersionLabel());
		Set<QName> aspects = nodeService.getAspects(pub);
		assertEquals(5, aspects.size());
		ContentReader reader = contentService.getReader(pub, PROP_CONTENT);
		boolean contains = false;
		try {
			contains = getText(reader.getContentInputStream()).contains("Residente");
		} catch (ContentIOException | IOException e) {
			log.error(e);
		}
		assertTrue(contains);

		fileName = "InterrogazioneCavaColleLargo.pdf";
		doc = classLoader.getResourceAsStream("docs/" + fileName);
		createContentNode(albo, fileName, doc);

		NodeRef interrogazione = nodeService.getChildByName(albo, ASSOC_CONTAINS, "InterrogazioneCavaColleLargo.pdf");
		version = versionService.getCurrentVersion(interrogazione);
		assertNull(version);
		aspects = nodeService.getAspects(interrogazione);
		assertEquals(4, aspects.size());
		reader = contentService.getReader(interrogazione, PROP_CONTENT);
		try {
			contains = getText(reader.getContentInputStream()).contains("Sebastiano");
		} catch (ContentIOException | IOException e) {
			log.error(e);
		}
		assertTrue(contains);

		nodeService.deleteNode(pub);
		nodeService.deleteNode(interrogazione);
	}

	private NodeRef createContentNode(NodeRef parent, String name, InputStream text) {

		// Create a map to contain the values of the properties of the node

		Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
		props.put(PROP_NAME, name);

		// use the node service to create a new node
		NodeRef node = this.nodeService
				.createNode(parent, ASSOC_CONTAINS, createQName(CONTENT_MODEL_1_0_URI, name), TYPE_CONTENT, props)
				.getChildRef();

		// Use the content service to set the content onto the newly created
		// node
		ContentWriter writer = contentService.getWriter(node, PROP_CONTENT, true);
		writer.setMimetype(MIMETYPE_PDF);
		writer.setEncoding("UTF-8");
		writer.putContent(text);

		// Return a node reference to the newly created node
		return node;
	}

}
