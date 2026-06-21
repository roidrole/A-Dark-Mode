#!/bin/bash

# Updating Script
# Automatically compress and merge Release Ready with Resource Pack/assets, update the changelog and generate a zip
# Requires Oxipng

read -t 10 -p "Version Number : " version


#Update changelog
echo -e "\n" >> ./"Release Ready"/Changelog.md
cat ./Changelog.md >> ./"Release Ready"/Changelog.md
echo "$version:" > ./Changelog.md
cat ./"Release Ready"/Changelog.md >> ./Changelog.md
rm ./"Release Ready"/Changelog.md

#Update Resource Pack & zips
oxipng -r "./Release Ready/"
mv -f ./"Release Ready"/* ./"Resource Pack"/assets
cd ./"Resource Pack"
zip -q -r "Dark Mode-$version.zip" assets pack.mcmeta pack.png
mv -f ./"Dark Mode-$version.zip" ../"Releases"

#Recreated the changelog file of the next release
cd ../
> ./"Release Ready"/Changelog.md