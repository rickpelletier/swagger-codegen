package io.swagger.codegen.languages;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;

public class PHPTypeSafeModelsCodegen extends AbstractPhpCodegen {
	public PHPTypeSafeModelsCodegen() {
    	super();
    	
    		templateDir = "php-type-safe-models";

    		// no api files
        apiTemplateFiles.clear();
        apiDocTemplateFiles.clear();
        apiTestTemplateFiles.clear();
        
        // just model files
        modelTemplateFiles.clear();
        modelTemplateFiles.put("model.mustache", ".php");
        
        // @TODO implement this?
        modelDocTemplateFiles.clear();
        //modelDocTemplateFiles.put("model_doc.mustache", ".md");
        
        //modelPackage = invokerPackage + "\\" + modelDirName;

        // provide primitives to mustache template
        String primitives = "'" + StringUtils.join(languageSpecificPrimitives, "', '") + "'";
        additionalProperties.put("primitives", primitives);
        
        setSupportingFiles();
    }
	
	private void setSupportingFiles() {
		// super annoying that processOpts doesn't run until AFTER these supporting files would be set in the 
		// constructor.  Due to this we have to catch each relevant property assignment and recreate the supporting
		// files
		supportingFiles.clear();
		// templates are named "Base" to differentiate from regular "model.mustache" - the generated classes will not have "Base"
		// in the class name
		supportingFiles.add(new SupportingFile("BaseModelCollection.mustache", this.toPackagePath(modelPackage, srcBasePath), "ModelCollection.php"));
        supportingFiles.add(new SupportingFile("BaseModel.mustache", this.toPackagePath(modelPackage, srcBasePath), "Model.php"));
	}
	
	@Override
	public void setPackagePath(String packagePath) {
        super.setPackagePath(packagePath);
        setSupportingFiles();
    }
	
	@Override
	public void setSrcBasePath(String srcBasePath) {
		super.setSrcBasePath(srcBasePath);
		setSupportingFiles();
	}
	
	@Override
	public void setModelPackage(String modelPackage) {
		super.setModelPackage(modelPackage);
		setSupportingFiles();
	}
    
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    public String getName() {
        return "php-type-safe-models";
    }

    public String getHelp() {
        return "Generates \"POJO-style\" PHP model objects for use in server or client code.";
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
    	
        @SuppressWarnings("unchecked")
		ArrayList<Object> models = (ArrayList<Object>) objs.get("models");
        String modelPackage = objs.get("package").toString();
        
        for (int i = 0; i < models.size(); i++) {
            
            CodegenModel model = (CodegenModel) ( (HashMap) ( models ).get(i)).get("model");
            // @TODO Adding modelPackage to the model shouldn't be necessary - figure out how to 
            // reference the configuration value from the mustache templates
            //model.modelPackage = modelPackage;
            
            if (model.name.contains("Collection")) {
                //System.out.println("Processing collection model");
                model.isCollection = true;

                // process the imports and get the item type
                @SuppressWarnings("unchecked")
				ArrayList<Object> imports = (ArrayList<Object>) objs.get("imports");
                if (imports.size() > 0) {
                   
                    String importedModelType = null;
                    
                    for(int importsIndex = 0; importsIndex < imports.size(); importsIndex++) {
                        importedModelType = (String) ((HashMap) imports.get(importsIndex)).get("import");
                        //System.out.println(importedModelType);
                             
                        if (importedModelType != "array") {
                            break;
                        }
                    }
                    
                    //remove the model package to get the schema name
                    model.itemType = importedModelType.replace(modelPackage+".", "");  
                }
            }
            
            // flip through the properties and set the isDateTime value on them
            for (int varIndex = 0; varIndex < model.vars.size(); varIndex++) {
                //System.out.println(model.vars.get(varIndex).baseType);
                if ("array".equals(model.vars.get(varIndex).baseType)) {
                    model.vars.get(varIndex).isArray = true;
                }
                if ("\\DateTime".equals(model.vars.get(varIndex).baseType)) {
                    model.vars.get(varIndex).isDateTime = true;
                }
            }
        }
        
        
        
        return objs;
    }
    
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = (Map<String, Object>) objs.get("operations");
        @SuppressWarnings("unchecked")
        List<CodegenOperation> operations = (List<CodegenOperation>) objectMap.get("operation");
        for (CodegenOperation operation : operations) {
            //System.out.println("processing operation: " + operation.nickname);
            operation.httpMethod = operation.httpMethod.toLowerCase();
        }
        return objs;
    }

}