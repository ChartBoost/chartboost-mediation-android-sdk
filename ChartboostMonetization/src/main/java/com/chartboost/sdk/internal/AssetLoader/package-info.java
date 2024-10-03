package com.chartboost.sdk.internal.AssetLoader;

/*

    Dependencies in this package look like this:

          Prefetcher       The Prefetcher issues prefetch requests
            |    |         and tells the Downloader what assets to then download.
            |    |
            |    |
            |    v
            |  Downloader  The Downloader downloads files and tells the AssetManager about them.
            |    |
            |    |
            |    |
            v    v         The AssetManager determines which templates are available,
          AssetManager     and builds finished html from templates+parameters.

    Dependencies are one-way, in part to avoid deadlocks.


    Asset Loader Block Diagram

      +--------------+                               +------------------------------+
      |              |                               |                              |
      |  Chartboost  |    prefetch() <---------------|        CBInterstitial        |
      |              |     |                         |                              |
      +--------------+     |                         +------------------------------+
               |           |                                   ^                   |
               |           |                                   |                   |
            prefetch()     |                        onAssetDownloadComplete()      |
            cancel()       |                              handleError()            |
               |           |                                   |                   |
               v           v                                   |                   |
      +----------------------+                                 |                   |
      |                      |                                 |                   |
      |      Prefetcher      |                      +-------------------------+    |
      |                      |------getAssets()---->|                         |    |
      +----------------------+                      |       AssetManager      |    |
                   |                                |                         |    |
                   |                                +-------------------------+    |
            downloadNativeVideos()                           ^                     |
            downloadWebViewAssets()                          |                     |
               cancel()`                             registerTemplate()            |
                   |                                  registerFile()        requestTemplate()
                   |                                 downloadFinished()            |
                   |                                  requestTemplate()            |
                   |                                    getAssets()                |
                   |                                         |                     |
                   |                                  +---------------------+      |
                   |                                  |                     |      |
                   +--------------------------------->|      Downloader     |<-----+
                                                      |                     |
                                                      +---------------------+
*/
