
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    mkdir -p "out/""$modifiedName"



        LPFilePath="in/strengthedModelAfterRootRelaxation.lp"
        initMembershipFilePath="in/bestSolutionILS_""$modifiedName"".txt"

	    ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=false -Dcp=true -DMaxTimeForRelaxationImprovement=20 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=true -DfractionalSolutionGapPropValue=-1.0 -DnbThread=2 -Dverbose=true -Dtilim=60 run
        
	    #ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=false -Dcp=true -DMaxTimeForRelaxationImprovement=20 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=true -DfractionalSolutionGapPropValue=0.01 -DnbThread=2 -Dverbose=true -Dtilim=60 run
	    
    fi
done
