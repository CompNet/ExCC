
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    if [ "$modifiedName" != "fractionalGraph" ]
    then

	    echo "in/""$modifiedName"
	    mkdir -p "out/""$modifiedName"

        ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=false -Dcp=false -DnbThread=2 -Dverbose=true -Dtilim=30 run
        
        # LPFilePath=""
        # initMembershipFilePath="" #"in/bestSolutionILS_""$modifiedName"".txt"
        # ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=false -Dcp=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DnbThread=2 -Dverbose=true -Dtilim=30 run

    fi
done
