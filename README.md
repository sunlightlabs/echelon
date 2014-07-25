# ECHELON

<img align="right" src="http://upload.wikimedia.org/wikipedia/commons/2/2f/Menwith-hill-radome.jpg" width=200 border=10/>

ECHELON is a project designed to make up for the government of the
United States' shortcomings in regards to disclosures and open
data. Many an intrepid researcher has tried to ask a simple question
about the workings of government only to find themselves stymied by
the poor quality of data released and activities disclosed. We use a
variety of computational techniques and clever database design to
overcome the hurdles of trying to model the government inside a
computer.

Overarching generalities aside, ECHELON's main purpose is two
fold. Firstly, ECHELON takes data from the government and structures
it in useful ways. This is accomplished by creating models derived
from domain expertise in the given data and then inserting the data
into [Datomic](http://www.datomic.com/), a
[curious database](http://www.infoq.com/presentations/datomic-functional-database)
with a
[powerful query engine](http://docs.datomic.com/query.html). Once the
data has been loaded, ECHELON goes beyond just providing a better
interface by enhancing the data via
[record linkage](http://en.wikipedia.org/wiki/Record_linkage) and
[information extraction](http://en.wikipedia.org/wiki/Information_extraction)
techniques. In particular, this means that we can figure out that
something named "Big Company Incorporated (formerly known as Small
Company co.)"  represents the same being as "SMALL COMPANY INC.". We
then provide functions for working with the representations needed to
model these abstract relationships. To accomplish both of the previous
objectives, ECHELON aims to provide durable and reliable ID's for the
various beings that are involved in the workings of the government.

ECHELON is very much a work in progress and currently only ingests
data from the Senate Lobbying Disclosure website and performs only
simple calculations for resolving the various beings in the data
set. We have many other data sources we are interested in pulling in
and are working towards ingesting them as soon as possible. And of
course, there are far more complicated record linkage and information
extraction techniques we have planned as well. The government is one
of the largest and most complicated systems mankind has ever assembled
and, understandably, there is much work to be done.

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

## Conceptual Framework

> As an organizer I start from where the world is, as it is, not as I
> would like it to be. (Saul D. Alinsky)

Before outlining the conceptual framework that supports ECHELON,
sadly, we must first dash your hopes and implicit assumptions about
the quality of government data against the harsh uncaring rocks of
reality. We will focus on the lobbying disclosure data, but the low
level of quality we will soon see is the rule rather than the
exception.



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
