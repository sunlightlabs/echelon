# ECHELON

<img align="right" src="http://upload.wikimedia.org/wikipedia/commons/2/2f/Menwith-hill-radome.jpg" width=200 border=10/>

ECHELON ingests and reconciles everything it can find relating to the
government and assigns unique ID's to the beings it finds. In it's
current form, this means that ECHELON ingests and analyzes the forms
that lobbyists file to disclose their activities each year. It
attempts to perform a reasonable deterministic entity resolution
across all the beings it knows of. Sadly, there are reliable no
primary keys identifying the various lobbying firms, clients and
lobbyists and so we must do our best to guess which beings are the
same and assign our own arbitrary keys to whatever we come up with.

## Usage

0. Install the appropriate JVM for your hardware.
1. Download [Datomic Free Edition](https://my.datomic.com/downloads/free) and install locally.
2. Install [leiningen](http://leiningen.org/).
3. Download
   [the data from here](http://datacommons.s3.amazonaws.com/sopr_html_transformed.tar.gz). This
   is a compressed directory, which is
   [cleaned version of scraped data](https://github.com/influence-usa/lobbying_federal_domestic)
   from the
   [Senate disclosure website](http://www.senate.gov/pagelayout/legislative/g_three_sections_with_teasers/lobbyingdisc.htm#lobbyingdisc=lad)). Unzip
   this and put it someplace safe (it should be 2.6G).
4. Start up datomic. You will need to provide datomic with the path to
   the `transactor.properties` file inside `echelon/resources`. Here
   is an example of how to start up datomic:

``` sh
$ cd ~/software/datomic/
$ bin/transactor -Xmx4g ~/software/echelon/resources/transactor.properties
Launching with Java options -server -Xms1g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=50
Starting datomic:free://localhost:4334/<DB-NAME>, storing data in: data ...
System started datomic:free://localhost:4334/<DB-NAME>, storing data in: data
```

5. Once datomic has started, it's time to load in the data. An
environment variable, `DATA_LOCATION` is used to provide the location
of the download and unzipped data. This step will load, process and
save all the resulting datoms to disk.

``` sh
$ cd ~/software/echelon
$ DATA_LOCATION=~/data/sopr_html lein run load
```

6. Now we get to various the resolution steps. Currently the only effective means we have of evaluating the resolution steps is to look at the names of the matched entities. Thus, the following command will create the file `output/names-output.clj`. The resolution step currently do not save to disk and so the resolutions only exist in memory and are lost as soon as the process ends.

``` sh
$ lein run match
$ head output/names-output.c
```


## Challenges

1. Documentation around the entity resolution framework.
2. Documentation of the choices made concerning document representation.
3. Writing tests for the name parser (see `src/echelon/parser.bnf`).
4. Discovering better metrics for how well the resolution steps have done.
5. Optimizing the load speed for documents.
6. Writing better resolutions steps and expanding how well the parser
   can recognize various entity names.


## Contact

Please use github issues to contact the developers about this project.

## License

Copyright © 2014 Sunlight Foundation
