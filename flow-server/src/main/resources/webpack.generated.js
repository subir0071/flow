/**
 * NOTICE: this is an auto-generated file
 *
 * This file has been generated by the `flow:prepare-frontend` maven goal.
 * This file will be overwritten on every run. Any custom changes should be made to webpack.config.js
 */
const fs = require('fs');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');
const { InjectManifest } = require('workbox-webpack-plugin');
const { DefinePlugin } = require('webpack');
const ManifestPlugin = require('webpack-manifest-plugin');
const ExtraWatchWebpackPlugin = require('extra-watch-webpack-plugin');

// Flow plugins
const StatsPlugin = require('@vaadin/stats-plugin');
const ThemeLiveReloadPlugin = require('@vaadin/theme-live-reload-plugin');
const { ApplicationThemePlugin, processThemeResources, extractThemeName } = require('@vaadin/application-theme-plugin');

const path = require('path');

// this matches /themes/my-theme/ and is used to check css url handling and file path build.
const themePartRegex = /(\\|\/)themes\1[\s\S]*?\1/;

// the folder of app resources:
//  - flow templates for classic Flow
//  - client code with index.html and index.[ts/js] for CCDM
const frontendFolder = '[to-be-generated-by-flow]';
const frontendGeneratedFolder = '[to-be-generated-by-flow]';
const fileNameOfTheFlowGeneratedMainEntryPoint = '[to-be-generated-by-flow]';
const mavenOutputFolderForFlowBundledFiles = '[to-be-generated-by-flow]';
const mavenOutputFolderForResourceFiles = '[to-be-generated-by-flow]';
const useClientSideIndexFileForBootstrapping = '[to-be-generated-by-flow]';
const clientSideIndexHTML = '[to-be-generated-by-flow]';
const clientSideIndexEntryPoint = '[to-be-generated-by-flow]';
const devmodeGizmoJS = '[to-be-generated-by-flow]';
const pwaEnabled = false; // to be generated by flow;
const offlinePathEnabled = false; // to be generated by flow;
const offlinePath = '[to-be-generated-by-flow]';
const clientServiceWorkerEntryPoint = '[to-be-generated-by-flow]';
// public path for resources, must match Flow VAADIN_BUILD
const VAADIN = 'VAADIN';
const build = 'build';
// public path for resources, must match the request used in flow to get the
const config = '../config';
const outputFolder = mavenOutputFolderForFlowBundledFiles;
const indexHtmlPath = 'index.html';
// folder for outputting vaadin-bundle and other fragments
const buildFolder = path.resolve(outputFolder, VAADIN, build);
// folder for outputting stats.json
const confFolder = path.resolve(mavenOutputFolderForResourceFiles, 'config');
const serviceWorkerPath = 'sw.js';
// file which is used by flow to read templates for server `@Id` binding
const statsFile = `${confFolder}/stats.json`;

// Folders in the project which can contain static assets.
const projectStaticAssetsFolders = [
  path.resolve(__dirname, 'src', 'main', 'resources', 'META-INF', 'resources'),
  path.resolve(__dirname, 'src', 'main', 'resources', 'static'),
  frontendFolder
];

const projectStaticAssetsOutputFolder = [to-be-generated-by-flow];

// Folders in the project which can contain application themes
const themeProjectFolders = projectStaticAssetsFolders.map((folder) =>
  path.resolve(folder, 'themes')
);


// Target flow-fronted auto generated to be the actual target folder
const flowFrontendFolder = '[to-be-generated-by-flow]';

// make sure that build folder exists before outputting anything
const mkdirp = require('mkdirp');

const devMode = process.argv.find(v => v.indexOf('webpack-dev-server') >= 0);
if (!devMode) {
  // make sure that build folder exists before outputting anything
  const mkdirp = require('mkdirp');
  mkdirp(buildFolder);
  mkdirp(confFolder);
}

let stats;

// Open a connection with the Java dev-mode handler in order to finish
// webpack-dev-mode when it exits or crashes.
const watchDogPrefix = '--watchDogPort=';
let watchDogPort = devMode && process.argv.find(v => v.indexOf(watchDogPrefix) >= 0);
if (watchDogPort) {
  watchDogPort = watchDogPort.substr(watchDogPrefix.length);
  const runWatchDog = () => {
    const client = new require('net').Socket();
    client.setEncoding('utf8');
    client.on('error', function () {
      console.log("Watchdog connection error. Terminating webpack process...");
      client.destroy();
      process.exit(0);
    });
    client.on('close', function () {
      client.destroy();
      runWatchDog();
    });

    client.connect(watchDogPort, 'localhost');
  }
  runWatchDog();
}

// Compute the entries that webpack have to visit
const webPackEntries = {};
if (useClientSideIndexFileForBootstrapping) {
  webPackEntries.bundle = clientSideIndexEntryPoint;
  const dirName = path.dirname(fileNameOfTheFlowGeneratedMainEntryPoint);
  const baseName = path.basename(fileNameOfTheFlowGeneratedMainEntryPoint, '.js');
  if (fs.readdirSync(dirName).filter(fileName => !fileName.startsWith(baseName)).length) {
    // if there are vaadin exported views, add a second entry
    webPackEntries.export = fileNameOfTheFlowGeneratedMainEntryPoint;
  }
} else {
  webPackEntries.bundle = fileNameOfTheFlowGeneratedMainEntryPoint;
}

const appShellUrl = '.';

const swManifestTransform = (manifestEntries) => {
  const warnings = [];
  const manifest = manifestEntries;

  // `index.html` is a special case: in contrast with the JS bundles produced by webpack
  // it's not served as-is directly from the webpack output at `/index.html`.
  // It goes through IndexHtmlRequestHandler and is served at `/`.
  //
  // TODO: calculate the revision based on the IndexHtmlRequestHandler-processed content
  // of the index.html file
  const indexEntryIdx = manifest.findIndex(entry => entry.url === 'index.html');
  if (indexEntryIdx !== -1) {
    manifest[indexEntryIdx].url = appShellUrl;
  }

  return { manifest, warnings };
};

const serviceWorkerPlugin = new InjectManifest({
  swSrc: clientServiceWorkerEntryPoint,
  swDest: serviceWorkerPath,
  manifestTransforms: [swManifestTransform],
  maximumFileSizeToCacheInBytes: 100 * 1024 * 1024,
  dontCacheBustURLsMatching: /.*-[a-z0-9]{20}\.cache\.js/,
  include: [
    (chunk) => {
      return true;
    },
  ],
  webpackCompilationPlugins: [
    new DefinePlugin({
      OFFLINE_PATH_ENABLED: offlinePathEnabled,
      OFFLINE_PATH: JSON.stringify(offlinePath)
    }),
  ],
});

if (devMode) {
  webPackEntries.devmodeGizmo = devmodeGizmoJS;
}

const flowFrontendThemesFolder = path.resolve(flowFrontendFolder, 'themes');
const themeName = extractThemeName(flowFrontendThemesFolder);
const themeOptions = {
  devMode: devMode,
  // The following matches folder 'target/flow-frontend/themes/'
  // (not 'frontend/themes') for theme in JAR that is copied there
  themeResourceFolder: flowFrontendThemesFolder,
  themeProjectFolders: themeProjectFolders,
  projectStaticAssetsOutputFolder: projectStaticAssetsOutputFolder,
  frontendGeneratedFolder: frontendGeneratedFolder
};
const processThemeResourcesCallback = (logger) => processThemeResources(themeOptions, logger);

exports = {
  frontendFolder: `${frontendFolder}`,
  buildFolder: `${buildFolder}`,
  confFolder: `${confFolder}`
};

module.exports = {
  mode: 'production',
  context: frontendFolder,
  entry: webPackEntries,

  output: {
    filename: `${VAADIN}/${build}/vaadin-[name]-[contenthash].cache.js`,
    path: outputFolder
  },

  resolve: {
    // Search for import 'x/y' inside these folders, used at least for importing an application theme
    modules: [
      'node_modules',
      flowFrontendFolder,
      ...projectStaticAssetsFolders,
    ],
    extensions: [
      useClientSideIndexFileForBootstrapping && '.ts',
      '.js'
    ].filter(Boolean),
    alias: {
      Frontend: frontendFolder
    }
  },

  devServer: {
    // webpack-dev-server serves ./ ,  webpack-generated,  and java webapp
    contentBase: [outputFolder, 'src/main/webapp'],
    after: function(app, server) {
      app.get(`/stats.json`, function(req, res) {
        res.json(stats);
      });
      app.get(`/stats.hash`, function(req, res) {
        res.json(stats.hash.toString());
      });
      app.get(`/assetsByChunkName`, function(req, res) {
        res.json(stats.assetsByChunkName);
      });
      app.get(`/stop`, function(req, res) {
        // eslint-disable-next-line no-console
        console.log("Stopped 'webpack-dev-server'");
        process.exit(0);
      });
    }
  },

  module: {
    rules: [
      useClientSideIndexFileForBootstrapping && {
        test: /\.ts$/,
        loader: 'ts-loader'
      },
      {
        test: /\.css$/i,
        use: [
          {
            loader: "lit-css-loader"
          },
          {
            loader: "extract-loader"
          },
          {
            loader: 'css-loader',
            options: {
              url: (url, resourcePath) => {
                // Only translate files from node_modules
                const resolve = resourcePath.match(/(\\|\/)node_modules\1/);
                const themeResource = resourcePath.match(themePartRegex) && url.match(/^themes\/[\s\S]*?\//);
                return resolve || themeResource;
              },
              // use theme-loader to also handle any imports in css files
              importLoaders: 1
            },
          },
          {
            // theme-loader will change any url starting with './' to start with 'VAADIN/static' instead
            // NOTE! this loader should be here so it's run before css-loader as loaders are applied Right-To-Left
            loader: '@vaadin/theme-loader',
            options: {
              devMode: devMode
            }
          }
        ],
      },
      {
        // File-loader only copies files used as imports in .js files or handled by css-loader
        test: /\.(png|gif|jpg|jpeg|svg|eot|woff|woff2|otf|ttf)$/,
        use: [{
          loader: 'file-loader',
          options: {
            outputPath: 'VAADIN/static/',
            name(resourcePath, resourceQuery) {
              if (resourcePath.match(/(\\|\/)node_modules\1/)) {
                return /(\\|\/)node_modules\1(?!.*node_modules)([\S]+)/.exec(resourcePath)[2].replace(/\\/g, "/");
              }
              if (resourcePath.match(/(\\|\/)flow-frontend\1/)) {
                return /(\\|\/)flow-frontend\1(?!.*flow-frontend)([\S]+)/.exec(resourcePath)[2].replace(/\\/g, "/");
              }
              return '[path][name].[ext]';
            }
          }
        }],
      },
    ].filter(Boolean)
  },
  performance: {
    maxEntrypointSize: 2097152, // 2MB
    maxAssetSize: 2097152 // 2MB
  },
  plugins: [
    // Generate manifest.json file
    new ManifestPlugin(),

    new ApplicationThemePlugin(themeOptions),

    devMode && themeName && new ExtraWatchWebpackPlugin({
      files: [],
      dirs: [path.resolve(__dirname, 'frontend', 'themes', themeName),
        path.resolve(__dirname, 'src', 'main', 'resources', 'META-INF', 'resources', 'themes', themeName),
        path.resolve(__dirname, 'src', 'main', 'resources', 'static', 'themes', themeName)]
    }),

    devMode && themeName && new ThemeLiveReloadPlugin(themeName, processThemeResourcesCallback),

    new StatsPlugin({
      devMode: devMode,
      statsFile: statsFile,
      setResults: function (statsFile) {
        stats = statsFile;
      }
    }),

    // Includes JS output bundles into "index.html"
    useClientSideIndexFileForBootstrapping && new HtmlWebpackPlugin({
      template: clientSideIndexHTML,
      filename: indexHtmlPath,
      inject: 'head',
      scriptLoading: 'defer',
      chunks: ['bundle', ...(devMode ? ['devmodeGizmo'] : [])]
    }),

    // Service worker for offline
    pwaEnabled && serviceWorkerPlugin,

    // Generate compressed bundles when not devMode
    !devMode && new CompressionPlugin(),
  ].filter(Boolean)
};
