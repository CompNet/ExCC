
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    mkdir -p "out/""$modifiedName"


        LPFilePath=""
        initMembershipFilePath="in/bestSolutionILS_""$modifiedName"".txt"

        #ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=20 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DnbThread=2 -Dverbose=true -Dtilim=40 -DtilimForEnumAll=-1 -DsolLim=100 run
        ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=20 -DuserCutInBB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=0.01 -DnbThread=3 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=120 -DsolLim=100 run
        
    fi
done
