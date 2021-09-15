
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    mkdir -p "out/""$modifiedName"


        LPFilePath=""
        initMembershipFilePath="in/bestSolutionILS_""$modifiedName"".txt"

        #ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=true -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=2 -Dverbose=true -Dtilim=60 -DtilimForEnumAll=-1 -DsolLim=100 run
        ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=true -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=4 -Dverbose=true -Dtilim=-1 -DtilimForEnumAll=60 -DsolLim=100 run

    fi
done
