# Minimal makefile to build Antora documentation
BUILDDIR = docs/html
PLAYBOOK = docs/antora-playbook-local.yml 

# Put it first so that "make" without argument is like "make help".
help: ## Show target options
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##//'

clean: ## removes remote documentation and compiled documentation
	rm -rf $(BUILDDIR)

.PHONY: docs
docs: clean ## builds the antora documentation 
	docker run \
	-t --rm \
	-v ~/.git-credentials:/home/antora/.git-credentials \
	-v $(shell pwd):/app -w /app \
	docker.pkg.github.com/boozallen/sdp-docs/builder \
	generate --generator booz-allen-site-generator \
	--to-dir $(BUILDDIR) \
	$(PLAYBOOK)

jpi: ## builds the jpi via gradle
	./gradlew clean jpi 

test: ## runs the plugin's test suite 
	./gradlew clean test 

# Catch-all target: route all unknown targets to Sphinx using the new
# "make mode" option.  $(O) is meant as a shortcut for $(SPHINXOPTS).
%: Makefile
	echo "Make command $@ not found" 