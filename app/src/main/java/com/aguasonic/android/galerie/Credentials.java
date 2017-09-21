//- ~ ©2015 Aguasonic Acoustics ~
package com.aguasonic.android.galerie;

//- Constants used for authentication.
//- Declaring as 'enum' is the best way to build a Singleton.
public enum Credentials {
    //- Never used -- but <must> declare at least one symbol to be an enumerated type.
    SINGLETON_INSTANCE;
    //- 'false', of course, means these are not authentic.
    static final Boolean enabled = false;
    static final String SENDER_ID = "123456789abc";
    //--------------------------------         1         2         3
    static final String MSG_PREFIX = "12345678901234567890123456789012";
    static final String MSG_KEY =
    //-       1         2         3         4         5         6
    "1234567890123456789012345678901234567890123456789012345678901234";
    static final String password_fullsize = "imputed";
    static final String password_thumbnails = "imputed";
}

//- ~ ©2015 Aguasonic Acoustics ~

