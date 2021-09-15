# ExCC
Exact partitioning method for the *Correlation Clustering (CC)* problem

* Copyright 2020-21 Nejat Arınık

*ExCC* is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation. For source availability and license information see the file `LICENCE`

* Lab site: http://lia.univ-avignon.fr/
* GitHub repo: https://github.com/CompNet/ExCC
* Contact: Nejat Arınık <arinik9@gmail.com>, Vincent Labatut <vincent.labatut@univ-avignon.fr>



## Description

*ExCC* aims at solving optimally the Correlation Clustering problem. It offers two different tasks: Obtaining a *single* vs. *all* optimal solution(s). When all optimal partitions of a given signed graph are required, we call this method *OneTreeCC*. Each mentioned task above can be performed in two different ways: *Branch&Bound (B&B)* and *Branch&Cut (B&C)*.

 In both methods, we first create the ILP model based on the vertex pair formulation, where there are *(n*(n-1)/2)* variables. In *B&B*, we just let Cplex solve it with B&B. In B&C, we first perform a root relaxation and find violated valid inequalities through a *Cutting Plane (CP)* method. After have found sufficient valid inequalities, we insert them into the model and then let Cplex solve it, as in *B&B*. In general, *B&C* is much preferable than *B&B*, since it drastically reduces the execution time.

This program also allows to solve the CC problem by importing a LP file, where a ILP model is already recorded in a previous run. The advantage of doing it is that if we provide *ExCC* with a ILP model containing violated valid inequalities found during the *CP* method, then it amounts to skip the *CP* phase of the *B&C* method. So, it directly proceeds to the second phase: branching. This allows to gain a considerable amount of time.



### Description


 * **inFile:** Input file path. See *in/net.G* for the input graph format. 

 * **outDir:** Output directory path. Default "." (i.e. the current directory).

 * **cp:** True if B&C (i.e. Cutting Plane method + branching) will be used. Default false.

 * **enumAll:** True if enumerating all optimal solutions is desired. Default false. Note that we call *OneTreeCC* this enumeration method.

 * **tilim:** Time limit in seconds for the whole program.Default *-1*, which means no time limit.

 * **tilimForEnumAll** Time limit in seconds for the second phase of the OneTreeCC method. This is useful when doing a benchmarking with EnumCC. Default *-1*.

 * **solLim**  Maximum number of optimal solutions to be discovered when OneTreeCC is called. This can be useful when there are a huge number of optimal solutions, e.g. 50,000. Default *-1*.

 * **MaxTimeForRelaxationImprovement:** Max time limit for relaxation improvement in the first phase of the Cutting Plane method. This is independent of the time limit. If there is no considerable improvement for X seconds, it stops and passes to the 2nd phase, which is branching. This parameter can be a considerable impact on the resolution time. For medium-sized instances (e.g. 50,60), it might be beneficial to increase the value of this parameter (e.g. 1800 or 3600s). The default value is 600s.	Moreover, it might be beneficial to decrease the default value to 30s or 60s if the graph is easy to solve or the number of vertices is below 28.

 * **lazyInBB:** Used only for B&C method. True if adding lazily triangle constraints (i.e. lazy callback approach) in the branching phase. If it is False, the whole set of triangle constraints is added before branching. Based on our experiments, we can say that the lazy callback approach is not preferable over the default approach. Default false.

 * **userCutInBB:** Used only for B&C method. True if adding user cuts during the branching phase of the B&C method or in B&B method is desired. Based on our experiments, we can say that it does not yield any advantage, and it might even slow down the optimization process. Default false.

 * **verbose:** Default value is True. When True, it enables to display log outputs during the Cutting Plane method.

 * **initMembershipFilePath** Default value is "". It allows to import an already known solution into the optimization process. Since we solve a minimization problem, the imbalance value of the imported solution is served as the upper bound. It is usually beneficial to use this option, when we possess some good-quality heuristics.

 * **LPFilePath** Default value is "". It allows to import a LP file, corresponding to a ILP formulation. Remark: such a file can be obtained through Cplex by doing 'exportModel'.

 * **onlyFractionalSolution** Default value is False. It allows to run only the cutting plane method in B&C, so the program does not proceed to the branching phase

 * **fractionalSolutionGapPropValue** It allows to limit the gap value to some proportion value during the cutting plane method in B&C. It can be useful when we solve an easy graph. Hence, we do not spent much time by obtaining very tiny improvement when the solution is already close to optimality. Default *-1*.
   



### Instructions & Use

Install [`IBM CPlex`](https://www.ibm.com/docs/en/icos/20.1.0?topic=2010-installing-cplex-optimization-studio). The default installation location is: `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>`. Tested with Cplex 12.8 and 20.1.

 Put `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>/cplex/lib/cplex.jar` into the `lib` folder in this repository.

Compile and get the jar file for *ExCC*: `ant -v -buildfile build.xml compile jar`.

Run one of the scripts *.sh* available in this repository.



### Examples

See `run-bb.sh`, `run-bb-enum-all.sh`, `run-cp-bb.sh`, `run-cp-bb-enum-all.sh`, `run-cp-only.sh`, `run-lp.sh` and `run-lp-enum-all.sh` for more execution scenarios.

#### Example for B&B

ant clean compile jar
``` ant -DinFile=in/net.G -DoutDir=out/net -Dcp=false -DenumAll=false run java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/  -DinFile=data/net.G -DoutDir=out/net -Dcp=false -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false -DenumAll=false -DinitMembershipFilePath="" -DnbThread=2 -Dverbose=true -DLPFilePath="" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DMaxTimeForRelaxationImprovement=20 -DtilimForEnumAll=-1 -DsolLim=1 -jar exe/ExCC.jar```



#### Example for B&C	  	

```
ant -DinFile=in/net.G -DoutDir=out/net -Dcp=true -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false -DenumAll=false -Dverbose=true run java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/ 
-DinFile=data/net.G -DoutDir=out/net -Dcp=true -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false -DenumAll=false -DinitMembershipFilePath="" -DnbThread=2 -Dverbose=true -DLPFilePath="" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 
-DMaxTimeForRelaxationImprovement=20 -DtilimForEnumAll=-1 -DsolLim=1 -jar exe/ExCC.jar```
```



#### Example for enumerating all optimal solutions through B&C:

```ant -DinFile=in/net.G -DoutDir=out/net -Dcp=true -Dtilim=3600  -DlazyInBB=false -DuserCutInBB=false -DenumAll=true -Dverbose=true run java -Djava.library.path=/opt/ibm/ILOG/CPLEX_Studio201/cplex/bin/x86-64_linux/ -DinFile=data/net.G -DoutDir=out/net -Dcp=true -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false -DenumAll=true -DinitMembershipFilePath="" -DnbThread=2 -Dverbose=true -DLPFilePath="" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DMaxTimeForRelaxationImprovement=-1 -DtilimForEnumAll=-1 -DsolLim=-1 -jar exe/ExCC.jar```




### Output

The names of the optimal solutions are as '*solXX.txt*', where *XX* is the solution id. Moreover, the file *strengthedModelAfterRootRelaxation.lp* is a Cplex LP file, corresponding to a ILP formulation of a signed graph for the CC problem.

