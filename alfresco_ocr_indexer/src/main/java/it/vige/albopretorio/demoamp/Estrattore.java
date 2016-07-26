package it.vige.albopretorio.demoamp;

import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.apache.commons.logging.LogFactory.getLog;

import java.io.InputStream;
import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;

public class Estrattore extends ActionExecuterAbstractBase {
	private Log log = getLog(Estrattore.class);

	private ContentService contentService;

	public final static String NAME = "estrattore-txt";

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
	}

	/**
	 * @see org.alfresco.repo.action.executer.ActionExecuter#execute(org.alfresco.repo.ref.NodeRef,
	 *      org.alfresco.repo.ref.NodeRef)
	 */
	@Override
	public void executeImpl(Action ruleAction, NodeRef actionedUponNodeRef) {
		InputStream inputStream = contentService.getReader(actionedUponNodeRef, PROP_CONTENT).getContentInputStream();
		log.debug(inputStream);
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
}
