Project that download video from streaming url.

## BUILD :
`mvn clean package`

## Configuration :
The project require a configuration file to be created and passed when executed.\
This file is used to provide the path to `ffmpeg`, `ffprobe`, define a `working directory` and a `series directory` where everything will go after processing it is also used to provide a log path to trace errors. 
```
path.ffmpeg=E:\\path\\to\\ffmpeg\\ffmpeg.exe
path.ffprobe=E:\\path\\to\\ffprobe\\ffprobe.exe
path.workdir=E:\\path\\to\\workdir
path.series=E:\\path\\to\\series
logs.file.error=E:\\path\\to\\errors.log
pocisalie.endpoint.login=https://pocisalie.xxx/login
pocisalie.endpoint.logout=https://pocisalie.xxx/logout
pocisalie.endpoint.upload=https://pocisalie.xxx/upload
pocisalie.cred.login=Username
pocisalie.cred.password=Password
```

## RUN : 
`java --module-path "C:\path_to\java_fx_lib\javafx-sdk-22\lib" --add-modules javafx.controls,javafx.fxml "-Dvideoquacker.properties=E:\Deploy\VideoQuacker\VideoQuacker.properties" -jar .\target\VideoQuacker-1.0-SNAPSHOT-jar-with-dependencies.jar`
