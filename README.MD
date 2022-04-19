MeoWrapper
============

MeoWrapper is a simple Launch-Wrapper for Minecraft. It downloads the games files for you and 
launches it. MeoWrapper is intended to be used for development projects where a Minecraft runtime
is needed (e.g. plugin development) or when you're having issues with your Minecraft launcher.

MeoWrapper can run in 3 environments:
- Client (Normal Minecraft)
- Data (Generation of data files; documentation for data packs)
- Server (Minecraft Server)

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
        runtimeOnly 'io.github.lgatodu47:meowrapper:1.1.0'
    }

We first declare the maven repository where the artifacts are located and then add the dependency
to the `runtimeOnly` configuration (you will almost never use the library's code in your project,
in which case you would declare it as an `implementation`). Just refresh your gradle project and 
the dependency should be there.

You will now need to create a run configuration to launch Minecraft. First assert that **you are
running on Java 17** (However your project can be built on Java 1.8). Next set the main class to 
be MeoWrapper's main class (currently `io.github.lgatodu47.meowrapper.MeoWrapper`). Finally, you 
should specify the run directory (Many files and folders will be created, running in the project
directory is not a good idea) and the run arguments. If you never use the program you should start
it with `help-<env>` to have more information about the arguments needed to run Minecraft on the
specified environment. You can always add argument properties or environment variables if you wish.

Here is what an example configuration may look like on IntelliJ IDEA:
![A run configuration on IntelliJ IDEA](/images/intellij-run-configuration.png)

Once your run configuration is done, you should be able to run the game.

****

### Using MeoWrapper in Command Line

You will first need to install **JDK 17** if you haven't installed it yet as MeoWrapper is compiled
in Java 17. There are many JDKs out there such as [OpenJDK](https://jdk.java.net/17/) or [Eclipse Temurin](https://adoptium.net/temurin/archive).

Next go on the [releases page](https://github.com/LGatodu47/MeoWrapper/releases) and download the
latest release of MeoWrapper. Put the downloaded jar in an empty folder and open the command line
in the run folder. You can then run the jar with argument `help-<env>` to have more info about an
environment.  

If you are running on Windows, the console output may be messed up: this is probably because you
haven't enabled ANSI. To enable ANSI, run the command prompt as administrator and paste this line:  
`reg add HKEY_CURRENT_USER\Console /v VirtualTerminalLevel /t REG_DWORD /d 0x00000001 /f`  
If you want for some reason to disable ANSI, you can paste this line:  
`reg add HKEY_CURRENT_USER\Console /v VirtualTerminalLevel /t REG_DWORD /d 0x00000000 /f`

****

And there you go! You can now launch Minecraft in your Java projects. Don't forget that if you
find a bug or have issues launching the game you can open an issue on the [issue tracker](https://github.com/LGatodu47/MeoWrapper/issues).

**MeoWrapper should not be used to play Minecraft for free! Remember that pirating is bad!**

*MeoWrapper is not affiliated with Minecraft, Mojang or Microsoft*  
*You can get Minecraft [here](https://www.minecraft.net/)*

## Have a question ?

You can email me at `driph.v450@gmail.com`.