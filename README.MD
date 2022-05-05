MeoWrapper
============

MeoWrapper is a simple Launch-Wrapper for Minecraft. It downloads the games files for you and 
launches it. MeoWrapper is intended to be used for development projects where a Minecraft runtime
is needed (e.g. plugin development) or when you're having issues with your Minecraft launcher.

MeoWrapper can run in 3 environments:
- Client (Normal Minecraft)
- Data (Generation of data files; documentation for data packs)
- Server (Minecraft Server but not very useful)

You can either use MeoWrapper in a Java project or in command-line.

****

### Using MeoWrapper in a Java Project

To use MeoWrapper in your java project, you will first need to declare it as a dependency. You can
find artifacts on [Sonatype Releases Maven](https://s01.oss.sonatype.org/) or on [Maven Central](https://repo1.maven.org/maven2/)
at `io.github.lgatodu47.meowrapper`. If you are running on a Gradle project, your `build.gradle` 
should look like this:

    repositories {
        // Archives can sometimes be found on maven central
        // However, they aren't always up-to-date
        mavenCentral()

        // Should always work
        maven {
            url 'https://s01.oss.sonatype.org/content/repositories/releases'
        }
    }

    dependencies {
        runtimeOnly 'io.github.lgatodu47:meowrapper:1.3.0'
    }

We first declare the maven repository where the artifacts are located and then add the dependency
to the `runtimeOnly` configuration (you will almost never use the library's code in your project,
in which case you would declare it as an `implementation`). Just refresh your gradle project and 
the dependency should be there.

You will now need to create a run configuration to launch Minecraft. First assert that **you are
running on the same or higher Java version as Minecraft** (Java 17 for mc 1.18+, Java 16 for mc
1.17, Java 1.8 for older versions). Next set the main class to be MeoWrapper's main class (currently `io.github.lgatodu47.meowrapper.MeoWrapper`).
Finally, you should specify the run directory (Many files and folders will be created, running in
the project directory is not a good idea) and the run arguments. If you never used the program before 
you should start it with `help-<env>` to have more information about the arguments needed to run 
Minecraft on the specified environment. You can always add argument properties or environment 
variables if you wish.

Here is what an example configuration may look like on IntelliJ IDEA:
![A run configuration on IntelliJ IDEA](/images/intellij-run-configuration.png)

Once your run configuration is done, you should be able to run the game.

****

### Using MeoWrapper in Command Line

Go on the [releases page](https://github.com/LGatodu47/MeoWrapper/releases) and download the
latest release of MeoWrapper. Put the downloaded jar in an empty folder and open the command line
in the run folder. You can then run the jar with argument `help-<env>` to have more info about an
environment.  

If you are running on an old enough Windows 10 version, the console output may be messed up: this is probably because you
haven't enabled ANSI. To enable ANSI, run the command prompt as administrator and paste this line:  
`reg add HKEY_CURRENT_USER\Console /v VirtualTerminalLevel /t REG_DWORD /d 0x00000001 /f`  
If you want for some reason to disable ANSI, you can paste this line:  
`reg add HKEY_CURRENT_USER\Console /v VirtualTerminalLevel /t REG_DWORD /d 0x00000000 /f`

One more thing, to play minecraft 1.17+ you will need to install a newer version of Java, preferably **JDK 17**.
You can find many JDKs on websites such as [OpenJDK](https://jdk.java.net/17/) or [Eclipse Temurin](https://adoptium.net/temurin/archive).

****

### Mod loaders

Mod loaders are supported by MeoWrapper starting from 1.3. I tested Forge, Fabric and Optifine, but
MeoWrapper should have no issue handling other Mod loaders. Of course if you encounter an issue with mod loaders
feel free to open an issue on the GitHub page.

You can install a mod loader on client simply by specifying your minecraft home directory in the installer.
To launch the modded game you will need to go to '.minecraft/versions' and search for the version that has
the name of the mod loader with the minecraft version that it is installed for. Simply copy the name of that
folder and specify it in the `--version` argument. For example, you may set `--version 1.18.2-forge-40.1.0`
for forge 40.1 for minecraft 1.18.2.

Note that MeoWrapper doesn't handle mod loading on the server environment. The reason why is that most of the
case mod loaders installers directly download the server jar and sets up everything, making using MeoWrapper
useless.

****

And there you go! Don't forget that if you
find a bug or have issues launching the game you can open an issue on the [issue tracker](https://github.com/LGatodu47/MeoWrapper/issues).

*MeoWrapper is not affiliated with Minecraft, Mojang or Microsoft*  
*You can get Minecraft [here](https://www.minecraft.net/)*

## Have a question ?

You can email me at `driph.v450@gmail.com`.