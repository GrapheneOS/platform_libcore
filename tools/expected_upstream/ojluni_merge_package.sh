#!/bin/bash

SELF=$(basename "${0}")
DEFAULT_TAG="jdk17u/jdk-17.0.5-ga"
SUPPORTED_TAGS="jdk7u/jdk7u40-b60"
SUPPORTED_TAGS="${SUPPORTED_TAGS} jdk8u/jdk8u121-b13"
SUPPORTED_TAGS="${SUPPORTED_TAGS} jdk8u/jdk8u60-b31"
SUPPORTED_TAGS="${SUPPORTED_TAGS} jdk9/jdk-9+181"
SUPPORTED_TAGS="${SUPPORTED_TAGS} jdk11u/jdk-11+28"
SUPPORTED_TAGS="${SUPPORTED_TAGS} jdk11u/jdk-11.0.13-ga"
SUPPORTED_TAGS="${SUPPORTED_TAGS} jdk17u/jdk-17.0.2-ga"
SUPPORTED_TAGS="${SUPPORTED_TAGS} jdk17u/jdk-17.0.5-ga"

USAGE=$(cat << EndOfUsage
Usage:
  ${SELF} [-b <bug_number>] [-t <upstream_tag>] <package_name> <package_name> ...
  For example:
    ${SELF} -b 123456 -t jdk17u/jdk-17.0.5.-ga java.util.concurrent java.util.concurrent.atomic

Possible arguments:
  -h|--help - print help and exit
  -t|--tag - the upstream tag to merge to; default: ${DEFAULT_TAG} or
             OJLUNI_MERGE_TARGET (if defined)
  -b|--bug - the bug number to use in the commit message; if not defined it will
             be picked up from the libcore branch name (for example
             "b-12345-oj-merge" -> "-b 12345")

The supported upstream tags are:
  $(echo ${SUPPORTED_TAGS} | sed 's/ /\n  /g')
EndOfUsage
)

HELP=$(cat << EndOfHelp
Merges one or more packages from an upstream branch.

This will use the correct form of add/modify based on what is already stored in
libcore/EXPECTED_UPSTREAM.  Also it will find new files in the new version of
the upstream package and add those as well.

${USAGE}
EndOfHelp
)

BUG=""
PACKAGES=""
TAG="${OJLUNI_MERGE_TARGET:-"$DEFAULT_TAG"}"

function die()
{
  echo -e ${1}
  if [[ -n "${2}" ]]
  then
    echo -e ""
    echo -e "${USAGE}"
  fi
  exit 1
}

function validate_tag
{
  for expected in ${SUPPORTED_TAGS}
  do
    if [[ "${TAG}" == "${expected}" ]]
    then
      return
    fi
  done
  die "Unknown tag: ${TAG}" "y"
}

function setup_env
{
  if [[ -z "${ANDROID_BUILD_TOP}" ]]
  then
    die "ANDROID_BUILD_TOP not found. You need to run lunch first."
  fi

  shopt -s expand_aliases
  source "${ANDROID_BUILD_TOP}/libcore/tools/expected_upstream/install_tools.sh"
}

while [[ $# -gt 0 ]]; do
  case ${1} in
    -h|--help)
      echo "${HELP}"
      exit 0
      ;;
    -b|--bug)
      BUG="${2}"
      shift
      ;;
    -t|--tag)
      TAG="${2}"
      shift
      ;;
    *)
      PACKAGES="${PACKAGES} ${1}"
      ;;
  esac
  shift
done

if [[ -z "${PACKAGES}" ]]
then
  die "You need to specify at least one package to merge." "y"
fi

setup_env
validate_tag

if [[ -z "${BUG}" ]]
then
  pushd "${ANDROID_BUILD_TOP}/libcore"
  BUG=$(git branch --show-current | grep -E -o "\<b\>[-/][0-9]+-" | grep -E -o "[0-9]+")
  popd
fi

function ojluni-merge-class
{
  local method="${1}"
  local name="${2}"
  local version="${3}"

  local first_arg="${name}"
  local second_arg="${version}"

  if [[ "${method}" == "add" ]]
  then
    first_arg="${version}"
    second_arg="${name}"
  fi
  echo ojluni_modify_expectation "${method}" "${first_arg}" "${second_arg}"
  ojluni_modify_expectation "${method}" "${first_arg}" "${second_arg}" || \
    die "Failed to modify expectation file for ${name}"
}

function ojluni-merge-package
{
  local package="${1}"
  local version="${2}"
  local bug="${3}"
  local package_path=$(echo "${package}" | sed --sandbox 's/\./\//'g)
  local package_full_path="${ANDROID_BUILD_TOP}/libcore/ojluni/src/main/java/${package_path}"

  pushd "${ANDROID_BUILD_TOP}/libcore"

  for f in $(ls "${package_full_path}"/*.java)
  do
    local class_name=$(basename -s .java ${f})
    local class_path="ojluni/src/main/java/${package_path}/${class_name}\.java"
    local in_expected_upstream=$(grep "${class_path}" "${ANDROID_BUILD_TOP}/libcore/EXPECTED_UPSTREAM")
    if [[ -n "${in_expected_upstream}" ]]
    then
      ojluni-merge-class modify "${package}.${class_name}" "${version}"
    else
      ojluni-merge-class add "${package}.${class_name}" "${version}"
    fi
  done

  local version_id=$(echo "${version}" | grep -oE "^[^/]+")
  local branch="aosp/upstream-open${version_id}"
  local new_classes=$(git diff --name-only --diff-filter=D "${branch}" -- \
    "src/java.base/share/classes/${package_path}" \
    "ojluni/src/main/java/${package_path}")

  for f in ${new_classes}
  do
    local class_name=$(basename -s .java ${f})
    local class_path="ojluni/src/main/java/${package_path}/${class_name}\.java"
    ojluni-merge-class add "${package}.${class_name}" "${version}"
  done

  if [[ -n "${bug}" ]]
  then
    echo ojluni_merge_to_master -b "${bug}"
    ojluni_merge_to_master -b "${bug}" || die "Failed to merge ${package} to master"
  else
    echo ojluni_merge_to_master
    ojluni_merge_to_master || die "Failed to merge ${package} to master"
  fi

  popd
}

for package in ${PACKAGES}
do
  echo "Merging '${package}' from ${TAG}"
  ojluni-merge-package "${package}" "${TAG}" "${BUG}"
done
