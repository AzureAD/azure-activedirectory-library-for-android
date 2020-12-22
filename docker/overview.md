# Build Pipelines Android Complete Projects

## Introduction

This document describes how to use docker and containers locally, via Azure DevOps or via github actions to build and test android projects.  

### References

- [What's a container?](https://www.redhat.com/en/topics/containers/whats-a-linux-container)
- [Open Container Initiative](https://opencontainers.org/)
- [Linux Containers Project](https://linuxcontainers.org/)
- [Docker - Getting Started](https://docs.docker.com/get-started/overview/)
- [Docker Desktop Installation](https://www.docker.com/products/docker-desktop)
- [Travis CI](https://travis-ci.org/)
- [Github Actions](https://github.com/features/actions)
- [Azure DevOps Pipelines](https://docs.microsoft.com/en-us/azure/devops/pipelines/?view=azure-devops)
- [Intro to Azure Container Registry](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-intro)
- [Push and Pull from Azure Container Registry](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-docker-cli)
- [Self hosted linux build agents](https://docs.microsoft.com/en-us/azure/devops/pipelines/agents/v2-linux?view=azure-devops)
- [Agent Pools](https://docs.microsoft.com/en-us/azure/devops/pipelines/agents/pools-queues?view=azure-devops&tabs=yaml%2Cbrowser)
- [Auth Client Pipelines in Engeering Project](https://identitydivision.visualstudio.com/Engineering/_build?definitionScope=%5CAndroid%5CAuthClient)

### Glossary

- Build agent/server/runner - The server (usually a virtual machine) where the build is performed
  - Azure DevOps calls these "build agents"
  - Github actions call these "runners"
- Build Pipeline/Workflow - The series of steps performed to build and test code
  - Azure DevOps call these pipelines
  - Github actions call these workflows
- Build tasks/actions/recipes - Reusable building blocks (script blocks) that can be used to compose a pipeline/workflow
  - Azure DevOps call these tasks
  - Github actions call these actions
  - Travis calls these recipes
- Container image - All of the information necessary to instantiate a container on Linux or Windows
  - Usually a docker image consists of series of layers each of which describe files installed in order to enable the container to perform the function that you require
- Docker daemon - The docker service that manages the creation and controls the lifetime of a container
- Docker cli - The command line interface used to interact with the docker daemon


## Build background

Currently as of Dec 2020, we run build workflows using the follwoing build systems:

- Travis
- Github Actions
- Azure DevOps (Private Project)

The definition for each of these system is unique with varying controls over their management:

- Travis: Travis YAML File - No longer use travis "tasks", "actions" equivalent and is not script based.
  - Workflow is defined in YAML and under source control
- Github actions: Github actions YAML file - Under source control, but not under restrictions.  Meaning that anyone authorized can change directly without needing to perform a pull request.  These builds use github "actions".
- Azure DevOps - Although Azure DevOps pipelines can be described via YAML and that YAML can be put under source control they are not.  These pipelines also use Azure DevOps tasks.

## Build requirements

- Decouple builds from actions/tasks/recipes - It's not a requirement to take dependency on these things and do so hurts the portability of our build workflows from one system to another.
- Run the same build locally that is being run on the server.  Developers shoudl be able to run the same build locally that is being run on the build server.
- Isolate the build process from build agent/server/runner configuration.  Our builds do not require custom kernels or specific operating systems. 
- Reduce number of endpoints being contacted during build.  Scripts can be used to download and install packages and system images however every endpoint that needs to be contacted introduced another point of failure
- Workflows and workflow output for our open source projects need to be available to external contributors.  Our current azure devops pipelines are not available to them.
- New versions of android, android build tools and CPP build tools are continually being created.  Should be simple to update these in one place and have all build workflows benefit from these changes.

## Plan/Design

- Perform all builds on container images
  - Both github actions and azure devops support container images (specifically via docker although this could change in the future)
- Create container images that correspond to specific android api-levels and build tools and have everything necessary to build:
  - Pure java projects
  - Java + CPP projects
- Create tool to parameterize the creation of container images
  
- Define new generic build workflow that can be executed on both github actions and azure devops
- Replace existing Travis workflows with Azure DevOps pipelines

## Tool pre-reqs

- Docker Desktop - See link in references above.
- Nodejs - Install nodejs: [https://nodejs.org/en/download/](https://nodejs.org/en/download/)
- Azure CLI: [https://docs.microsoft.com/en-us/cli/azure/install-azure-cli](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
  - Needed to push docker images to azure container repository
  - Needed to access/manage Azure Android Build Support resources including:
    - Self-hosted azure build agents
    - Keyvault

## Tool Details & Usage

Dockerfiles for different Android api levels are generated from a template that's found in the "templates" folder.  The template is combined with a parameters file in the parameters folder and outputted to a folder named for the specific API level.  Once generated docker build command is used to create the docker image corresponding to the Dockerfile.

### Node CLI to generate Dockerfiles

Here's an example of using the provided nodejs CLI to generate a new or updated docker image for a specific api level:

```bash
# in the script folder run the following
node ./generate.js generate --params "../parameters/api29.json" --outputPath "../api29"
```

Here's a look at the template

```Dockerfile
#
# THIS IS A GENERATED-FILE.  DO NOT EDIT DIRECTLY
#
FROM {{dockerBaseImage}} as BASE
USER root
ENV SDK_URL="{{androidCommandLineUtilsUrl}}" \
    ANDROID_HOME="/usr/local/android-sdk" \
    CMAKE_BIN_URL="{{cmakeBinUrl}}{{cmakeBinFile}}" \
    CMAKE_TAR_FILE="{{cmakeBinFile}}" \
    CMAKE_HOME="/usr/local/cmake" \
    CMAKE_HOME_BIN="/usr/local/cmake/{{cmakeBinFolder}}/bin" \
    NINJA_BIN_URL="{{ninjaBinUrl}}" \
    NINJA_ZIP_FILE="{{ninjaZipFile}}"
RUN cd ~/.gradle \
    && touch gradle.properties \
    && printf "vstsUsername=VSTS\n" >> gradle.properties \
    && printf "vstsMavenAccessToken=mz3rbwtljuo7g5dt3l4nprqsbtnciuxezzslkuzv6267qka5zv4q\n" >> gradle.properties
RUN apt-get update \
    &&  apt-get -y install build-essential
# Download Android SDK and Fix SDKManager for JDK 11
RUN mkdir "$ANDROID_HOME" .android \
    && cd "$ANDROID_HOME" \
```

Here's a look at the parameters file

```json
{
    "dockerBaseImage": "gradle:6.7.1-jdk11",
    "androidCommandLineUtilsUrl": "https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip",
    "androidApiLevel": "29",
    "androidBuildToolsVersion": "30.0.2",
    "androidNdkVersion": "ndk;21.1.6352462",
    "androidImageVariant": "google_apis",
    "androidImageAbi": "x86_64",
    "cmakeBinUrl": "https://github.com/Kitware/CMake/releases/download/v3.18.5/",
    "cmakeBinFile": "cmake-3.18.5-Linux-x86_64.tar.gz",
    "cmakeBinFolder": "cmake-3.18.5-Linux-x86_64",
    "ninjaBinUrl": "https://github.com/ninja-build/ninja/releases/download/v1.10.2/ninja-linux.zip",
    "ninjaZipFile": "ninja-linux.zip"
}
```

Here's a look at the generated Dockerfile

```Dockerfile
#
# THIS IS A GENERATED-FILE.  DO NOT EDIT DIRECTLY
#
FROM gradle:6.7.1-jdk11 as BASE
USER root
ENV SDK_URL="https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip" \
    ANDROID_HOME="/usr/local/android-sdk" \
    Blah, blah... excluding for brevity

FROM BASE as DBI_UNIT
USER root
ENV PATH="/usr/local/cmake/cmake-3.18.5-Linux-x86_64/bin:/usr/local/android-sdk/emulator:/usr/local/android-sdk/tools:/usr/local/android-sdk/tools/bin:/usr/local/android-sdk/platform-tools:${PATH}" \
    ANDROID_HOME="/usr/local/android-sdk" \
    ANDROID_VERSION=29 \
    ANDROID_BUILD_TOOLS_VERSION=30.0.2 \
    ANDROID_NDK_VERSION=ndk;21.1.6352462
# Install Android Build Tool and Libraries
RUN $ANDROID_HOME/tools/bin/sdkmanager --update
RUN touch /root/.android/repositories.cfg
RUN $ANDROID_HOME/tools/bin/sdkmanager "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
    "platforms;android-${ANDROID_VERSION}" \
    "platform-tools" \
    "${ANDROID_NDK_VERSION}" \
    "emulator" 

FROM DBI_UNIT as DBI_INSTRUMENTED
USER root
ENV ANDROID_HOME="/usr/local/android-sdk" \
    SYSTEM_IMAGE="system-images;android-29;google_apis;x86_64" \
    SYSTEM_IMAGE_TAG="google_apis"
RUN apt-get -y install qt5-default
RUN $ANDROID_HOME/tools/bin/sdkmanager "emulator"
RUN yes | $ANDROID_HOME/tools/bin/sdkmanager $SYSTEM_IMAGE
RUN echo no | $ANDROID_HOME/tools/bin/avdmanager create avd --force -g $SYSTEM_IMAGE_TAG -n test -k $SYSTEM_IMAGE
```

>NOTE: That each build stage in the docker file can be given a name.  This name can be used when building the image to specific which specific target you want to create.  

### Docker command to build an image
Here's an example of building the DBI_INSTRUMENTED target

```bash
# In the folder containing the api level Dockerfile... don't forget the period to indicate use the dockerfile from this directory
docker build --target DBI_INSTRUMENTED -t dbi-instrumented-api29 .
```

>NOTE: Image generation effectively runs all of the scripts found in the Dockerfile, so it can take some time.

### Docker command to run image locally
Run a build using a container locally
```bash
# run the following from the folder/directory on your host containing the source code for your project.  This folder will then be mounted as a volume
# replace PWD with cd to get current directory
# Privileged flag required when running tests using an emulator
# -it flags make the session interactive
# specifying /bin/bash will open an interactive terminal session within the container.  From there you can start an emulator or run a build of your code
# you can optionally specify max number of cpus and memory
# This example is pulling the image from the azure container registry
docker run -it --privileged --cpus="3" --memory="12g" -v "$PWD":/home/gradle/ -w /home/gradle/ authclient.azurecr.io/samples/dbi-instrumented-api30 /bin/bash

```

### Docker commands to tag and push an image to Azure Container Registry

```bash
# Apply a tag specifying the container registry service location... to the the locally generated images
docker tag dbi-unit-api30 authclient.azurecr.io/samples/dbi-unit-api30
docker tag dbi-instrumented-api30 authclient.azurecr.io/samples/dbi-instrumented-api30

# Login in to azure container registry using azure cli
az acr login --name authclient

# Push to the authclient container registry
docker push authclient.azurecr.io/samples/dbi-instrumented-api30

```

### Other docker commands

Here are some other useful commands:

```bash
#lists all running containers
docker ps 

#list all containers running or not.  These take up diskspace!
docker container ls -a

#prune containers that have been shutdown (if we're dynamically running these instead of specifying a name... these keep getting created)
docker system prune

```

## Azure DevOps YAML to run tests via docker images

Here's an exmaple of the azure_pipelines.yaml file that is used to define out pipeline

> NOTE: We are not using azure devops tasks beyone the simple script task intentionally.

```yaml
name: Instrumented Tests

trigger:
- main

pool:
 name: DockerBuildAgents

workspace:
  clean: all

steps:
- script: |
    docker --version
    echo =============================================
    echo Kill all running containers if existing
    echo =============================================
    docker container kill $(docker ps -q)
    echo =============================================
    echo Run unit and instrumented inside docker container
    echo =============================================
    docker run --privileged --cpus="3" --memory="12g" -v "$PWD":/home/gradle/ -w /home/gradle/ authclient.azurecr.io/samples/dbi-instrumented-api30 sh scripts/run-instrumented-tests.sh
  displayName: 'Build and test inside docker container'
- script: |
    echo =============================================
    echo Cleaning up build output that is owned by docker user rather than agent user
    echo =============================================
    docker run --privileged --cpus="3" --memory="12g" -v "$PWD":/home/gradle/ -w /home/gradle/ authclient.azurecr.io/samples/dbi-instrumented-api30 gradle clean
    echo =============================================
    echo prune containers to avoid running out of disk space - shutdown containers still exist on disk
    echo =============================================
    docker system prune -f
    echo =============================================
    echo Dump environment variables for build agent
    echo =============================================
    env
  displayName: 'Cleanup'
```

Here's the script invoked by the pipeline to start the emulator and run the instrumented tests
```bash
#!/bin/bash
echo =============================================
echo Starting ADB Daemon
echo =============================================
adb start-server
echo =============================================
echo Starting Emulator
echo =============================================
emulator @test -no-window -no-audio -wipe-data &
echo =============================================
echo Gradle Version Info
echo =============================================
gradle -version
echo =============================================
echo Running instrumented tests
echo =============================================
gradle common:connectedDebugAndroidTest -i

```

>NOTE: The docker container linux user and the agent linux user are not the same.  Since the docker container performs the build.  The build artifacts are owned by the docker container.  In the event that the docker container is unable to clean up for itself.  The agent will need to have it's /myagent/_work directory manually cleaned.  I'm still investigating a more full proof solution for this problem.

## Build Agents and Agent Pool

Since recent android emulators require hardware acceleration we need to run our instrumented tests on servers that support nested virtualization.  Unfortunately neither Azure DevOps Micorsoft Build Agents or Github Runners support this.  

Both github actions and Azure DevOps support self-hosted agents/runner.  In our case we created a new agent pool called: "DockerBuildAgents" and added a new self-hosted linux Azure VM based on Ubuntu 18.05-LTS.  

>NOTE: I didn't have permission to create a new agent pool in IDDP and since we want over time to migrate our pipelines to the IdentityDivision Engineering project I went ahead and create the new pool and agent in that project.  Interestingly I did have permissions there so I'm assuming that all of us do.

Only our existing instrumented test pipeline has permisisons to use this pool, but as we add additional agents to the pool we can consider increasing the availability to other pipelines and indeed to other types of builds and teams.  

>NOTE: The first time an new pipeline attempts to use the DockerBuildAgents pool the Azure DevOPs UI will fail the build and prompt the user for permission.

### Agent prep

I used the Azure CLI to provision the Azure Linux VM.  I've included the commands used to create it below:

```bash

# login into azure cli
az login

# Note please change subscription to the subscription in which you want to act.. the default is correct for me, but likely not for you!

# Get the list of Azure Sites available to me
az account list-locations \
  --query '[].{Location:displayName,Name:name}' \
  --output table
  
  # I chose to use westus2
  #westus2
  
  # Get the list of VM Sizes
  az vm list-sizes \
   --location westus2 \
   --query '[].{Name:name,CPU:numberOfCores,Memory:memoryInMb}' \
   --output table | grep _v3
   
   # I confirmed via documentation that this one supported nested virtualization
   # Standard_D4_v3          4      16384
   
   # Get the list of available virtual machine OS images
   az vm image list \
  --location westus2 \
  --offer ubuntu \
  --sku 20.04 \
  --all \
  --query '[].urn' \
  --output table  
  
  # We're using ubuntu
  #Canonical:UbuntuServer:18.04-LTS:18.04.202012111

  # Let's put the new Azure VM in an azure resource group
  $ ./az group create \
  --name androidbuildagents \
  --location westus2
  
  # Let's create the Azure VM
  $ ./az vm create \
  --name androidagent1 \
  --resource-group androidbuildagents \
  --size Standard_D4_v3  \
  --image Canonical:UbuntuServer:18.04-LTS:latest \
  --generate-ssh-keys \
  --admin-username android \
  --storage-sku Standard_LRS
  
  # Take note of where it puts the SSH keys and store these in keyvault
  '/home/shane/.ssh/id_rsa' and '/home/shane/.ssh/id_rsa.pub' have been generated under ~/.ssh 
  
  
  # take note of the public IP ... this will be need to connect to the VM via SSH
  {- Finished ..
  "fqdns": "",
  "id": "/subscriptions/cde31ea7-d66a-4743-af52-1d2c0940779c/resourceGroups/androidbuildagents/providers/Microsoft.Compute/virtualMachines/androidagent1",
  "location": "westus2",
  "macAddress": "00-0D-3A-FE-E0-0B",
  "powerState": "VM running",
  "privateIpAddress": "10.0.0.4",
  "publicIpAddress": "52.143.102.122",
  "resourceGroup": "androidbuildagents",
  "zones": ""
}
```

#### Installing agent client, docker & pulling images

Please follow the linked instructions above to install:

- Docker 
- Agent client software

Once installed and configured correctly a new agent will be visible to the pool and the agent status will be online.  

Once this is done be sure to pull the images that your builds are going to required.  Pull the images ahead of time speeds up build tiems signficantly as there is no need to download them ahead of using them.  They are available locally to the build.



 




