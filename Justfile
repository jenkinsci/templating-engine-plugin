# when false, disables code coverage
coverage := "true"
# the output directory for the documentation
docsDir  := "site"
# variables for running containerized jenkins
container := "jenkins" # the name of the container
port      := "8080"    # the port to forward Jenkins to

# describes available recipes
help: 
  just --list --unsorted

# wipe local caches
clean: 
  ./gradlew clean 
  rm -rf {{docsDir}}

# Run unit tests
test class="*":
  #!/usr/bin/env bash
  set -euxo pipefail
  coverage=$([[ {{coverage}} == "true" ]] && echo "jacocoTestReport" || echo "")
  ./gradlew test --tests '{{class}}' $coverage

# Run spotless & codenarc
lint-code: 
  ./gradlew spotlessApply codenarc

# Build the JPI
jpi: 
  ./gradlew clean jpi 

############################
# Documentation Recipes
############################

# builds the jte docs builder image
docsImage := "jte-docs-builder"
buildDocsImage:
  docker build docs -t {{docsImage}}

# Build the jte documentation
docs: buildDocsImage
  docker run --rm -v $(pwd):/docs {{docsImage}} build

# serve the docs locally for development
serve: buildDocsImage
  docker run --rm -p 8000:8000 -v $(pwd):/docs {{docsImage}} serve -a 0.0.0.0:8000

lint-docs: lint-markdown lint-prose 

# use Vale to lint the prose of the documentation
lint-prose docsToLint="docs":
  docker run -v $(pwd):/app -w /app jdkato/vale:v2.18.0 {{docsToLint}}

# use markdownlint to lint the docs
lint-markdown: 
  docker run -v $(pwd):/app -w /app davidanson/markdownlint-cli2:0.4.0

# update current docs
update-docs: buildDocsImage
    #!/usr/bin/env bash
    version=$(./gradlew -q printJTEVersion)
    docker run --rm -v ~/.git-credentials:/root/.git-credentials -v $(pwd):/app -w /app --entrypoint=mike jte-docs-builder deploy -f --ignore --push --update-aliases $version latest
    echo "INFO     -  Published version '$version' to GitHub Pages"

###################
# Aggregate Tasks
###################

# Lint code and docs
lint: lint-code lint-docs

# executes the CI checks (test lint jpi)
ci: test lint jpi

# publishes the jpi
release version branch=`git branch --show-current`: 
  #!/usr/bin/env bash
  # validate currently on main
  if [[ ! "{{branch}}" == "main" ]]; then 
    echo "You can only cut a release from the 'main' branch."
    echo "Currently on branch '{{branch}}'"
    exit 1
  fi

  # update build.gradle on main
  sed -i '' "s/^version.*/version = '{{version}}'/g" build.gradle
  # update github issue template with new version in dropdown
  sed -i '' "s/options: \[/options: \[ {{version}},/g" .github/ISSUE_TEMPLATE/bug-report.yaml
  git add build.gradle .github/ISSUE_TEMPLATE/bug-report.yaml
  git commit -m "bump version to {{version}}"
  git push

  # cut a release branch
  git checkout -B release/{{version}}
  git push --set-upstream origin release/{{version}}

  # push a tag for this release
  git tag {{version}}
  git push origin refs/tags/{{version}}

  # publish the docs
  # docker run --rm -v ~/.git-credentials:/root/.git-credentials -v $(pwd):/app -w /app --entrypoint=mike jte-docs-builder deploy --push --update-aliases {{version}} latest

  # publish the JPI
  ./gradlew publish

# when done with a branch in your fork, run `just finish` to delete the local/remote branch and resync with upstream
finish:
  #!/bin/bash

  # make sure we arent deleting main or a release branch
  current_branch=$(git rev-parse --abbrev-ref HEAD)
  if [[ "${current_branch}" =~ (main|release/.*) ]]; then 
    echo "you probably shouldn't delete ${current_branch}"
    echo "this recipe is for feature branches on your fork"
    exit 1
  fi 

  # make sure there arent local changes
  if ! git diff-index --quiet HEAD --; then
    echo "There are local changes: "
    git status 
    exit 1
  fi 

  # confirm theres a remote called origin
  remotes=($(git remote))
  if [[ ! " ${remotes[@]} " =~ " origin " ]]; then
    echo "missing git remote origin"
    exit 1
  fi

  # confirm theres a remote called upstream 
  # and that its pointing at the right repo
  if [[ " ${remotes[@]} " =~ " upstream " ]]; then
    upstream_url=$(git remote get-url upstream)
    if [[ ! "${upstream_url}" =~ "jenkinsci/templating-engine-plugin" ]]; then
      echo "Git upstream url doesn't seem right: ${upstream_url}"
      exit 1
    fi
  else 
    echo "Missing git remote upstream"
    exit 1
  fi

  # re-sync main & delete the previous branch
  git fetch --all
  git checkout main
  git merge upstream/main
  git push origin main
  git branch -d $current_branch
  git push origin --delete $current_branch
 
# run a containerized jenkins instace
run flags='': 
  docker pull jenkins/jenkins:lts
  docker run -d \
  --publish {{port}}:8080 \
  --name {{container}} \
  {{flags}} \
  jenkins/jenkins:lts

# swap the JPI in a running container and restart
reload:
  if [ ! "$(docker ps -qaf name={{container}})" ]; then echo "container '{{container}}' not found'" && exit 1; fi
  if [ ! "$(docker ps -qaf name={{container}} -f status=running)" ]; then docker start {{container}}; fi
  just jpi
  docker exec -it {{container}} /bin/bash -c "rm -rf /var/jenkins_home/plugins/templating-engine{,.*}"
  docker cp build/libs/templating-engine.hpi {{container}}:/var/jenkins_home/plugins/templating-engine.hpi
  docker restart {{container}}

# watches the given path to commit all changes as they occur
watch path:
  watchexec 'cd {{path}} && git add -A && git commit -m "update"' -w {{path}}
