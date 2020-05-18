# ExCC
The ExCC program aims at solving Correlation Clustering problem. It proposes 2 approaches to solve the problem: 1) Pure CPLEX approach and Cutting Planes approach, and 2) The Cutting Planes approach consists of 2 steps: i) Root Relaxation, ii) Branch&Bound



### Input parameters


 * **inFile:** Input file path. See *in/exemple.G* the input graph format. 

 * **outDir:** Output directory path.

 * **cp:** True if Cutting Plane approach will be used. So, to use Pure CPLEX approach, set 'cp' to false. 

 * **enumAll:** True if enumerating all optimal solutions is desired.

 * **tilim:** Time limit in seconds. It can be used only with Cutting Plane approach. To deactivate it, use *-1*.

 * **MaxTimeForRelaxationImprovement:** Max time limit for relaxation improvement in the first phase of the Cutting Plane approach. This is independent of the time limit. If there is no considerable improvement for X seconds, it stops and passes to the 2nd phase. This parameter can be a considerable impact on the resolution time. For medium-sized instances (e.g. 50,60),  it might be beneficial to increase the value of this parameter (e.g. 1800 or 3600s). The default value is 600.

 * **lazyInBB:** True if adding triangle constraints as lazy approach during the Branch&Bound is desired. It is used in the Cutting Plane approach. This will set automatically the number of threads to 1.

 * **userCutInBB:** True if adding user cuts during the Branch&Bound is desired. It is used in the Cutting Plane approach. This will set automatically the number of threads to 1.

 * **verbose:** Default value is True. When True, it enables to display log outputs during the Cutting Plane approach.

   

### Instructions

Install [`IBM CPlex`](https://www.ibm.com/developerworks/community/blogs/jfp/entry/CPLEX_Is_Free_For_Students?lang=en). The default installation location for education version is: `/opt/ibm/ILOG/CPLEX_Studio128`.

 (optional) Put `/opt/ibm/ILOG/CPLEX_Studio128/cplex/lib/cplex.jar` into the `lib` folder in this repository.



There are 2 options to run the algorithm in command line


 * Without *Ant*:

    * `java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio128/cplex/bin/x86-64_linux/ -DinFile="in/GRAPH.G" -DoutDir=out -Dcp=false -Dtilim=-1 -DlazyInBB=false -DMaxTimeForRelaxationImprovement=60 -DuserCutInBB=false -DenumAll=false -Dverbose=false -jar exe/cplex-partition.jar`
 * With *Ant*:

    * Do not forget to set correctly the variable *java.library.path* in *build.xml*
    * `ant clean compile jar;`
    * `ant -DinFile=data/signed.G -DoutDir=out/signed -Dcp=false -DenumAll=false run`



See `run-cp.sh`, `run-cp-enum-all.sh`, `run-enum-all.sh` and `run-pure-cplex.sh` for more execution scenarios.

### Output

If  *enumAll=false*, then the output file is named as '*ExCC-result.txt*'. Otherwise, the output files are named as '*solXX.txt*', where *XX* is the solution id.