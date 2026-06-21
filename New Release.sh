#!/bin/bash

# Updating Script
# Automatically compress and merge Release Ready with Resource Pack/assets, update the changelog and generate a zip
# Requires Oxipng

read -t 10 -p "Version Number : " version


#Update changelog
echo -e "\n" >> ./"Release Ready"/Changelog.txt
cat ./Changelog.txt >> ./"Release Ready"/Changelog.txt
echo "$version:" > ./Changelog.txt
cat ./"Release Ready"/Changelog.txt >> ./Changelog.txt
touch ./"Release Ready"/Changelog.txt

#Update Resource Pack & zips
oxipng -r "./Release Ready/"
mv -f ./"Release Ready"/* ./"Resource Pack"/assets
cd ./"Resource Pack"
zip -r "Dark Mode-$version.zip" assets pack.mcmeta pack.png
mv -f ./"Dark Mode-$version.zip" ../"Releases"