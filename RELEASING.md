# Releasing to Maven Central

Everything Central needs lives in the `release` profile of `pom.xml`. An ordinary build does not
touch it: `mvn verify` on a machine with no Sonatype account and no GPG key produces the jar, the
sources jar and the javadoc jar, and never asks for a credential.

**The version bump is scripted for the whole SDK family.** From the backend checkout,
`tools/sdkgen/release.sh X.Y.Z` raises the version in all eight SDKs (manifest, version constant,
lock files, the install lines of the READMEs), closes the `## [X.Y.Z] — Unreleased` (or
`## [Unreleased]`) section of every `CHANGELOG.md` with today's date, commits and tags `vX.Y.Z`
locally; `-n` only checks. Pushing the tag — the step that publishes — stays manual.

## What you need once

1. **A Central Portal account** at <https://central.sonatype.com> with the `com.oblodai` namespace
   verified (it is verified by a DNS TXT record on `oblodai.com`).
2. **A publishing token** from the portal (Account → Generate User Token). It gives a username and a
   password; they are not the portal login.
3. **A GPG key** whose public half is on a public keyserver:

   ```bash
   gpg --gen-key                                   # RSA 4096, no expiry is fine
   gpg --list-keys --keyid-format=short            # note the key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
   ```

4. **`~/.m2/settings.xml`** carrying the token under the server id the pom names:

   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>TOKEN_USERNAME</username>
         <password>TOKEN_PASSWORD</password>
       </server>
     </servers>
   </settings>
   ```

## Cutting a release

```bash
# 1. Every gate is green against the backend the release is generated from: generated code
#    matches its contract (drift, names.lock), build, lint, tests, conformance, package.
OBLODAI_BACKEND=../oblodai-backend make ci

# 2. Version, changelog, docs.
#    - pom.xml <version> and Oblodai.VERSION agree (make ci checks it).
#    - CHANGELOG.md gets the release's section and date; MIGRATION notes for a breaking change.

# 3. Publish.
mvn -Prelease deploy -Dgpg.keyname=<KEY_ID> -Dgpg.passphrase=<PASSPHRASE>
```

The deploy uploads a deployment bundle and stops at `validated`: nothing is public until a human
presses **Publish** in the Central portal. Check the bundle there — group, artifact, version, the
three jars, the signatures — and publish. It appears on `repo1.maven.org` within about half an hour
and in the portal's search within a few hours.

Then tag it:

```bash
git tag -a vX.Y.Z -m "Java SDK X.Y.Z"
git push origin vX.Y.Z
```

## In CI

The same command, with the key and the token in the job's secrets:

```yaml
- run: mvn -Prelease deploy -Dgpg.keyname=$GPG_KEY_ID
  env:
    MAVEN_GPG_PASSPHRASE: ${{ secrets.GPG_PASSPHRASE }}
    MAVEN_USERNAME: ${{ secrets.CENTRAL_TOKEN_USERNAME }}
    MAVEN_PASSWORD: ${{ secrets.CENTRAL_TOKEN_PASSWORD }}
```

Import the private key into the runner's keyring before the step (`gpg --batch --import`), and keep
`--pinentry-mode loopback` — which the profile already passes — so signing never waits for a prompt.

## Notes

- A version, once published, is immutable. A mistake is fixed by publishing the next patch version.
- `autoPublish` is deliberately `false`. Turning it on would make `mvn deploy` publish irrevocably,
  with no chance to look at the bundle first.
- The `release` profile also skips `maven-deploy-plugin`: the central plugin is what uploads, and
  running both would try to push to a repository that does not exist.
- Snapshots are not published anywhere. If you need one internally, deploy it to your own repository
  with `-DaltDeploymentRepository=...` outside this profile.
