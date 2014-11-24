Finatra Sample App on OpenShift
=========================

Quickstart finatra application for openshift.

Running on OpenShift
--------------------

Create an account at http://openshift.com/

Create a Do-It-Yourself application

    rhc app create finatra diy-0.1

Add this upstream finatra quickstart repo

    cd finatra
    git remote add upstream -m master https://github.com/MuravevIV/finatra-openshift-quickstart.git
    git pull -s recursive -X theirs upstream master

Clean up your project and edit attributes in pom.xml (groupId, artifactId, name, build.finalName, etc.)

    rm -rf diy/ misc/ src/main/webapp README.md LICENSE
    # edit pom.xml
    git commit -am "cleaned"

Then push the repo upstream

    git push origin master

That's it, you can now checkout your application at:

    http://finatra-$yournamespace.rhcloud.com