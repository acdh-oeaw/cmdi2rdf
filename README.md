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
java -Dcmdi2rdf.properties=<path to you cmdi2rdf.properties> -jar cmdi2rdf-0.0.1-SNAPSHOT-jar-with-dependencies.jar

The program uses a property file cmdi2rdf.properties which is in the resources folder of the project. You have either to adapt the included property file to your needs before building the project or you can pass the pass the path to your property file as system parameter (-D) in the way shown above. **Since the parthenos_mapping project uses JAXB you must make sure to include the necessary packages if you run the program with JRE >= version 9**  