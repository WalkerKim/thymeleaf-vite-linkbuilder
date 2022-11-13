package kim.figure.vite.thymleaf.linkbuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Contexts;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.AbstractLinkBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;


/**
 * The type Vite path parse link builder.
 */
public class VitePathParseLinkBuilder extends AbstractLinkBuilder {

    private static final Logger logger = LoggerFactory.getLogger(VitePathParseLinkBuilder.class);


    /**
     * The Standard link builder.
     */
    AbstractLinkBuilder standardLinkBuilder;
    /**
     * The Object mapper.
     */
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The Asset manifest.
     */
    Map<String, Map<String, Object>> assetManifestMap;
    /**
     * The Manifest file.
     */
    File manifestFile;
    /**
     * The Compile asset extension set.
     */
    Set<String> compileAssetExtensionSet;

    /**
     * Whether to active Vite dev proxy settings
     */
    Boolean isViteDevProxyActive;

    /**
     * Instantiates a new Vite path parse link builder.
     *
     * @param standardLinkBuilder        the standard link builder. If you already use customLinkBuilder instead of standardLinkBuilder, you can inject it instead of standardLinkBuilder
     * @param compileAssetExtensionArray compile target assets extension array
     * @param manifestFile               the manifest.json file made by Vite build
     * @param isViteDevProxyActive      flag of active Vite dev server. If it set true Vite linkBuilder will be passed. You should set the path of vite config
     */
    public VitePathParseLinkBuilder(AbstractLinkBuilder standardLinkBuilder, String[] compileAssetExtensionArray, File manifestFile, Boolean isViteDevProxyActive ) throws IOException {
        Objects.requireNonNull(standardLinkBuilder, "standardLinkBuilder must not be null.");
        Objects.requireNonNull(compileAssetExtensionArray, "compileAssetExtensionArray must not be null.");
        if(isViteDevProxyActive){

        }else{
            Objects.requireNonNull(manifestFile, "manifestFile must not be null when build to production.");
        }
        Objects.requireNonNull(isViteDevProxyActive, "isViteDevProxyActive must not be null.");
        this.standardLinkBuilder = standardLinkBuilder;
        this.compileAssetExtensionSet = Arrays.asList(compileAssetExtensionArray).stream().map(String::toLowerCase).collect(Collectors.toSet());
        this.manifestFile = manifestFile;
        this.isViteDevProxyActive = isViteDevProxyActive;
        refreshManifestInformationByManifestJson();
    }


    public final String buildLink(final IExpressionContext context, final String base, final Map<String, Object> parameters) {
        if(isViteDevProxyActive){
            //If return null, this will be handled by StandardLinkBuilder
            return null;
        }

        String assetPathForCompareWithManifest;
        try {
            //remove queryString and '#'
            assetPathForCompareWithManifest = new URI(base).getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            //If return null, this will be handled by StandardLinkBuilder
            return null;
        }
        String assetName = assetPathForCompareWithManifest.substring(assetPathForCompareWithManifest.lastIndexOf('/') + 1);
        String extension = assetName.substring(assetName.lastIndexOf('.') + 1).toLowerCase();

        if (compileAssetExtensionSet.contains(extension.toLowerCase())) {

        } else {
            //request is not compile asset extensions
            //When it returns null, this will be handled by StandardLinkBuilder
            return null;
        }

        if (isLinkBaseAbsolute(base)) {
            //It is considered that external asset call
            return null;
        } else if (isLinkBaseContextRelative(base)) {
            //url is context relative. it is started with '/'
            //remove slash to compare
            assetPathForCompareWithManifest = assetPathForCompareWithManifest.substring(1, assetPathForCompareWithManifest.length());
        } else if (isLinkBaseServerRelative(base)) {
            //url is relative serverRelative it start with '~/'
            //remove slash to compare
            assetPathForCompareWithManifest = assetPathForCompareWithManifest.substring(2, assetPathForCompareWithManifest.length());
        } else {
            //Page-relative
            //page relative request cannot compare with manifest so make path with context root
            String requestPath = Contexts.getWebExchange(context).getRequest().getRequestPath();
            String urlWithoutLastPath = requestPath.substring(0, requestPath.lastIndexOf('/'));
            assetPathForCompareWithManifest = urlWithoutLastPath + "/" +base;
        }

        if (assetManifestMap.containsKey(assetPathForCompareWithManifest)) {
            String convertedBase = base.replace(assetPathForCompareWithManifest, assetManifestMap.get(assetPathForCompareWithManifest).get("file").toString());
            logger.debug("input base : `" + base + "` converted by manifest to " + convertedBase);
            return standardLinkBuilder.buildLink(context, convertedBase, parameters);
        } else {
            //When it returns null, this will be handled by StandardLinkBuilder
            return null;
        }
    }


    private static boolean isLinkBaseAbsolute(final CharSequence linkBase) {
        final int linkBaseLen = linkBase.length();
        if (linkBaseLen < 2) {
            return false;
        }
        final char c0 = linkBase.charAt(0);
        if (c0 == 'm' || c0 == 'M') {
            // Let's check for "mailto:"
            if (linkBase.length() >= 7 &&
                    Character.toLowerCase(linkBase.charAt(1)) == 'a' &&
                    Character.toLowerCase(linkBase.charAt(2)) == 'i' &&
                    Character.toLowerCase(linkBase.charAt(3)) == 'l' &&
                    Character.toLowerCase(linkBase.charAt(4)) == 't' &&
                    Character.toLowerCase(linkBase.charAt(5)) == 'o' &&
                    Character.toLowerCase(linkBase.charAt(6)) == ':') {
                return true;
            }
        } else if (c0 == '/') {
            return linkBase.charAt(1) == '/'; // It starts with '//' -> true, any other '/x' -> false
        }
        for (int i = 0; i < (linkBaseLen - 2); i++) {
            // Let's try to find the '://' sequence anywhere in the base --> true
            if (linkBase.charAt(i) == ':' && linkBase.charAt(i + 1) == '/' && linkBase.charAt(i + 2) == '/') {
                return true;
            }
        }
        return false;
    }

    private static boolean isLinkBaseContextRelative(final CharSequence linkBase) {
        // For this to be true, it should start with '/', but not with '//'
        if (linkBase.length() == 0 || linkBase.charAt(0) != '/') {
            return false;
        }
        return linkBase.length() == 1 || linkBase.charAt(1) != '/';
    }


    private static boolean isLinkBaseServerRelative(final CharSequence linkBase) {
        // For this to be true, it should start with '~/'
        return (linkBase.length() >= 2 && linkBase.charAt(0) == '~' && linkBase.charAt(1) == '/');
    }

    public Integer getStandardLinkBuilderOrder(){
        return this.standardLinkBuilder.getOrder();
    }

    public void refreshManifestInformationByManifestJson() throws IOException {
        if(isViteDevProxyActive){

        }else{
            if(manifestFile.exists()){

            }else{
                throw new ViteManifestFileNotFoundException("manifest.json file is not exists. check Vite build action");
            }
            try {
                assetManifestMap = objectMapper.readValue(manifestFile, new TypeReference<Map<String, Map<String, Object>>>() {
                });
            } catch (IOException e) {
                if(isViteDevProxyActive){
                    e.printStackTrace();
                }else{
                    e.printStackTrace();
                    throw e;

                }
            }
        }
    }


}
