# Kite - Morphlines Crunch Tool

## STATUS

EXPERIMENTAL

## Building

This step builds the software from source. It also runs the unit tests.

```bash
git clone https://github.com/kite-sdk/kite.git
cd kite
#git tag # list available releases
#git checkout master
#git checkout release-0.15.0 # or whatever the latest version is
mvn clean install -DskipTests -DjavaVersion=1.7
cd kite-morphlines/kite-morphlines-crunch-tool
mvn clean package -DjavaVersion=1.7 # This can take several minutes
ls target/kite-morphlines-crunch-tool-*-job.jar
```

## Getting Started

The steps below assume you have already built the software as described above.
In addition, below we assume a working MapReduce cluster, for example as installed by Cloudera Manager.

## MorphlineCrunchTool

`MorphlineCrunchTool` is a MapReduce ETL batch job driver that pipes data from an input source (splitable or non-splitable
HDFS files or Kite datasets) to an output target (HDFS files or Kite dataset or Apache Solr), and
along the way runs the data through a (optional) Morphline for extraction and transformation,
followed by a (optional) Crunch join followed by a (optional) arbitrary custom sequence of Crunch
processing steps.

More details are available through the command line help:

<pre>
$ hadoop jar target/kite-morphlines-crunch-tool-*-job.jar org.kitesdk.morphline.crunch.tool.MorphlineCrunchTool --help

usage: hadoop [GenericOptions]... jar kite-morphlines-crunch-tool-*-job.jar org.kitesdk.morphline.crunch.tool.MorphlineCrunchTool
       [--input-dataset-uri DATASET_URI]
       [--input-dataset-repository REPOSITORY_URI] [--input-dataset-include STRING]
       [--input-dataset-exclude STRING] [--input-file-list URI]
       [--input-file-format FQCN] [--input-file-projection-schema FILE]
       [--input-file-reader-schema FILE] [--output-dataset-repository REPOSITORY_URI]
       [--output-dataset-name STRING] [--output-dataset-schema FILE]
       [--output-dataset-format STRING] [--output-dataset-partition-strategy FILE]
       [--output-dataset-column-mapping FILE] [--output-write-mode STRING]
       [--morphline-file FILE] [--morphline-id STRING] [--help] [--mappers INTEGER]
       [--dry-run] [--log4j FILE] [--verbose] [HDFS_URI [HDFS_URI ...]]

MapReduce ETL batch job driver that pipes data  from an input source (splitable or non-
splitable HDFS files or Kite datasets) to an  output target (HDFS files or Kite dataset
or Apache Solr), and along the  way  runs  the  data through a (optional) Morphline for
extraction and transformation, followed  by  a  (optional)  Crunch  join  followed by a
(optional) arbitrary  custom  sequence  of  Crunch  processing  steps.  The  program is
designed for flexible, scalable  and  fault-tolerant  batch  ETL  pipeline  jobs. It is
implemented as an Apache Crunch  pipeline  and  as  such  can  run on either the Apache
Hadoop MapReduce or Apache Spark execution engine.

The program proceeds in several consecutive phases, as follows: 

1) Randomization phase: This (parallel) phase  randomizes  the list of HDFS input files
in order to spread ingestion load more evenly  among the mapper tasks of the subsequent
phase. This phase is only executed for non-splitables files, and skipped otherwise.

2) Extraction phase: This (parallel)  phase  emits  a  series  of HDFS input file paths
(for non-splitable files) or a series  of  input  data  records (for splitable files or
Kite datasets), in the form of a Crunch PCollection. 

3) Preprocessing phase: This  optional  (parallel)  phase  takes  a configurable custom
Java class to arbitrarily transform a  Crunch  PCollection  (the output of the previous
phase) into another Crunch PCollection. 

4) Morphline phase: This optional (parallel)  phase  takes the items of the PCollection
generated by the previous phase, and uses  a Morphline to extract the relevant content,
transform it and pipe zero  or  more  Avro  objects  into  another PCollection. The ETL
functionality  is  flexible  and  customizable  using  chains  of  arbitrary  morphline
commands that pipe records  from  one  transformation  command  to another. Commands to
parse and transform a set of standard  data  formats  such as Avro, Parquet, CSV, Text,
HTML, XML, PDF, MS-Office, etc.  are  provided  out  of  the box, and additional custom
commands and parsers for  additional  file  or  data  formats  can  be  added as custom
morphline commands or custom Hadoop  Input  Formats.  Any  kind  of  data format can be
processed and any kind output  format  can  be  generated  by  any custom Morphline ETL
logic. Also, this phase can be used to  send  data directly to a live SolrCloud cluster
(via the loadSolr morphline  command)  and  subsequently  emit  zero  items to the next
phase (via the dropRecord morphline command).

5) Join & Postprocessing phase:  This  optional  (parallel)  phase takes a configurable
custom Java class to arbitrarily  transform  a  Crunch  PCollection  (the output of the
previous phase) into another  Crunch  PCollection.  For  example,  this  phase can join
multiple data sources and afterwards  send  data  directly  to a live SolrCloud cluster
(via the loadSolr command used as part  of  a  second morphline - see class MorphlineFn
which is a Crunch DoFn) and subsequently emit zero items to the next phase.

6) Output phase: This (parallel)  phase  loads  the  output  of the previous phase into
HDFS files or a Kite Dataset.

The program is implemented  as  a  Crunch  pipeline  and  as  such Crunch optimizes the
logical phases mentioned above into an  efficient  physical  execution plan that runs a
single mapper-only job (e.g. if  no  pre  and  postprocessing  phase is specified) or a
complex sequence of many consecutive MapReduce  jobs  (if complex pre or postprocessing
joins, sorting or groupBys are specified), or as the corresponding Spark equivalent.

Fault Tolerance: Task attempts are  retried  on  failure  per the standard MapReduce or
Spark semantics. On program startup  all  data in --output-dir or --output-dataset-name
is  deleted  if  that   output   location   already   exists  and  the  --output-write-
mode=OVERWRITE parameter is specified. If the whole  job  fails you can retry simply by
rerunning the program again using the same arguments.

Input dataset arguments (also see http://ow.ly/uSn4q):
  --input-dataset-uri DATASET_URI
                         Kite Dataset URI of  the  dataset  to  read  from. Multiple --
                         input-dataset-uri arguments can be specified. Examples: 
                         --input-dataset-uri dataset:file:/path/to/events
                         --input-dataset-uri dataset:hdfs://localhost:/path/to/events
  --input-dataset-repository REPOSITORY_URI
                         Kite Dataset Repository  URI  to  read  from.  Syntax is repo:
                         [storage component], where  the  [storage component] indicates
                         the underlying metadata and,  in  some cases, physical storage
                         of the data, along with  any options. Examples: file:[path] or
                         hdfs://[host]:[port]/[path]    or     hive://[metastore-host]:
                         [metastore-port]/ or hive://[metastore-host]:[metastore-port]/
                         [path]    or     hbase:[zookeeper-host1]:[zk-port],[zookeeper-
                         host2],... as described in detail here: http://ow.ly/uSczs
                         
                         Multiple   --input-dataset-repository    arguments    can   be
                         specified, with the --input-dataset-*  options described below
                         always  applying  to  the  most  recently  specified  --input-
                         dataset-repository. For example, to  ingest repoA/datasetA1 as
                         well as repoA/datasetA2 as well  as repoB/datasetB specify the
                         following: 
                         --input-dataset-repository repo:hdfs://localhost:/repoA1 
                         --input-dataset-include literal:datasetA1 
                         --input-dataset-include literal:datasetA2 
                         --input-dataset-repository repo:hdfs://localhost:/repoB 
                         --input-dataset-include literal:datasetB
  --input-dataset-include STRING
                         An  expression  to  match  against   the  Kite  Dataset  names
                         contained in  the  most  recently  specified  --input-dataset-
                         repository.  A  dataset  that   matches  any  --input-dataset-
                         include expression yet  does  not  match  any --input-dataset-
                         exclude expression will be ingested.
                         The  expression  can  be  a   literal  string  equality  match
                         (example:  'literal:events'),  or  a   regex  match  (example:
                         'regex:logs-v[234]-california-.*') or a  glob  match (example:
                         'glob:logs-*-california-*'). Default is  to  match all dataset
                         names ('glob:*' aka '*'), i.e. include everything. Multiple --
                         input-dataset-include arguments can be specified.
  --input-dataset-exclude STRING
                         An  expression  to  match  against   the  Kite  Dataset  names
                         contained in  the  most  recently  specified  --input-dataset-
                         repository.  A  dataset  that   matches  any  --input-dataset-
                         include expression yet  does  not  match  any --input-dataset-
                         exclude expression will be ingested.
                         The  expression  can  be  a   literal  string  equality  match
                         (example:  'literal:events'),  or  a   regex  match  (example:
                         'regex:logs-v[234]-california.*') or  a  glob  match (example:
                         'glob:logs-*-california-*'). Default is  to  match  no dataset
                         names, i.e. exclude  nothing. Multiple --input-dataset-exclude
                         arguments can be specified.

Input file arguments:
  HDFS_URI               HDFS URI of file or directory  tree to ingest (unless --input-
                         dataset-repository  or   --input-dataset-uri   is  specified).
                         (default: [])
  --input-file-list URI  Local URI or HDFS URI  of  a  UTF-8  encoded file containing a
                         list of HDFS URIs to ingest, one  URI per line in the file. If
                         '-' is specified,  URIs  are  read  from  the  standard input.
                         Multiple --input-file-list arguments can be specified.
  --input-file-format FQCN
                         The Hadoop FileInputFormat  to  use  for  extracting data from
                         splitable HDFS files.  Can  be  a  fully  qualified Java class
                         name or  one  of  ['text',  'avro',  'avroParquet'].  If  this
                         option is present the extraction  phase  will emit a series of
                         input data records rather  than  a  series  of HDFS file paths
                         (unless --input-dataset-repository  or  --input-dataset-uri is
                         given, in which  case  the  extraction  phase  will  emit Avro
                         records from the (splitable) Kite dataset).
  --input-file-projection-schema FILE
                         Relative or absolute path to an  Avro schema file on the local
                         file system. This will be  used  as  the projection schema for
                         Parquet input  files  (but  not  yet  for  Kite  Parquet input
                         datasets).

Input dataset and file arguments:
  --input-file-reader-schema FILE
                         Relative or absolute path to an  Avro schema file on the local
                         file system. This will be used  as  the reader schema for Avro
                         or Parquet  input  files  or  Kite  input  datasets.  Example:
                         src/test/resources/strings.avsc

Output dataset arguments:
  --output-dataset-repository REPOSITORY_URI
                         Kite Dataset  Repository  URI  to  write  output  to.  For the
                         syntax see the  documentation  above  for the --input-dataset-
                         repository option.
  --output-dataset-name STRING
                         The name of the Kite Dataset to  write output to inside of the
                         --output-dataset-repository. Example: 'events'.
  --output-dataset-schema FILE
                         Relative or absolute path to an  Avro schema file on the local
                         file system. This will be used  on output as the writer schema
                         for Kite datasets. Example: src/test/resources/strings.avsc
  --output-dataset-format STRING
                         The type of format to use on  output for Kite datasets. Can be
                         one of ['avro', 'parquet']. (default: avro)
  --output-dataset-partition-strategy FILE
                         Relative or absolute path to  a  JSON  file  on the local file
                         system  that  defines  how   the   output   dataset  shall  be
                         partitioned   (also    see    http://ow.ly/yxvFz).    Example:
                         src/test/resources/partitioning.json
  --output-dataset-column-mapping FILE
                         Relative or absolute path to  a  JSON  file  on the local file
                         system that defines  how  a  Kite  entity  maps  to a columnar
                         store. Example: src/test/resources/columnMapping.json

Output file arguments:

Output dataset and file arguments:
  --output-write-mode STRING
                         Indicates  different  options  the   client  may  specify  for
                         handling  the  case  where  the  output  path,  dataset,  etc.
                         referenced  by  a  target  already   exists.  Can  be  one  of
                         ['DEFAULT', 'OVERWRITE', 'APPEND',  'CHECKPOINT']. For details
                         see                    http://crunch.apache.org/apidocs/0.10.0
                         /org/apache/crunch/Target.WriteMode.html (default: DEFAULT)

Morphline phase (also see http://ow.ly/uSnsv):
  --morphline-file FILE  Relative  or  absolute  path  to  a  local  config  file  that
                         contains one  or  more  morphlines.  The  file  must  be UTF-8
                         encoded. It  will  be  uploaded  to  each  MR  task.  Example:
                         /path/to/morphline.conf
  --morphline-id STRING  The identifier of the morphline  that shall be executed within
                         the morphline config  file  specified  by --morphline-file. If
                         the --morphline-id option  is  ommitted  the  first (i.e. top-
                         most) morphline  within  the  config  file  is  used. Example:
                         morphline1

Pre and Postprocessing phase (also see http://crunch.apache.org/user-guide.html):

Misc arguments:
  --help, -help, -h      Show this help message and exit
  --mappers INTEGER      Tuning knob that indicates  the  maximum  number  of MR mapper
                         tasks to use. -1 indicates use  all map slots available on the
                         cluster. This parameter  only  applies  to non-splitable input
                         files (default: -1)
  --dry-run              Run the  pipeline  but  do  not  write  output  to  the target
                         specified  by  --output-dataset-repository   or  --output-dir.
                         This can be used for  quicker  turnaround during early trial &
                         debug sessions. (default: false)
  --log4j FILE           Relative or absolute path  to  a  log4j.properties config file
                         on the local file system. This  file  will be uploaded to each
                         MR task. Example: /path/to/log4j.properties
  --verbose, -v          Turn on verbose output. (default: false)
  
Generic options supported are
  --conf &lt;configuration file&gt;
                         specify an application configuration file
  -D &lt;property=value&gt;    use value for given property
  --fs &lt;local|namenode:port&gt;
                         specify a namenode
  --jt &lt;local|jobtracker:port&gt;
                         specify a job tracker
  --files &lt;comma separated list of files&gt;
                         specify comma separated files to be copied to the map reduce cluster
  --libjars &lt;comma separated list of jars&gt;
                         specify comma separated jar files to include in the classpath.
  --archives &lt;comma separated list of archives&gt;
                         specify comma separated  archives  to  be  unarchived  on  the compute
                         machines.

The general command line syntax is
bin/hadoop command [genericOptions] [commandOptions]

Examples: 

# Ingest text file line by line into a Kite Avro Dataset
# and on the way transform each line with a morphline:
hadoop fs -copyFromLocal src/test/resources/test-documents/hello1.txt hdfs:/user/systest/input/
hadoop \
  --config /etc/hadoop/conf.cloudera.YARN-1 \
  jar target/kite-morphlines-crunch-tool-*-job.jar org.kitesdk.morphline.crunch.tool.MorphlineCrunchTool \
  -D 'mapred.child.java.opts=-Xmx500m' \
  --files src/test/resources/string.avsc \
  --morphline-file src/test/resources/test-morphlines/readLineWithOpenFileCWD.conf \
  --output-dataset-repository repo:hdfs:/user/systest/data1 \
  --output-dataset-name events \
  --output-dataset-schema src/test/resources/string.avsc \
  --output-dataset-format avro \
  --output-write-mode OVERWRITE \
  --log4j src/test/resources/log4j.properties \
  --verbose \
  hdfs:/user/systest/input/hello1.txt

# Replace readLineWithOpenFileCWD.conf with
# readSplittableLines.conf in the command above,
# then add this to say input files are splitable, rerun:
# --input-file-format=text

# View the output of the job:
hadoop fs -copyToLocal '/user/systest/data1/events/*.avro' /tmp/
wget http://archive.apache.org/dist/avro/avro-1.7.6/java/avro-tools-1.7.6.jar
java -jar avro-tools-1.7.6.jar getschema /tmp/*.avro
java -jar avro-tools-1.7.6.jar tojson /tmp/*.avro

# The above command will display this:
# {"text":"hello world"}
# {"text":"hello foo"}

# Ingest from a Kite Dataset into a Kite Dataset
# and on the way transform data with a morphline:
hadoop \
  --config /etc/hadoop/conf.cloudera.YARN-1 \
  jar target/kite-morphlines-crunch-tool-*-job.jar org.kitesdk.morphline.crunch.tool.MorphlineCrunchTool \
  -D 'mapred.child.java.opts=-Xmx500m' \
  --files src/test/resources/string.avsc \
  --input-dataset-repository repo:hdfs:/user/systest/data1 \
  --morphline-file src/test/resources/test-morphlines/extractAvroPathCWD.conf \
  --output-dataset-repository repo:hdfs:/user/systest/data2 \
  --output-dataset-name events \
  --output-dataset-schema src/test/resources/string.avsc \
  --output-dataset-format avro \
  --output-write-mode OVERWRITE \
  --log4j src/test/resources/log4j.properties \
  --verbose
</pre>
  