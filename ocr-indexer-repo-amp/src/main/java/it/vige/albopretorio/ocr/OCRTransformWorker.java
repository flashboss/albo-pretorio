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

import static com.asprise.ocr.Ocr.LANGUAGE_POR;
import static com.asprise.ocr.Ocr.OUTPUT_FORMAT_PDF;
import static com.asprise.ocr.Ocr.PROP_PDF_OUTPUT_FILE;
import static com.asprise.ocr.Ocr.RECOGNIZE_TYPE_ALL;
import static com.asprise.ocr.Ocr.SPEED_SLOW;
import static com.asprise.ocr.Ocr.setUp;
import static org.alfresco.util.TempFileProvider.createTempFile;

import java.io.File;
import java.util.Properties;

import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.TransformationOptions;

import com.asprise.ocr.Ocr;

// Transformer from plain PDF (images attached) to Searchable PDF by using OCR
public class OCRTransformWorker extends ContentTransformerHelper {

	private MimetypeService mimetypeService;

	public final void transform(ContentReader reader, ContentWriter writer, TransformationOptions options)
			throws Exception {

		try {

			String sourceMimetype = getMimetype(reader);
			String sourceExtension = mimetypeService.getExtension(sourceMimetype);
			File sourceFile = createTempFile(getClass().getSimpleName() + "_source_", "." + sourceExtension);
			reader.getContent(sourceFile);

			String path = sourceFile.getAbsolutePath();
			String targetPath = path.substring(0, path.toLowerCase().indexOf(".pdf")) + "_ocr.pdf";
			File targetFile = new File(targetPath);

			setUp(); // one time setup
			Ocr ocr = new Ocr(); // create a new OCR engine
			ocr.startEngine(LANGUAGE_POR, SPEED_SLOW); // Portoguese
			Properties props = new Properties();
			props.setProperty(PROP_PDF_OUTPUT_FILE, targetPath);
			String output = ocr.recognize(new File[] { sourceFile }, RECOGNIZE_TYPE_ALL, OUTPUT_FORMAT_PDF, props);

			if (output != null)
				writer.putContent(targetFile);

		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public void setMimetypeService(MimetypeService ms) {
		mimetypeService = ms;
	}
}