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
import static org.alfresco.model.ContentModel.ASSOC_CONTAINS;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.model.ContentModel.PROP_NAME;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.repo.security.authentication.AuthenticationUtil.setFullyAuthenticatedUser;
import static org.apache.log4j.Logger.getLogger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
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

	@Test
	public void testWiring() {
		assertNotNull(demoComponent);
	}

	@Test
	public void testGetCompanyHome() {
		setFullyAuthenticatedUser(ADMIN_USER_NAME);
		NodeRef companyHome = demoComponent.getCompanyHome();
		assertNotNull(companyHome);
		String companyHomeName = (String) nodeService.getProperty(companyHome, PROP_NAME);
		assertNotNull(companyHomeName);
		assertTrue(companyHomeName.contains("Home"));
	}

	@Test
	public void testChildNodesCount() {
		setFullyAuthenticatedUser(ADMIN_USER_NAME);
		NodeRef companyHome = demoComponent.getCompanyHome();
		int childNodeCount = demoComponent.childNodesCount(companyHome);
		assertNotNull(childNodeCount);
		// There are 8 folders by default under Company Home
		assertEquals(8, childNodeCount);
	}

	@Test
	public void testImportAlbo() {
		setFullyAuthenticatedUser(ADMIN_USER_NAME);
		NodeRef companyHome = demoComponent.getCompanyHome();
		NodeRef albo = nodeService.getChildByName(companyHome, ASSOC_CONTAINS, albo_name);
		int childNodeCount = demoComponent.childNodesCount(albo);
		assertNotNull(childNodeCount);
		// There are 6 documents by default under the albo_pretorio folder
		assertEquals(6, childNodeCount);
	}

	@Test
	public void testOCRConversion() {
		setFullyAuthenticatedUser(ADMIN_USER_NAME);
		NodeRef companyHome = demoComponent.getCompanyHome();
		NodeRef albo = nodeService.getChildByName(companyHome, ASSOC_CONTAINS, albo_name);

		ContentReader reader = contentService.getReader(
				nodeService.getChildByName(albo, ASSOC_CONTAINS, "InterrogazioneCavaColleLargo.pdf"), PROP_CONTENT);

		ContentWriter writer = contentService.getTempWriter();
		writer.setMimetype(MIMETYPE_PDF);
		try {
			ocrTransformWorker.transform(reader, writer, null);
			assertTrue(writer.getReader().getContentString().contains("Sebastiano"));
		} catch (Exception e) {
			fail();
		}
	}

}
