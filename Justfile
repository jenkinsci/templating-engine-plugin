# when false, disables code coverage
coverage := "true"
# the output directory for the documentation
docsDir  := "docs/html"
# the Antora playbook file to use when building docs
playbook := "docs/antora-playbook-local.yml"
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
lint: 
  ./gradlew spotlessApply codenarc

# Build the JPI
jpi: 
  ./gradlew clean jpi 

# executes the CI checks (test lint jpi)
ci: test lint jpi

# Build the local Antora documentation
docs: 
  docker run \
  -it --rm \
  -v ~/.git-credentials:/home/antora/.git-credentials \
  -v $(pwd):/app -w /app \
  docker.pkg.github.com/boozallen/sdp-docs/builder \
  generate --generator booz-allen-site-generator \
  --to-dir {{docsDir}} \
  {{playbook}}

# publishes the jpi
release version branch=`git branch --show-current`: 
  #!/usr/bin/env bash
  if [[ ! "{{branch}}" == "main" ]]; then 
    echo "You can only cut a release from the 'main' branch."
    echo "Currently on branch '{{branch}}'"
    exit 1
  fi
  # cut a release branch
  git checkout -B release/{{version}}
  # bump the version in relevant places
  sed -ie "s/^version.*/version = '{{version}}'/g" build.gradle
  sed -ie "s/^version:.*/version: '{{version}}'/g" docs/antora.yml
  git add build.gradle docs/antora.yml
  git commit -m "bump version to {{version}}"
  git push --set-upstream origin release/{{version}}
  # push a tag for this release
  git tag {{version}}
  git push origin refs/tags/{{version}}
  # publish the JPI
  ./gradlew publish

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
