
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    mkdir -p "out/""$modifiedName"

        LPFilePath="in/strengthedModelAfterRootRelaxation.lp"
        initMembershipFilePath="in/bestSolutionILS_""$modifiedName"".txt"
        
        ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=false -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=3 -Dverbose=true -Dtilim=60 run

    fi
done
