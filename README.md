# The cmdi2rdf project

## Description
This project is an umbrella project to create rdf files for the Parthenos project from CMDI files from the Clarin project. It uses the parthenos_mapping project 
(see https://github.com/acdh-oeaw/parthenos_mapping) to generate profile specific mappings and the x3ml project (see https://github.com/isl/x3ml) to generate 
rdf files with regard to these mappings. 

## Workflow
The program open recursively all cmdi files from a given directory (property DIR_CMDI). It reads each file as a string into memory and replaces all occurrences of 
the term »hdl:« by »http://hdl.handle.net«. Next it extracts the profile ID by searching in the first 200 bytes of the string for the pattern »p_\d+« (a p underscore, 
followed by a number). The profile ID serves a a key to look up a mapping in a hashmap. If the key doesn't exist so far, it is generated with the help of the 
parthenos_mapping project and stored as a byte array in the hashmap. 
The cmdi file and the specific mapping are passed to x3ml project to generate the rdf output. This output is stored in a given directory (property DIR_RDF). The output file name corresponds to the input file name, while the suffix .xml is replaced for .rdf. Since input files are organized in collection specific sub-directories, this organization is kept in the output to prevent overwriting in case the same file name is used in more than one collection. 

 

## Building the project
As mentioned before this project depends an the two project pathenos_mapping and x3ml. Both projects are on github. They have to be downloaded and build first **before** building the cmdi2rdf project

## Running the project
java -Dcmdi2rdf.properties=`<path to your cmdi2rdf.properties>` -jar cmdi2rdf-1.0-jar-with-dependencies.jar

The program uses a property file cmdi2rdf.properties which is in the resources folder of the project. You have either to adapt the included property file to your needs before building the project or you can pass the pass the path to your property file as system parameter (-D) in the way shown above. 

## The cmdi2rdf.properties
#### FORMAT_OUTPUT
Currently only `application/rdf+xml` is supported

#### DIR_CMDI
Input directory for cmdi-files. The directory is scanned recursively for files with suffix .xml

####  DIR_RDF
Output directory for rdf files. Other output formats might be supported later

#### DIR_MAPPING
Direcory for profile specific mapping files. If `USE_MAPPING_CACHE` is set to true the content is used for processing. Otherwise profile specific mappings are generated with each start of the program

#### USE_MAPPING_CACHE
If set to true the program doesn't generate mappings but uses existing mappings from directory `DIR_MAPPING`

#### FILE_MAPPING
This is usually the path to the file `CMDI2CIDOC.xml`, which is a part of the pathenos_mapping project

#### FILE_POLICY
This is usually the path to the file `policy.xml`, which is a part of the pathenos_mapping project

#### CONDITION_DEFAULT
Comma separated list of conditions. These conditions are used as default if not defined otherwise in `CONDITIONS`

#### CONDITIONS
The property allows to define profile specific conditions. Each condition(s)-profile(s) set follows the pattern:

`<condition1[,condition2,...]>:<profileID[,profileID,...]>`

means we have one ore more comma separated list of conditions, a colon and a comma separated list of profileIDs. Multiple condition(s)-profile(s) sets use the semicolon as a separator. 

Example:

`CONDITIONS=creator-software,service:p_1295178776924,p_1299509410083;creator-software,actor:p_1295178776925,p_1299509410084`

#### FILE_SIZE_LIMIT
Maximum file size in bytes of the CMDI input file. 

#### THREAD_POOL_SIZE
Processing of CDMI files is done in a multi-threaded way. The number of threads depends on the hardware configuration  