
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    mkdir -p "out/""$modifiedName"

        LPFilePath="in/strengthedModelAfterRootRelaxation.lp"
        initMembershipFilePath="in/bestSolutionILS_""$modifiedName"".txt"
        
        # ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=true -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=2 -Dverbose=true -Dtilim=120 -DtilimForEnumAll=-1 -DsolLim=100 run
        ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=true -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=3 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=120 -DsolLim=100 run

    fi
done
