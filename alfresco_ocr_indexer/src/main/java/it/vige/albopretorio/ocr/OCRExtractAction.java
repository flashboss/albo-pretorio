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
package it.vige.albopretorio.ocr;

import static it.vige.albopretorio.ocr.model.OCRdModel.ASPECT_OCRD;
import static it.vige.albopretorio.ocr.model.OCRdModel.PROP_PROCESSED_DATE;
import static org.alfresco.model.ContentModel.ASSOC_CONTAINS;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.model.ContentModel.PROP_NAME;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.repo.version.VersionBaseModel.PROP_VERSION_TYPE;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.BOOLEAN;
import static org.alfresco.service.cmr.version.Version.PROP_DESCRIPTION;
import static org.alfresco.service.cmr.version.VersionType.MINOR;
import static org.alfresco.service.namespace.NamespaceService.CONTENT_MODEL_1_0_URI;
import static org.alfresco.service.namespace.QName.createQName;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OCRExtractAction extends ActionExecuterAbstractBase {

	private static final Log logger = LogFactory.getLog(OCRExtractAction.class);

	private NodeService nodeService;
	private ContentService contentService;
	private VersionService versionService;

	private OCRTransformWorker ocrTransformWorker;

	// Continue current operation in case of OCR error
	private static final String PARAM_CONTINUE_ON_ERROR = "continue-on-error";

	public void init() {
		super.init();
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {

		paramList.add(new ParameterDefinitionImpl(PARAM_CONTINUE_ON_ERROR, BOOLEAN, false,
				getParamDisplayLabel(PARAM_CONTINUE_ON_ERROR)));
	}

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {

		if (!nodeService.hasAspect(actionedUponNodeRef, ASPECT_OCRD)) {

			ContentData contentData = (ContentData) nodeService.getProperty(actionedUponNodeRef, PROP_CONTENT);

			// Exclude folders and other nodes without content
			if (contentData != null) {

				Boolean continueOnError = (Boolean) action.getParameterValue(PARAM_CONTINUE_ON_ERROR);
				if (continueOnError == null)
					continueOnError = true;

				try {
					// Current transaction
					executeImplInternal(actionedUponNodeRef, contentData);
				} catch (Throwable throwableCurrentTransaction) {
					if (continueOnError) {
						logger.warn(actionedUponNodeRef + ": " + throwableCurrentTransaction.getMessage());
					} else {
						throw throwableCurrentTransaction;
					}
				}

			}

		}

	}

	private void executeImplInternal(NodeRef actionedUponNodeRef, ContentData contentData) {

		String originalMimeType = contentData.getMimetype();

		ContentReader reader = contentService.getReader(actionedUponNodeRef, PROP_CONTENT);

		ContentWriter writer = contentService.getTempWriter();
		writer.setMimetype(MIMETYPE_PDF);

		try {
			ocrTransformWorker.transform(reader, writer, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Set initial version if it's a new one
		versionService.ensureVersioningEnabled(actionedUponNodeRef, null);
		if (!versionService.isVersioned(actionedUponNodeRef)) {
			Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
			versionProperties.put(PROP_DESCRIPTION, "OCRd");
			versionProperties.put(PROP_VERSION_TYPE, MINOR);
			versionService.createVersion(actionedUponNodeRef, versionProperties);
		}

		ContentWriter writeOriginalContent = null;
		// Update original PDF file
		if (originalMimeType.equals(MIMETYPE_PDF)) {
			writeOriginalContent = contentService.getWriter(actionedUponNodeRef, PROP_CONTENT, true);
		} else {
			// Create new PDF file
			String fileName = nodeService.getProperty(actionedUponNodeRef, PROP_NAME) + ".pdf";
			Map<QName, Serializable> props = new HashMap<QName, Serializable>(1);
			props.put(PROP_NAME, fileName);
			NodeRef pdfNodeRef = createNode(nodeService.getPrimaryParent(actionedUponNodeRef).getParentRef(), fileName,
					props);
			writeOriginalContent = contentService.getWriter(pdfNodeRef, PROP_CONTENT, true);
			writeOriginalContent.setMimetype(MIMETYPE_PDF);
		}
		writeOriginalContent.putContent(writer.getReader());

		// Set OCRd aspect to avoid future re-OCR process

		Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
		aspectProperties.put(PROP_PROCESSED_DATE, new Date());
		nodeService.addAspect(actionedUponNodeRef, ASPECT_OCRD, aspectProperties);

		// Manual versioning because of Alfresco insane rules for first version
		// content nodes
		versionService.ensureVersioningEnabled(actionedUponNodeRef, null);
		Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
		versionProperties.put(PROP_DESCRIPTION, "OCRd");
		versionProperties.put(PROP_VERSION_TYPE, MINOR);
		versionService.createVersion(actionedUponNodeRef, versionProperties);

	}

	private NodeRef createNode(NodeRef parentNodeRef, String name, Map<QName, Serializable> props) {
		return nodeService.createNode(parentNodeRef, ASSOC_CONTAINS, createQName(CONTENT_MODEL_1_0_URI, name),
				TYPE_CONTENT, props).getChildRef();
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setOcrTransformWorker(OCRTransformWorker ocrTransformWorker) {
		this.ocrTransformWorker = ocrTransformWorker;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

}
