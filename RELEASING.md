# Releasing to Maven Central

Everything Central needs lives in the `release` profile of `pom.xml`. An ordinary build does not
touch it: `mvn verify` on a machine with no Sonatype account and no GPG key produces the jar, the
sources jar and the javadoc jar, and never asks for a credential.

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
# 1. The contract snapshot is current and the generated sources match it.
codegen/run.sh --check

# 2. Everything is green, including the live tier against a gateway.
mvn -o verify
OBLODAI_LIVE_URL=http://127.0.0.1:8095 mvn -o verify

# 3. Version, changelog, docs.
#    - pom.xml <version> and Oblodai.VERSION must agree; a test would not catch a mismatch.
#    - CHANGELOG.md gets the release's section and date.
#    - README/AGENTS route and error-code counts match contract/contract.json.

# 4. Publish.
mvn -Prelease deploy -Dgpg.keyname=<KEY_ID> -Dgpg.passphrase=<PASSPHRASE>
```

The deploy uploads a deployment bundle and stops at `validated`: nothing is public until a human
presses **Publish** in the Central portal. Check the bundle there — group, artifact, version, the
three jars, the signatures — and publish. It appears on `repo1.maven.org` within about half an hour
and in the portal's search within a few hours.

Then tag it:

```bash
git tag -a v1.3.0 -m "Java SDK 1.3.0"
git push origin v1.3.0
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
