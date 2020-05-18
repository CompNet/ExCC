
for filename in `ls in | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}
	echo "in/""$modifiedName"
	mkdir -p "out/""$modifiedName"

	ant -DinFile="in/""$name" -DoutDir="out/""$modifiedName" -DenumAll=true -Dcp=true -DMaxTimeForRelaxationImprovement=600 -Dverbose=false run
done
