SpeedBuildArena
===============

Build Instructions
------------------
    
1. Download BlocksHub and install that to the local Maven repository:
```
    wget https://github.com/SBPrime/BlocksHub/releases/download/v3.1.0/BlocksHub-3.1.0.jar
    mvn install:install-file -DgroupId=org.primesoft -DartifactId=blockshub -Dversion=3.1.0 \
        -Dpackaging=jar -Dfile=./BlocksHub-3.1.0.jar
```
2. Finally, compile this plugin:
```
    git clone https://github.com/NerdNu/SpeedBuildArena
    cd SpeedBuildArena
    mvn
```
