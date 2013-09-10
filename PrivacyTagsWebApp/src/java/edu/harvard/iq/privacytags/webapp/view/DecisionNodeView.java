package edu.harvard.iq.privacytags.webapp.view;

import edu.harvard.iq.privacytags.model.PrivacyTagSet;
import edu.harvard.iq.privacytags.model.questionnaire.Answer;
import edu.harvard.iq.privacytags.model.questionnaire.DecisionNode;
import edu.harvard.iq.privacytags.webapp.boundary.App;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author michael
 */
@ManagedBean
@RequestScoped
public class DecisionNodeView {
	
	@ManagedProperty(value="#{param.nodeId}")
	private String nodeId;
	
	@ManagedProperty( value="#{App}" )
	private App app;
	
	/**
	 * Creates a new instance of DecisionNodeView
	 */
	public DecisionNodeView() {
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public App getApp() {
		return app;
	}

	public void setApp(App app) {
		this.app = app;
	}
	
	public DecisionNode getNode() {
		return app.getDecisionNode(nodeId);
	}
	
	public DecisionNode getYesNode() {
		return hasNode() ? getNode().getNodeFor(Answer.YES) : null;
	}
	
	public boolean hasYesNode() {
		return getYesNode() != null;
	}
	
	public DecisionNode getNoNode() {
		return hasNode() ? getNode().getNodeFor(Answer.NO) : null;
	}
	
	public boolean hasNoNode() {
		return getNoNode() != null;
	}
	
	public DecisionNode getParentNode() {
		return hasNode() ? getNode().getParent() : null;
	}
	
	public boolean hasParentNode() {
		return getParentNode() != null;
	}
	
	public boolean hasNode() {
		return getNode() != null;
	}
	
	public boolean hasNodeFor( Answer a ) {
		return hasNode() && (getNode().getNodeFor(a)!=null);
	}
	
	public PrivacyTagSet tags() {
		return hasNode() ? getNode().getAbsoluteAssumption() : null;
	}
}