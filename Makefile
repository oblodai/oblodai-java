# Local gates of the Java SDK. Maven runs in docker (no JDK/Maven needed on the host), with its
# cache in the git-ignored .m2cache/; the drift check needs Go and the backend checkout.
#
#   make ci                                   every gate: drift, build + lint + tests + conformance, package
#   OBLODAI_BACKEND=../oblodai-backend make ci
#
# The backend checkout ($OBLODAI_BACKEND, else ../oblodai-backend) provides tools/sdkgen (drift)
# and tools/sdkgen/conformance (the shared scenarios); without it both are skipped, loudly.

BACKEND := $(or $(OBLODAI_BACKEND),$(abspath $(CURDIR)/../oblodai-backend))
HAS_BACKEND := $(wildcard $(BACKEND)/tools/sdkgen/conformance)
MAVEN_IMAGE ?= maven:3-eclipse-temurin-21
MVN := docker run --rm --label oblodai.sdkcheck=1 --memory 3g \
	-v $(CURDIR):/src -v $(CURDIR)/.m2cache:/root/.m2 \
	$(if $(HAS_BACKEND),-v $(BACKEND):/backend:ro -e OBLODAI_BACKEND=/backend,) \
	-w /src $(MAVEN_IMAGE) mvn -B -T 1

.PHONY: ci drift verify package-check test conformance

ci: drift verify package-check  ## every gate
	@echo "all gates green"

drift:  ## generated code == what tools/sdkgen makes of the contract, names.lock holds
	@echo "== generated code drift"
	@OBLODAI_BACKEND=$(BACKEND) ./scripts/check_generated.sh $(if $(OBLODAI_BACKEND),--require,)

verify:  ## compile with -Xlint -Werror, unit + conformance tests, jar + sources + javadoc (doclint)
	@echo "== build, lint, tests, conformance, package"
	@mkdir -p .m2cache
	$(MVN) verify

package-check:  ## the jars carry what a release needs
	@echo "== package contents"
	@./scripts/check_package.sh

test:  ## unit tests only, fast
	@mkdir -p .m2cache
	$(MVN) test -Dmaven.javadoc.skip=true

conformance:  ## only the shared conformance suite
	@mkdir -p .m2cache
	$(MVN) test -Dmaven.javadoc.skip=true -Dtest=ConformanceTest -Dsurefire.failIfNoSpecifiedTests=false
