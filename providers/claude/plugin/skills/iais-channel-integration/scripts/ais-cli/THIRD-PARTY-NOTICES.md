# Third-party notices

AIS CLI source is licensed under Apache-2.0. The executable JAR includes third-party software whose own licenses continue to apply.

| Component | Version | License | Upstream |
| --- | --- | --- | --- |
| Picocli | 4.7.7 | Apache-2.0 | https://github.com/remkop/picocli |
| FreeMarker | 2.3.34 | Apache-2.0 | https://freemarker.apache.org/ |
| Jackson annotations/core/databind | 2.19.2 | Apache-2.0 | https://github.com/FasterXML/jackson |
| JLine reader/terminal/terminal-jni/native | 3.26.3 | BSD-3-Clause | https://github.com/jline/jline3 |

The JLine license is retained at `src/main/resources/META-INF/licenses/jline-BSD-3-Clause.txt` in the source and at `META-INF/licenses/jline-BSD-3-Clause.txt` in the built executable JAR. Binary redistributors must retain it. It applies to the JLine native integration as well as its Java components. Source: [JLine 3.26.3 LICENSE.txt](https://github.com/jline/jline3/blob/jline-parent-3.26.3/LICENSE.txt).

Original dependency `META-INF/LICENSE`, `META-INF/NOTICE` and differently named license resources are retained in the executable JAR. Jackson Core's license also documents embedded FastDoubleParser (MIT) and BigInt (BSD-2-Clause) code. Their original notices must not be removed. Test-only JUnit and transitive test components are not bundled.

Maven verify writes the resolved runtime component list to `target/public-dependencies.txt`. A separate binary publisher should use the actual resolved artifacts to produce its dependency inventory and retain original notices; the Skill source archive does not include a generated CycloneDX file. An inventory is not a vulnerability scan or legal clearance. Review dependencies and retained notices whenever versions change. The platform SDK is excluded from public distribution and is not relicensed by this repository.
