
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    mkdir -p "out/""$modifiedName"



        LPFilePath="" #"in/strengthedModelAfterRootRelaxation.lp"
        initMembershipFilePath="in/bestSolutionILS_""$modifiedName"".txt"

	    ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=false -Dcp=true -DMaxTimeForRelaxationImprovement=60 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=0.01 -DnbThread=3 -Dverbose=true -Dtilim=300 -DtilimForEnumAll=-1 -DsolLim=10 run
	    

    fi
done
