package mat.client.clause.cqlworkspace;

import org.gwtbootstrap3.client.ui.Label;
import org.gwtbootstrap3.client.ui.constants.LabelType;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.ycp.cs.dh.acegwt.client.ace.AceEditor;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorMode;
import edu.ycp.cs.dh.acegwt.client.ace.AceEditorTheme;
import mat.client.shared.SpacerWidget;

public class CQLViewCQLView {
	
	SimplePanel cqlViewSimpleP = new SimplePanel();
	/** The cql ace editor. */
	private AceEditor cqlAceEditor = new AceEditor();
	public CQLViewCQLView(){
		cqlViewSimpleP.clear();
		cqlAceEditor.startEditor();
	}
	
	public SimplePanel buildView(){
		cqlViewSimpleP.clear();
		VerticalPanel cqlViewVP = new VerticalPanel();
		cqlViewSimpleP.getElement().setId("ViewCQl_SimplePanel");
		cqlViewVP.getElement().setId("ViewCQl_VPanel");
		
		
		cqlAceEditor.setMode(AceEditorMode.CQL);
		cqlAceEditor.setTheme(AceEditorTheme.ECLIPSE);

		cqlAceEditor.getElement().getStyle().setFontSize(14, Unit.PX);
		cqlAceEditor.setSize("675px", "500px");
		cqlAceEditor.setAutocompleteEnabled(true);
		cqlAceEditor.setReadOnly(true);
		cqlAceEditor.setUseWrapMode(true);
		cqlAceEditor.clearAnnotations();
		cqlAceEditor.redisplay();
		Label viewCQlFileLabel = new Label(LabelType.INFO);
		viewCQlFileLabel.setId("viewCQl_Lbl");
		viewCQlFileLabel.setText("View CQL file here");
		viewCQlFileLabel.setTitle("View CQL file here");
		cqlViewVP.add(new SpacerWidget());
		cqlViewVP.add(new SpacerWidget());
		
		cqlViewVP.add(viewCQlFileLabel);
		cqlViewVP.add(new SpacerWidget());
		cqlViewVP.add(new SpacerWidget());
		cqlViewVP.add(cqlAceEditor);
		cqlViewSimpleP.add(cqlViewVP);
		cqlViewSimpleP.setStyleName("cqlRightContainer");
		cqlViewSimpleP.setWidth("700px");
		cqlViewSimpleP.setStyleName("marginLeft15px");
		return cqlViewSimpleP;
	}

	
	public AceEditor getCqlAceEditor() {
		return cqlAceEditor;
	}

	public void setCqlAceEditor(AceEditor cqlAceEditor) {
		this.cqlAceEditor = cqlAceEditor;
	}
	
	public void resetAll() {
		getCqlAceEditor().setText("");
	}
}
