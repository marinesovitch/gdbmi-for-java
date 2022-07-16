package wingdbJavaDebugEngine;

class FileUtils
{
    /*
        extracts file name
        samples:
        c:/source/main.cpp -> main.cpp
        Controller.java -> Controller.java
    */
    static String getFileName( String filePath )
    {
        String result = null;

        int slashIndex = filePath.lastIndexOf( '/' );
        int backslashIndex = filePath.lastIndexOf( '\\' );
        int separatorIndex = Math.max( slashIndex, backslashIndex );

        if ( separatorIndex != -1 )
            result = filePath.substring( separatorIndex + 1 );
        else
            result = filePath;
        return result;
    }

    /*
        extracts base name
        samples:
        c:/source/main.cpp -> main
        Controller.java -> Controller
    */
    static String getBaseName( String filePath )
    {
        String result = null;

        String fileName = getFileName( filePath );
        int dotIndex = fileName.indexOf( '.' );
        if ( dotIndex != -1 )
            result = fileName.substring( 0, dotIndex );
        else
            result = fileName;

        return result;
    }

    /*
        extracts extension
        samples:
        c:/source/main.cpp -> cpp
        Controller.java -> java
    */
    static String getExtension( String filePath )
    {
        String result = null;

        int dotIndex = filePath.lastIndexOf( '.' );
        if ( dotIndex != -1 )
            result = filePath.substring( dotIndex + 1 );
        else
            result = "";

        return result;
    }
}
