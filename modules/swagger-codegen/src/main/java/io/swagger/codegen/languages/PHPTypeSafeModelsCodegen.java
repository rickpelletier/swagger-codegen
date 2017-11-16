package io.swagger.codegen.languages;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenOperation;
import io.swagger.codegen.CodegenModel;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.CodegenResponse;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.codegen.SupportingFile;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PHPTypeSafeModelsCodegen extends DefaultCodegen implements CodegenConfig {
	// @TODO is this actually used?
    protected String invokerPackage = "App";

    public PHPTypeSafeModelsCodegen() {
        super();
        
        //outputFolder = "generated-code/java";
        modelTemplateFiles.put("model.mustache", ".php");
        //apiTemplateFiles.put("api.mustache", ".java");
        templateDir = "php-type-safe-models";
        //apiPackage = "io.swagger.client.api";
        modelPackage = invokerPackage + "\\Swagger\\Models";

        reservedWords = new HashSet<String>(
                Arrays.asList(
                        "__halt_compiler", "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "namespace", "new", "or", "print", "private", "protected", "public", "require", "require_once", "return", "static", "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor")
        );
        
        // ref: http://php.net/manual/en/language.types.intro.php
        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "boolean",
                        "int",
                        "integer",
                        "double",
                        "float",
                        "string",
                        "object",
                        "DateTime",
                        "mixed",
                        "number")
        );

        instantiationTypes.put("array", "array");
        instantiationTypes.put("map", "map");

        // ref: https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#data-types
        typeMapping = new HashMap<String, String>();
        typeMapping.put("integer", "integer");
        typeMapping.put("long", "integer");
        typeMapping.put("float", "float");
        typeMapping.put("double", "double");
        typeMapping.put("string", "string");
        typeMapping.put("byte", "integer");
        typeMapping.put("boolean", "boolean");
        typeMapping.put("date", "\\DateTime");
        typeMapping.put("datetime", "\\DateTime");
        typeMapping.put("file", "\\SplFileObject");
        typeMapping.put("map", "map");
        typeMapping.put("array", "array");
        typeMapping.put("list", "array");
        typeMapping.put("object", "object");
        typeMapping.put("DateTime", "\\DateTime");
        
        // no api files
        apiTemplateFiles.clear();
        // yes model files
        modelTemplateFiles.put("model.mustache", ".php");
        
        String packagePath = invokerPackage;

        //supportingFiles.add(new SupportingFile("swagger_routes.mustache", packagePath.replace('/', File.separatorChar), "Http/swagger_routes.php"));
        supportingFiles.add(new SupportingFile("ModelCollection.mustache", this.modelFileFolder()+File.separatorChar, "ModelCollection.php"));
        supportingFiles.add(new SupportingFile("BaseModel.mustache", this.modelFileFolder()+File.separatorChar, "BaseModel.php"));
        
        cliOptions.add(new CliOption("invokerPackage", "root package for generated code"));
        
    }
    
    public void setInvokerPackage(String invokerPackage) {
        this.invokerPackage = invokerPackage;
    }
    
    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey("invokerPackage")) {
            this.setInvokerPackage((String) additionalProperties.get("invokerPackage"));
        } else {
            //not set, use default to be passed to template
            additionalProperties.put("invokerPackage", invokerPackage);
        }
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
    public String escapeReservedWord(String name) {
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        // PSR-4 style folder structure
        return (outputFolder + "/" + apiPackage()).replace('\\', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        // PSR-4 style folder structure
        return (outputFolder + "/" + modelPackage()).replace('\\', File.separatorChar);
    }

    @Override
    public String getTypeDeclaration(Property p) {
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            return getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            return getSwaggerType(p) + "[string," + getTypeDeclaration(inner) + "]";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            } else if (instantiationTypes.containsKey(type)) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        if (type == null) {
            return null;
        }
        return toModelName(type);
    }

    public String toDefaultValue(Property p) {
        return "null";
    }


    @Override
    public String toVarName(String name) {
        // parameter name starting with number won't compile
        // need to escape it by appending _ at the beginning
        if (name.matches("^[0-9]")) {
            name = "_" + name;
        }

        // return the name in underscore style
        // PhoneNumber => phone_number
        return underscore(name);
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        // model name cannot use reserved keyword
        if (reservedWords.contains(name)) {
            escapeReservedWord(name); // e.g. return => _return
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(name);
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }
    
    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        ArrayList models = (ArrayList) objs.get("models");
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
                ArrayList imports = (ArrayList) objs.get("imports");
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