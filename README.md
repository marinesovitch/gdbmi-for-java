# gdbmi-for-java

## Introduction

Many years ago, I was involved (as a co-founder) in a startup called Win GDB. In brief, it was an add-in for Visual Studio allowing to debug programs with GDB in various scenarios (e.g. native Linux/Solaris applications on a remote host, embedded Linux on a connected device, etc.). Over time it evolved into a more versatile tool helping in other development stages like building or deploying. To drive GDB, it exploited [GDB/MI Interface](https://sourceware.org/gdb/onlinedocs/gdb/GDB_002fMI.html). Later, we also added support for Android (Java).

Instead of creating a new add-in meant strictly for Android, the solution was a command-line tool that would support GDB/MI Interface at the front-end, and drive Java debugging using [JDI](https://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/) at the back-end. This is how this project came about. From the Visual Studio add-in point of view, it was yet another GDB binary to drive.

It implements only a subset of GDB/MI commands which we call from VS Add-in. They are easy to find in code by pattern "^done," which is the standard prefix GDB returns in a command result while working in GDB/MI mode.

## Sources

I was the main developer of that module, although the co-founder was also involved in it. Therefore the sources, <u>**are not complete**</u>. Some parts (like expression parser) have been removed as not implemented by me. Hence it is buildable but can't run properly. Although, I thought it is still worth putting onto GitHub for my portfolio, and to prove I touched Java in the past ;-)

### The directory structure

* engine - the actual code in Java implementing GDB/MI commands, calls to JDI, and all related logic and data structures. Due to the reasons mentioned above the code is incomplete.
* debugger - a simple Java package containing only one routine wingdbJavaDebugger.Debugger.main, it starts the engine.
* launcher - a simple Windows console application written in C++ that starts the debugger. It is executed by Visual Studio Add-in. The code published here is incomplete, and can't be built.

### Dependencies

The project depends on [jython](https://www.jython.org/) and package [`com.sun.jdi`](https://docs.oracle.com/javase/7/docs/jdk/api/jpda/jdi/com/sun/jdi/package-summary.html).

### How to build

```bat
cd engine
set CLASSPATH=c:\jython2.7.2\jython.jar
javac -Xlint -classpath %CLASSPATH% *.java
```
