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

import static it.vige.albopretorio.ocr.model.OCRdModel.*;
import static it.vige.albopretorio.ocr.model.OCRdModel.PROP_PROCESSED_DATE;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_PDF;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.BOOLEAN;
import static org.alfresco.service.cmr.version.VersionType.MINOR;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.apache.pdfbox.pdmodel.PDDocument.loadNonSeq;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.FileContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

public class OCRExtractAction extends ActionExecuterAbstractBase {

	private static final Log logger = getLog(OCRExtractAction.class);

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
		if (originalMimeType.equals(MIMETYPE_PDF)) {

			ContentReader reader = contentService.getReader(actionedUponNodeRef, PROP_CONTENT);
			if (isBinary(reader)) {
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
					versionProperties.put(Version.PROP_DESCRIPTION, "OCRd");
					versionProperties.put(VersionModel.PROP_VERSION_TYPE, MINOR);
					versionService.createVersion(actionedUponNodeRef, versionProperties);
				}

				ContentWriter writeOriginalContent = null;
				// Update original PDF file
				writeOriginalContent = contentService.getWriter(actionedUponNodeRef, PROP_CONTENT, true);
				writeOriginalContent.putContent(writer.getReader());

				versionService.ensureVersioningEnabled(actionedUponNodeRef, null);
				Map<String, Serializable> versionProperties = new HashMap<String, Serializable>();
				versionProperties.put(Version.PROP_DESCRIPTION, "OCRd");
				versionProperties.put(VersionModel.PROP_VERSION_TYPE, MINOR);
				versionService.createVersion(actionedUponNodeRef, versionProperties);

			}
			nodeService.setType(actionedUponNodeRef, MODEL_OCRD);
			// Set OCRd aspect to avoid future re-OCR process
			Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
			aspectProperties.put(PROP_PROCESSED_DATE, new Date());
			nodeService.addAspect(actionedUponNodeRef, ASPECT_OCRD, aspectProperties);
		}
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

	public void setOcrTransformWorker(OCRTransformWorker ocrTransformWorker) {
		this.ocrTransformWorker = ocrTransformWorker;
	}

	private boolean isBinary(ContentReader reader) {
		String filePath = ((FileContentReader) reader).getFile().getAbsolutePath();
		String content = "";
		try (FileInputStream in = new FileInputStream(filePath)) {
			content = getText(in);
		} catch (IOException e) {
			logger.error(e);
		}

		return content.isEmpty();
	}

	public static String getText(InputStream in) throws IOException {
		PDDocument document = loadNonSeq(in, null);
		String content = new PDFTextStripper().getText(document).trim();
		document.close();
		return content;
	}

}
