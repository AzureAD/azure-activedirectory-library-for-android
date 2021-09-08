### Overview 

This is 'how to' Regenerate Client for LabApi using the latest labapi [swagger spec](https://msidlab.com/swagger/v1/swagger.json). See [Swagger codegen](https://swagger.io/tools/swagger-codegen/) for more. 

### Steps: 

**a) Install/Download**

There are 2 ways to use swagger-codegen,
1) To download jar [here](https://github.com/swagger-api/swagger-codegen). 
2) To Install, run 'brew install swagger-codegen' on Mac, for Windows see Perquisite's section from [here](https://github.com/swagger-api/swagger-codegen)

**b) Place gen files**

Place these 2 in root for audroid-complete/common project. 

1) swagger.json ->https://msidlab.com/swagger/v1/swagger.json
2) config.json ->
{
    "modelPackage": "com.microsoft.identity.internal.test.labapi.model",
    "apiPackage": "com.microsoft.identity.internal.test.labapi.api",
    "invokerPackage": "com.microsoft.identity.internal.test.labapi" }

**c) Run**

Command used to generate client code:

If using **jar**: 
swagger-codegen-cli-3.0.25.jar generate -i ~/{path_to_common}/swagger.json -c ~/{path_to_common}/config.json -l java -o labapi

If using **system install**: 
swagger-codegen generate -i ~/{path_to_common}/swagger.json -c ~/{path_to_common}/config.json -l java -o labapi

