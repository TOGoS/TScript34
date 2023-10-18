## Accessing the GitHub package repo

You'll need to add something like the folllwing to `~/.m2/settings.xml`,
replacing `YourGitHubUsername` and `YourGitHubAccessToken`
with a personal access token, which you can generate at https://github.com/settings/tokens.

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd"
>
	<activeProfiles>
		<activeProfile>github</activeProfile>
	</activeProfiles>

	<profiles>
		<profile>
			<id>github</id>
			<repositories>
				<repository>
					<id>central</id>
					<url>https://repo1.maven.org/maven2</url>
				</repository>
				<repository>
					<id>github</id>
					<url>https://maven.pkg.github.com/TOGoS/tscript34-p0012-lib01</url>
					<snapshots>
						<enabled>true</enabled>
					</snapshots>
				</repository>
			</repositories>
		</profile>
	</profiles>

	<servers>
		<server>
			<id>github</id>
			<username>YourUsername</username>
			<password>YourGitHubAccessToken</password>
		</server>
	</servers>
</settings>
```

## Running the application

Try `mvn exec:java`.
