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

import static com.asprise.ocr.Ocr.OUTPUT_FORMAT_PLAINTEXT;
import static com.asprise.ocr.Ocr.RECOGNIZE_TYPE_ALL;
import static com.asprise.ocr.Ocr.SPEED_SLOW;
import static com.asprise.ocr.Ocr.setUp;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static org.apache.pdfbox.pdmodel.PDDocument.loadNonSeq;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import com.asprise.ocr.Ocr;

// Transformer from plain PDF (images attached) to Searchable PDF by using OCR
public class OCRTransformWorker extends ContentTransformerHelper {

	private static final Log logger = LogFactory.getLog(OCRTransformWorker.class);

	public final void transform(ContentReader reader, ContentWriter writer, TransformationOptions options)
			throws Exception {

		try {

			setUp(); // one time setup
			Ocr ocr = new Ocr(); // create a new OCR engine
			ocr.startEngine("por", SPEED_SLOW); // Portoguese
			String result = "";
			try {
				PDDocument document = loadNonSeq(reader.getContentInputStream(), null);
				@SuppressWarnings("unchecked")
				List<PDPage> pdPages = (List<PDPage>) document.getDocumentCatalog().getAllPages();
				for (PDPage pdPage : pdPages) {
					BufferedImage bim = pdPage.convertToImage(TYPE_INT_RGB, 300);
					result += ocr.recognize(bim, RECOGNIZE_TYPE_ALL, OUTPUT_FORMAT_PLAINTEXT);
				}
				document.close();
			} catch (IOException e) {
				logger.error(e);
			}
			ocr.stopEngine();
			writer.putContent(result);

		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
}