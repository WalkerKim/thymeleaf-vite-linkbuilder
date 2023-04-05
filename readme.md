# Thymeleaf Vite Link Builder

This Project provides a LinkBuilder for Thymeleaf to replace the path of static resources used in the View with the address of the compiled result of [Vite](https://vitejs.dev).

## How to use

```xml
<dependency>
    <groupId>kim.figure</groupId>
    <artifactId>thymeleaf-vite-link-builder</artifactId>
    <version>0.1.0</version>
</dependency>
```


```yml
implementation 'kim.figure:thymeleaf-vite-link-builder:1.0.0'
```

or use [vite-thymeleaf-spring-boot-starter](https://github.com/WalkerKim/vite-thymeleaf-spring-boot-starter)


## Usage
> recommand to use [vite-thymeleaf-spring-boot-starter](https://github.com/WalkerKim/vite-thymeleaf-spring-boot-starter)

### Spring Settings

#### Make BeanPostProcessor and add VitePathParseLinkBuilder

```java
public class ViteBeanPostProcessor implements BeanPostProcessor {
    private VitePathParseLinkBuilder vitePathParseLinkBuilder;

    ViteBeanPostProcessor(VitePathParseLinkBuilder vitePathParseLinkBuilder) {
        this.vitePathParseLinkBuilder = vitePathParseLinkBuilder;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof SpringTemplateEngine){
            SpringTemplateEngine springTemplateEngine = (SpringTemplateEngine)bean;
            Integer standardLinkBuilderOrder = vitePathParseLinkBuilder.getStandardLinkBuilderOrder();
            if (standardLinkBuilderOrder == null) {
                vitePathParseLinkBuilder.setOrder(1);
            } else {
                vitePathParseLinkBuilder.setOrder(standardLinkBuilderOrder-1);
            }
            springTemplateEngine.addLinkBuilder(vitePathParseLinkBuilder);
        }
        return bean;
    }

    public VitePathParseLinkBuilder getVitePathParseLinkBuilder(){
        return this.vitePathParseLinkBuilder;
    }


}
```

#### Add ViteBeanPostProcessor to SpringTemplateEngine

```java
@Configuration
public class ViteThymeleafTemplateEngineAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ViteThymeleafTemplateEngineAutoConfiguration.class);

    @Bean
    public ViteBeanPostProcessor addViteLinkBuilder(){
        //Vite manifest file that setting in vite.config.js
        File manifestFile = new File("/static/manifest.json");
        VitePathParseLinkBuilder vitePathParseLinkBuilder = null;
        try {
            vitePathParseLinkBuilder = new VitePathParseLinkBuilder(
                    new StandardLinkBuilder(),
                    new String[]{"css", "js"}, 
                    manifestFile //Vite manifest file
                    , true // whether to use Vite dev proxy server
            );
        } catch (IOException e) {
            throw new ViteManifestFileException(e.getMessage());
        }

        vitePathParseLinkBuilder.setOrder(1);
        return new ViteBeanPostProcessor(vitePathParseLinkBuilder);
    }
}
```

### Vite settings
vite.config.js
```js
// ...

// location of creating manifest.json
const outDir = path.relative(root, path.join(projectRoot, 'target/classes/static'))

//...

export default defineConfig({
    root,
    server: {
        //Vite dev server proxy settings for static assets
        proxy: {
            '/': {
                target: 'http://localhost:8080', //spring boot server
                bypass: function (req, res, proxyOptions) {
                    //static assets pattern
                    if (req.url.startsWith('/assets/js/') || req.url.startsWith('/assets/css/') || req.url.startsWith('/@')) {
                        return req.url;
                    }
                    return null;
                }
            }
        },
    },
    appType: "custom",
    build: {
        manifest: true,
        minify: false,
        outDir, // location of creating manifest.json
        assetsDir: "./",
        rollupOptions: {
            input: Object.fromEntries(entrypoints)
        }

    }
});

// ...

```

