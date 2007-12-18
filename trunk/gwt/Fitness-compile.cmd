@set GWTDIR=D:\Software\Java\gwt-windows-1.4.60
@java -cp "%~dp0src;%~dp0bin;%GWTDIR%\gwt-user.jar;%GWTDIR%\gwt-dev-windows.jar" com.google.gwt.dev.GWTCompiler -out "%~dp0www" %* fitness.Fitness
