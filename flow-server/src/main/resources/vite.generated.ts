/**
 * NOTICE: this is an auto-generated file
 *
 * This file has been generated by the `flow:prepare-frontend` maven goal.
 * This file will be overwritten on every run. Any custom changes should be made to vite.config.ts
 */
import path from 'path';
import * as net from 'net';

import { processThemeResources } from '@vaadin/application-theme-plugin/theme-handle.js';
import settings from '#settingsImport#';
import { UserConfigFn, defineConfig, HtmlTagDescriptor, mergeConfig } from 'vite';
import { injectManifest } from 'workbox-build';

import brotli from 'rollup-plugin-brotli';
import checker from 'vite-plugin-checker';


const frontendFolder = path.resolve(__dirname, settings.frontendFolder);
const themeFolder = path.resolve(frontendFolder, settings.themeFolder);
const frontendBundleFolder = path.resolve(__dirname, settings.frontendBundleOutput);
const addonFrontendFolder = path.resolve(__dirname, settings.addonFrontendFolder);

const projectStaticAssetsFolders = [
  path.resolve(__dirname, 'src', 'main', 'resources', 'META-INF', 'resources'),
  path.resolve(__dirname, 'src', 'main', 'resources', 'static'),
  frontendFolder
];

// Folders in the project which can contain application themes
const themeProjectFolders = projectStaticAssetsFolders.map((folder) => path.resolve(folder, settings.themeFolder));

const themeOptions = {
  devMode: false,
  // The following matches folder 'target/flow-frontend/themes/'
  // (not 'frontend/themes') for theme in JAR that is copied there
  themeResourceFolder: path.resolve(__dirname, settings.themeResourceFolder),
  themeProjectFolders: themeProjectFolders,
  projectStaticAssetsOutputFolder: path.resolve(__dirname, settings.staticOutput),
  frontendGeneratedFolder: path.resolve(frontendFolder, settings.generatedFolder)
};

// Block debug and trace logs.
console.trace = () => {};
console.debug = () => {};

function updateTheme(contextPath: string) {
  const themePath = path.resolve(themeFolder);
  if (contextPath.startsWith(themePath)) {
    const changed = path.relative(themePath, contextPath);

    console.debug('Theme file changed', changed);

    if (changed.startsWith(settings.themeName)) {
      processThemeResources(themeOptions, console);
    }
  }
}

function runWatchDog(watchDogPort) {
  const client = net.Socket();
  client.setEncoding('utf8');
  client.on('error', function () {
    console.log('Watchdog connection error. Terminating vite process...');
    client.destroy();
    process.exit(0);
  });
  client.on('close', function () {
    client.destroy();
    runWatchDog(watchDogPort);
  });

  client.connect(watchDogPort, 'localhost');
}

let spaMiddlewareForceRemoved = false;

const allowedFrontendFolders = [
  frontendFolder,
  addonFrontendFolder,
  path.resolve(addonFrontendFolder, '..', 'frontend'), // Contains only generated-flow-imports
  path.resolve(frontendFolder, '../node_modules')
];

export const vaadinConfig: UserConfigFn = (env) => {
  const devMode = env.mode === 'development';
  const basePath = env.mode === 'production' ? '' : '/VAADIN/';
  let pwaConfig;

  if (devMode && process.env.watchDogPort) {
    // Open a connection with the Java dev-mode handler in order to finish
    // vite when it exits or crashes.
    runWatchDog(process.env.watchDogPort);
  }
  return {
    root: 'frontend',
    base: basePath,
    resolve: {
      alias: {
        themes: themeFolder,
        Frontend: frontendFolder
      }
    },
    define: {
      // should be settings.offlinePath after manifests are fixed
      OFFLINE_PATH: "'.'"
    },
    server: {
      fs: {
        allow: allowedFrontendFolders,
      }
    },
    build: {
      outDir: frontendBundleFolder,
      assetsDir: 'VAADIN/build',
      rollupOptions: {
        input: {
          indexhtml: path.resolve(frontendFolder, 'index.html')
        }
      }
    },
    plugins: [
      !devMode && brotli(),
      settings.pwaEnabled &&
      {
        name: 'pwa',
        enforce: 'post',
        apply: 'build',
        async configResolved(config) {
          pwaConfig = config;
        },
        async buildStart() {
          // Before inject manifest we need to resolve and transpile the sw.ts file
          // This could probably be made another way which needs to be investigated

          // eslint-disable-next-line @typescript-eslint/no-var-requires
          const rollup = require('rollup') as typeof Rollup
          const includedPluginNames = [
            'alias',
            'vite:resolve',
            'vite:esbuild',
            'replace',
            'vite:define',
            'rollup-plugin-dynamic-import-variables',
            'vite:esbuild-transpile',
            'vite:terser',
          ]
          const plugins = pwaConfig.plugins.filter(p => includedPluginNames.includes(p.name)) as Plugin[]
          const bundle = await rollup.rollup({
            input: path.resolve(settings.clientServiceWorkerSource),
            plugins,
          })
          try {
            await bundle.write({
              format: 'es',
              exports: 'none',
              inlineDynamicImports: true,
              file: path.resolve(buildFolder, 'sw.js'),

            })
          }
          finally {
            await bundle.close()
          }
          // end of resolve and transpilation

          await injectManifest({
            swSrc: path.resolve(buildFolder, 'sw.js'),
            swDest: path.resolve(buildFolder, 'sw.js'),
            globDirectory: buildFolder,
            injectionPoint: 'self.__WB_MANIFEST',
          });
        }
      },
      {
        name: 'custom-theme',
        config() {
          processThemeResources(themeOptions, console);
        },
        handleHotUpdate(context) {
          updateTheme(path.resolve(context.file));
        }
      },
      {
        name: 'inject-entrypoint-script',
        transformIndexHtml: {
          enforce: 'pre',
          transform(_html, context) {
            if (context.server && !spaMiddlewareForceRemoved) {
              context.server.middlewares.stack = context.server.middlewares.stack.filter((mw) => {
                const handleName = '' + mw.handle;
                return !handleName.includes('viteSpaFallbackMiddleware');
              });
              spaMiddlewareForceRemoved = true;
            }

            if (context.path !== '/index.html') {
              return;
            }
            const vaadinScript: HtmlTagDescriptor = {
              tag: 'script',
              attrs: { type: 'module', src: devMode ? '/VAADIN/generated/vaadin.ts' : './generated/vaadin.ts' },
              injectTo: 'head'
            };

            let scripts = [vaadinScript];

            if (devMode) {
              const viteDevModeScript: HtmlTagDescriptor = {
                tag: 'script',
                attrs: { type: 'module', src: '/VAADIN/generated/vite-devmode.ts' },
                injectTo: 'head'
              };
              scripts.push(viteDevModeScript);
            }
            return scripts;
          }
        }
      },
      checker({
        typescript: true
      })
    ]
  };
};

export const overrideVaadinConfig = (customConfig: UserConfigFn) => {
  return defineConfig((env) => mergeConfig(vaadinConfig(env), customConfig(env)));
};
