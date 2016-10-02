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
package it.vige.albopretorio.site;

import org.alfresco.repo.admin.patch.impl.SiteLoadPatch;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PersonService;

public class AlboPretorioSite extends SiteLoadPatch {

	private final static String M_JACKSON = "mjackson";
	private final static String SITE_COLLABORATOR = "GROUP_site_albopretorio_SiteCollaborator";
	private AuthorityService authorityService;
	private PersonService personService;

	@Override
	protected String applyInternal() throws Exception {
		String result = super.applyInternal();
		NodeRef person = personService.getPerson(M_JACKSON);
		if (person != null)
			authorityService.addAuthority(SITE_COLLABORATOR, M_JACKSON);
		return result;
	}

	@Override
	public void setAuthorityService(AuthorityService authorityService) {
		super.setAuthorityService(authorityService);
		this.authorityService = authorityService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

}
