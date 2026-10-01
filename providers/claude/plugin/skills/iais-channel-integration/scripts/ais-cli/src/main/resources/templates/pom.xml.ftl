<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- TODO[CHANNEL-01]: Replace these Maven coordinates with those of the developer's company. -->
    <groupId>[=groupId]</groupId>
    <artifactId>[=artifactId]</artifactId>
    <version>[=version]</version>
    <name>IAIS Channel Adapter Scaffold</name>
    <description>External channel SPI adapter for IAIS Channel Integration Hub</description>

    <!-- This standalone adapter does not inherit the platform parent POM; SDK and provided API versions must match the target host. -->
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <common-sdk.version>1.5.2</common-sdk.version>
        <spring.version>5.3.29</spring.version>
        <slf4j.version>1.7.32</slf4j.version>
        <fastjson.version>1.2.83_noneautotype</fastjson.version>
        <junit.version>5.10.2</junit.version>
        <mockito.version>4.11.0</mockito.version>
    </properties>

    <!-- Resolve the bundled SDK from this project; no SDK installation step is required. -->
    <repositories>
        <repository>
            <id>bundled-sdk</id>
            <url>file://${project.basedir}/lib/repository</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
    </repositories>

    <dependencies>
        <!--
          Compile against the SDK shipped by the target platform; the platform supplies it at runtime.
          Keep common-sdk.version and the bundled Jar/POM aligned with the platform SDK version.
        -->
        <dependency>
            <groupId>com.alipay.iacqintegrationhub</groupId>
            <artifactId>common-sdk</artifactId>
            <version>${common-sdk.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- The IAIS host provides these APIs at runtime; the adapter JAR must not bundle them. -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
            <version>${spring.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>${slf4j.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>${fastjson.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <parameters>true</parameters>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <useModulePath>false</useModulePath>
                    <failIfNoTests>true</failIfNoTests>
                </configuration>
            </plugin>
            <!-- Deliver a plain JAR without the host runtime; the manifest records the adapter and SDK versions used at build time. -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <Implementation-Title>${project.artifactId}</Implementation-Title>
                            <Implementation-Version>${project.version}</Implementation-Version>
                            <IAIS-SDK-Version>${common-sdk.version}</IAIS-SDK-Version>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
