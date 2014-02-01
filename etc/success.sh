#!/bin/bash

set -e

BASEDIR=etc/
descriptor="jdk_${TRAVIS_JDK_VERSION}"

if [[ "${TRAVIS_PULL_REQUEST}" != false ]]; then
	descriptor="${descriptor}_pr"
else
	descriptor="${descriptor}_branch_${TRAVIS_BRANCH}"
fi

echo "Descriptor: ${descriptor}"

if [[ -x ${BASEDIR}/${descriptor} ]]; then
	exec ${BASEDIR}/${descriptor}
else
	echo "No script"
fi

