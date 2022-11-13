package kim.figure.vite.thymleaf.linkbuilder;

import java.io.IOException;

/**
 * author         : walker
 * date           : 2022. 11. 13.
 * description    :
 */
public class ViteManifestFileNotFoundException extends IOException {
    public ViteManifestFileNotFoundException(){
        super();
    }
    public ViteManifestFileNotFoundException(String message){
        super(message);
    }
}
