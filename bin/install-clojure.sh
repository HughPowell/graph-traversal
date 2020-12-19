#!/bin/bash
set -o errexit

readonly version=linux-install-1.10.1.763

which rlwrap >/dev/null || sudo apt-get -y install rlwrap
if ! which clojure >/dev/null ; then
  echo "Installing Clojure"
  curl -O "https://download.clojure.org/install/${version}.sh"
  chmod +x "${version}.sh"
  sudo "./${version}.sh"
  rm "${version}.sh"
  echo "Done!"
else
  echo "Clojure already installed!"
fi
