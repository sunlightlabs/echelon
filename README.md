# ECHELON

<img align="right" src="http://upload.wikimedia.org/wikipedia/commons/2/2f/Menwith-hill-radome.jpg" width=200 border=10/>

ECHELON is a project designed to make up for the U.S. government's
shortcomings in regards to disclosures and open data. Due to the poor
quality of the data released, simple question about the workings of
our government are difficult to answer and useful questions often
can't be answered at all. ECHELON uses a variety of computational
techniques and clever database design to overcome the hurdles of
trying to model the government inside a computer based on the limited
information publicly available .

Overarching generalities aside, ECHELON's main purpose is three
fold. Firstly, ECHELON takes data from the government and structures
it in useful ways. This is accomplished by creating models derived
from domain expertise in the given data and then inserting the data
into [Datomic](http://www.datomic.com/), a
[curious database](http://www.infoq.com/presentations/datomic-functional-database)
with a
[powerful query engine](http://docs.datomic.com/query.html). Once the
data has been loaded, ECHELON goes beyond just providing a better
interface by enhancing the data via the usage of
[record linkage](http://en.wikipedia.org/wiki/Record_linkage) and
[information extraction](http://en.wikipedia.org/wiki/Information_extraction)
techniques. In particular, this means that we can figure out that
something named "Big Company Incorporated (formerly known as Small
Company co.)"  represents the same being as "SMALL COMPANY
INC.". Lastly, ECHELON aims to provide durable and reliable ID's for
the various beings that are involved in the workings of the
government.

ECHELON is very much a work in progress and currently only ingests
data from the Senate Lobbying Disclosure website and performs only
simple calculations for resolving the various beings in the data
set. We have many other data sources we are interested in pulling in
and are working towards continually ingesting them as soon as
possible. And of course, there are far more complicated record linkage
and information extraction techniques we would like to explore.

## Usage

To begin with, we are currently running ECHELON on an EC2 instance
with 32 cores and 60 gigabytes of ram. It can take upwards of a hour
or two to load the data into datomic and the simplest resolution steps
take at least ten minutes to run. We're working on a more complicated
step that extracts simplified names from lobbying forms based on
instaparse, which, while in theory should work fine, is throwing out
of memory errors despite a heap size of 40 gigabytes. Again, this is a
large work in progress.

0. Install the appropriate JVM for your hardware.
1. Download [Datomic Free Edition](https://my.datomic.com/downloads/free) and unzip it someplace where it can take up at least 20 gigabytes of disk space.
2. Install [leiningen](http://leiningen.org/).
3. Download
   [the data from here](http://datacommons.s3.amazonaws.com/sopr_html_transformed.tar.gz). This
   is a compressed directory, which is
   [cleaned version of scraped data](https://github.com/influence-usa/lobbying_federal_domestic)
   from the
   [Senate disclosure website](http://www.senate.gov/pagelayout/legislative/g_three_sections_with_teasers/lobbyingdisc.htm#lobbyingdisc=lad). Unzip
   this and put it someplace safe. Untared, this file should be
   2.6G. There is no system set up yet to keep the data up to date
   on S3, so if you run into any problems running the load script,
   please file an issue on github and we'll make sure the data is
   fully updated.
4. Start up datomic. You will need to provide datomic with the path to
   the `transactor.properties` file inside `echelon/resources`. Here
   is an example of how to start up datomic:

   ``` sh
   $ cd ~/software/datomic/
   $ bin/transactor -Xmx4g ~/software/echelon/resources/transactor.properties
   Launching with Java options -server -Xms1g -Xmx10g -XX:+UseG1GC -XX:MaxGCPauseMillis=50
   Starting datomic:free://localhost:4334/<DB-NAME>, storing data in: data ...
   System started datomic:free://localhost:4334/<DB-NAME>, storing data in: data
   ```

5. Once datomic has started, it's time to load in the data. An
environment variable, `DATA_LOCATION` is used to provide the location
of the download and unzipped data. This step will load, process and
save all the resulting datoms to disk. The default heap size for the
JVM is 20gb. During the loading, this can probably safely turned down to under 10gb.

   ``` sh
   $ cd ~/software/echelon
   $ DATA_LOCATION=~/data/sopr_html lein with-profile +user,+prod run load
   ```

Finally, there are several methods used to resolve entities. Currently
the most effective means we have of evaluating the resolution steps is
to look at the names of the matched entities. Thus, all of the
following commands will create the file
`output/names-output.clj`. This file will have a list of vectors that
have a structure similar to the following:

``` clj
[17592186297621
("AEROSPACE MISSIONS CORPORATION" "Aerospace Missions Corporation")]
[17592190608335 ("AES Corporation" "AES Corp")]
[17592191194294 ("AES Sparrows Point LNG, LLC")]
[17592195354891 ("AES Wind Generation" "AES Wind Generation, Inc.")]
[17592191304138 ("AESTI")]
[17592195340675 ("AETNA INC." "AETNA" "Aetna Inc." "Aetna, Inc.")]
[17592195436234 ("AETNA INC.  (Formerly, AETNA LIFE & CASUALTY)")]
[17592188160497 ("AETREX WORLDWIDE, INC.")]
[17592189483957
("AFC FIRST FINANCIAL CORPORATION d/b/a GREAT BEAR FINANCIAL")]
[17592188989049
("AFFILIATED COMPUTER SERVICES, INC.-STATE AND LOCAL SOLUTIONS")]
[17592199962992
("AFFILIATED MANAGERS GROUP, Inc."
"Affiliated Managers Group, Inc.")]
[17592197543002
("AFFORD (formerly Climate Policy Group)"
"AFFORD, Formerly Climate Policy Group")]
[17592199047305 ("AFFORD Group (formerly Climate Policy Group)")]
[17592190928110
("AFFORDABLE HOUSING TAX CREDIT COALITION"
"Affordable Housing Tax Credit Coalition")]
```

Each vector represents a being within the database. The first element
of a vector is the entity id from datomic for the being. The second
element of the vector is a list of strings which correspond to the
various names that representations associated with the being
possess. As we can see above, `"AETNA INC."`, `"AETNA"`, `"Aetna INC."` and
`"Aetna,Inc."` are all associated with the same being, while `"AETNA INC.
(Formerly, AETNA LIFE & CASUALTY)"` has it's own being.

There are currently three resolution steps. The first and simplest
relies on a checkbox on the lobbying forms which indicates that the
registrant being is the same as the client being.

``` sh
$ lein with-profile +user,+prod run match same-on-form
```

The next step, in terms of increasing complexity, matches some beings
based on name. In particular, if a representation of a client, foreign
entity, registrant or affiliated organization have the same exact
name, then the beings they represent are merged together. Note that we
do not match people solely based on exact name matches.

``` sh
$ lein with-profile +user,+prod run match exact-name
```

The third and final step used is the main focus right now in terms of
effort. ECHELON uses
[instaparse](https://github.com/Engelberg/instaparse) and
[a formal grammar](https://github.com/sunlightlabs/echelon/blob/master/src/echelon/parser.bnf)
to parse the same names that were previously matched on into data
structures. The data structures results are then used to match
representations together and indicate which beings should be merged.

``` clj
(require '[echeon.text :refer [extract-names]])
(extract-names "Independent School District No. 1 of Tulsa County, Oklahoma a/k/a Tulsa Public School")
;;[["independent" "school" "district" [:number "1"] "of" "tulsa" "county" "oklahoma"]
;; :aka
;; ["tulsa" "public" "school"]]
(extract-names "Terminix, International Co. Lp.")
;;[["terminix" :international :company :lp]]
(extract-names "GenCorp Inc./Aerojet Rocketdyne Inc. (fka Aerojet General Corporation)")
;;[["gencorp" :incorporated "aerojet" "rocketdyne" :incorporated]
;; :fka
;; ["aerojet" "general" :corporation]]
```

While it may seem ridiculous to use a parser to try and parse free
text english, we've found that the lobbyists filling out these forms
are creatures of habit. Much of the same patterns and vocabulary
occurs over and over again and so the text inside these name fields
have more structure than one might expect. While it may not be
possible to get an unambiguous parsing of the fields, it is possible
to get fairly useful information out of the fields without an undue
amount of effort.

``` sh
$ lein with-profile +user,+prod run match extracted-name
```

It's also possible to run all of the above steps sequentially.

``` sh
$ lein with-profile +user,+prod run match extracted-name
```

Finally, the match steps save the produced resolutions to disk. This
means that running a step more than once will not currently produce
better results.

## Challenges

1. Outline the being representations and what they mean.
2. Documentation of the choices made concerning document representation.
3. Discovering better metrics for how well the resolution steps have done.
4. Writing better resolutions steps and expanding how well the parser
   can recognize various entity names.

## Contact

Please use github issues to contact the developers about this project.

## License

Copyright © 2014 Sunlight Foundation
