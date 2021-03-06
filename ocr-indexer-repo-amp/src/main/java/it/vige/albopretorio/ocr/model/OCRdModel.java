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
package it.vige.albopretorio.ocr.model;

import static org.alfresco.service.namespace.QName.createQName;

import org.alfresco.service.namespace.QName;

public interface OCRdModel {
	static final String URI = "http://vige.it/model/content/1.0";
	static final QName ASPECT_OCRD = createQName(URI, "ocrd");
	static final QName PROP_PROCESSED_DATE = createQName(URI, "processedDate");
	static final QName MODEL_OCRD = createQName(URI, "content");
}
