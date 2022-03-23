#!/bin/sh

build_gradle="aic-application/app/build.gradle"
previours_version=$(grep "versionName " ${build_gradle} | awk -F\" '{print $2}')
new_version=$(echo "$previours_version + 0.01" | bc -l | awk '{printf "%.2f", $0}')
sed -i "s#versionName \"[0-9]*.[0-9]*\"#versionName \"${new_version}\"#g" ${build_gradle}
if [ $? -ne 0 ]; then
	echo "Fail to modify the versionName from $previours_version to $new_version"
	exit 1
else
	echo "Sucess to modify the versionName from $previours_version to $new_version"
fi
