package mat.model.cql.parser;

import java.util.ArrayList;
import java.util.List;

import mat.model.cql.parser.CQLFunctionModelObject.FunctionArgument;

public class CQLDefinitionModelObject extends CQLBaseModelDefinitionObject implements CQLBaseStatementInterface{

	private String expression = "";
	private List<String> childTokens = new ArrayList<String>();
	
	/**
	 * This is a list of all Definition object that are being 'referred to'/'called from' from this definition.
	 */
	private List<CQLDefinitionModelObject> referredToDefinitions = new ArrayList<CQLDefinitionModelObject>();
	
	/**
	 * This is list of all definitions that this definition is 'referred by'/'called at'.
	 */
	private List<CQLDefinitionModelObject> referredByDefinitions = new ArrayList<CQLDefinitionModelObject>();
	
	/**
	 * This is a list of all functions 'referred to'/'called from' from this definition.
	 */
	private List<CQLFunctionModelObject> referredToFunctions = new ArrayList<CQLFunctionModelObject>();
	
	/**
	 * This is list of all functions that this definition is 'referred by'/'called at'.
	 */
	private List<CQLFunctionModelObject> referredByFunctions = new ArrayList<CQLFunctionModelObject>();
	
	
	private List<CQLValueSetModelObject> referredToValueSets = new ArrayList<CQLValueSetModelObject>();
	
	/**
	 * This is list of all functions that this function is 'referred by'/'called by'.
	 */
	private List<CQLValueSetModelObject> referredByValueSets = new ArrayList<CQLValueSetModelObject>();
	
	
	private List<CQLParameterModelObject> referredToParameters = new ArrayList<CQLParameterModelObject>();
	
	/**
	 * This is list of all functions that this function is 'referred by'/'called by'.
	 */
	private List<CQLParameterModelObject> referredByParameters = new ArrayList<CQLParameterModelObject>();
	
	private List<CQLCodeModelObject> referredToCodes = new ArrayList<CQLCodeModelObject>();
	
	//Boolean field to indicate If this definition might be a possible supplemental definition.
	private boolean possibleSupplementalDef;
	
	
	
	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	@Override
	/**
	 * Override this to do nothing, since Context definitions don't have a 'version' 
	 * attribute.
	 */
	public String getVersion() {
		return "";
	}
	
	@Override
	/**
	 * Override this to do nothing, since Context definitions don't have a 'version' 
	 * attribute.
	 */
	public void setVersion(String version) {
	}
	
	@Override
	/**
	 * Override this to do nothing, since Context definitions don't have a 'typeSpecifier' 
	 * attribute.
	 */
	public String getTypeSpecifier() {
		return "";
	}

	@Override
	/**
	 * Override this to do nothing, since Context definitions don't have a 'typeSpecifier' 
	 * attribute.
	 */
	public void setTypeSpecifier(String typeSpecifier) {
	}

	@Override
	/**
	 * Override this to do nothing, since Context definitions don't have a 'defaultExpression' 
	 * attribute.
	 */
	public String getDefaultExpression() {
		return "";
	}
	
	@Override
	/**
	 * Override this to do nothing, since Context definitions don't have a 'defaultExpression' 
	 * attribute.
	 */
	public void setDefaultExpression(String defaultExpression) {		
	}

	public List<String> getChildTokens() {
		return childTokens;
	}

	public void setChildTokens(List<String> childTokens) {
		this.childTokens = childTokens;
	}

	public List<CQLDefinitionModelObject> getReferredToDefinitions() {
		return referredToDefinitions;
	}

	public void setReferredToDefinitions(List<CQLDefinitionModelObject> referredToDefinitions) {
		this.referredToDefinitions = referredToDefinitions;
	}

	public List<CQLDefinitionModelObject> getReferredByDefinitions() {
		return referredByDefinitions;
	}

	public void setReferredByDefinitions(List<CQLDefinitionModelObject> referredByDefinitions) {
		this.referredByDefinitions = referredByDefinitions;
	}
	
	@Override
	public String toString() {
		StringBuffer opString = new StringBuffer();
		
		opString.append("\r\n"+this.getIdentifier()+"\r\n");
		opString.append(this.getChildTokens()+"\r\n");
		opString.append("Definitions referred to by "+this.getIdentifier()+":");
		
		for(CQLDefinitionModelObject referredToDefinition:this.getReferredToDefinitions()){
			opString.append(referredToDefinition.getIdentifier()+",");
		}
		
		opString.append("\r\nDefinitions referring to "+this.getIdentifier() + ":");
		
		for(CQLDefinitionModelObject referredByDefinition:this.getReferredByDefinitions()){
			opString.append(referredByDefinition.getIdentifier()+",");
		}
		
		return opString.toString();
	}

	public List<CQLFunctionModelObject> getReferredToFunctions() {
		return referredToFunctions;
	}

	public void setReferredToFunctions(List<CQLFunctionModelObject> referredToFunctions) {
		this.referredToFunctions = referredToFunctions;
	}

	public List<CQLFunctionModelObject> getReferredByFunctions() {
		return referredByFunctions;
	}

	public void setReferredByFunctions(List<CQLFunctionModelObject> referredByFunctions) {
		this.referredByFunctions = referredByFunctions;
	}

	@Override
	public List<FunctionArgument> getArguments() {
		return new ArrayList<CQLFunctionModelObject.FunctionArgument>();
	}

	@Override
	public List<CQLParameterModelObject> getReferredByParameters() {
		return referredByParameters;
	}

	@Override
	public List<CQLValueSetModelObject> getReferredToValueSets() {
		return referredToValueSets;
	}

	@Override
	public List<CQLValueSetModelObject> getReferredByValueSets() {
		return referredByValueSets;
	}

	@Override
	public List<CQLParameterModelObject> getReferredToParameters() {
		return referredToParameters;
	}

	public void setReferredToValueSets(List<CQLValueSetModelObject> referredToValueSets) {
		this.referredToValueSets = referredToValueSets;
	}

	public void setReferredByValueSets(List<CQLValueSetModelObject> referredByValueSets) {
		this.referredByValueSets = referredByValueSets;
	}

	public void setReferredToParameters(List<CQLParameterModelObject> referredToParameters) {
		this.referredToParameters = referredToParameters;
	}

	public void setReferredByParameters(List<CQLParameterModelObject> referredByParameters) {
		this.referredByParameters = referredByParameters;
	}

	public void setPossibleSupplementalDef(boolean possibleSupplementalDef) {
		this.possibleSupplementalDef = possibleSupplementalDef;		
	}
	
	public boolean isPossibleSupplementalDef(){
		return this.possibleSupplementalDef;
	}

	public List<CQLCodeModelObject> getReferredToCodes() {
		return referredToCodes;
	}

	public void setReferredToCodes(List<CQLCodeModelObject> referredToCodes) {
		this.referredToCodes = referredToCodes;
	}


}
