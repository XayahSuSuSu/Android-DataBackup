# Changelog

## [2.0.3](https://github.com/XayahSuSuSu/Android-DataBackup/compare/v2.0.2...v2.0.3) (2024-07-24)


### gradle

* Version 2.0.3 ([b44959c](https://github.com/XayahSuSuSu/Android-DataBackup/commit/b44959cb1bf3ce2ccf5c38875f59f1b37e46acdb))


### Features

* Downgrade minSdk to 24 ([#270](https://github.com/XayahSuSuSu/Android-DataBackup/issues/270)) ([f56a949](https://github.com/XayahSuSuSu/Android-DataBackup/commit/f56a949b3cf5c89fd5a82a589ded8554935d095e))
* Update translators ([63fd390](https://github.com/XayahSuSuSu/Android-DataBackup/commit/63fd390d8279a558d5b1601a91b16ae57aa2a22c))
* Update translators ([2de9898](https://github.com/XayahSuSuSu/Android-DataBackup/commit/2de98982f88ee207214e919ef8dab7780ab5746b))


### Bug Fixes

* [#267](https://github.com/XayahSuSuSu/Android-DataBackup/issues/267) ([cccd485](https://github.com/XayahSuSuSu/Android-DataBackup/commit/cccd485c58c157ffb850e12e2a6e14a78198888d))
* [SMB] 0 byte uploaded file ([a49031d](https://github.com/XayahSuSuSu/Android-DataBackup/commit/a49031d26368339e3cd51b2101b0ebf6d72a7105))
* Add compose lifecycle for crash screen ([689d27e](https://github.com/XayahSuSuSu/Android-DataBackup/commit/689d27e4a71031c1725ab3c72f1def7e155915b4))
* Add compose lifecycle for setup screen ([35e6f0b](https://github.com/XayahSuSuSu/Android-DataBackup/commit/35e6f0b4970739ba5441627e0a3d6e220d5c9a8f))
* Missing binaries on certain devices ([7aebbd7](https://github.com/XayahSuSuSu/Android-DataBackup/commit/7aebbd7db47337b94dd834fc7108488613c48bff))
* Try reduce applist lag and navigator issues ([#272](https://github.com/XayahSuSuSu/Android-DataBackup/issues/272)) ([0f66a8a](https://github.com/XayahSuSuSu/Android-DataBackup/commit/0f66a8a4202c9459b958fa98522e5a5337e8a9ba))
* Try to fix compose crash and some RemoteRootServiceImpl issues ([3ddb598](https://github.com/XayahSuSuSu/Android-DataBackup/commit/3ddb598d4f663501e7d41050ed5b9cacaf20dcd7))


### Performance Improvements

* Add leading icon for package apk/data chips ([6fa3601](https://github.com/XayahSuSuSu/Android-DataBackup/commit/6fa360169289eee5d4a8f4e2fd3fb48a05a115d1))
* Remove String, ImageVector and StringArray tokens ([#276](https://github.com/XayahSuSuSu/Android-DataBackup/issues/276)) ([458dddb](https://github.com/XayahSuSuSu/Android-DataBackup/commit/458dddbffa0cf8294d2ae7da31f1ac3a81f1c950))
* Update UIs ([553e808](https://github.com/XayahSuSuSu/Android-DataBackup/commit/553e808c2e9d32c291c3b687935c7f29b1c24f3b))

## [2.0.2](https://github.com/XayahSuSuSu/Android-DataBackup/compare/v2.0.1...v2.0.2) (2024-07-11)


### gradle

* Version 2.0.2 ([0e7a038](https://github.com/XayahSuSuSu/Android-DataBackup/commit/0e7a038548497aa71b3e5db13011f41bb7da9158))


### Features

* [#257](https://github.com/XayahSuSuSu/Android-DataBackup/issues/257) ([c7221af](https://github.com/XayahSuSuSu/Android-DataBackup/commit/c7221af08c8a657a23915685582050e7a0305c47))
* Add language selector ([#260](https://github.com/XayahSuSuSu/Android-DataBackup/issues/260)) ([14bdcb0](https://github.com/XayahSuSuSu/Android-DataBackup/commit/14bdcb0ac766b0a7cfd37280d993b8b67dc2b124))
* Implement update detection for premium build ([049be8a](https://github.com/XayahSuSuSu/Android-DataBackup/commit/049be8a1cfdf6a1525b210a4c2629a15e4aaa00c))
* Update translators ([8667f96](https://github.com/XayahSuSuSu/Android-DataBackup/commit/8667f962e65208c6485412071d7221238c2ba9fc))


### Bug Fixes

* [#245](https://github.com/XayahSuSuSu/Android-DataBackup/issues/245) ([4f65309](https://github.com/XayahSuSuSu/Android-DataBackup/commit/4f653092cf26b043f42697f0f47fd9d010f76b11))
* [Dialog] Incorrect dismiss state ([967cea8](https://github.com/XayahSuSuSu/Android-DataBackup/commit/967cea81f6f2fca33bd56eeb01fc6d69de2bebc3))
* [WebDAV] Https connection ([aa9312f](https://github.com/XayahSuSuSu/Android-DataBackup/commit/aa9312fb8884a6873278a3043abbf2dc06aeeab1))
* app theme being applied with delay ([#253](https://github.com/XayahSuSuSu/Android-DataBackup/issues/253)) ([a7b6c8d](https://github.com/XayahSuSuSu/Android-DataBackup/commit/a7b6c8dc7f2cd9e454ce542d23aa7eac672d8998))
* using collectImmediatelyAsState in all theme-related places ([#256](https://github.com/XayahSuSuSu/Android-DataBackup/issues/256)) ([f56a18f](https://github.com/XayahSuSuSu/Android-DataBackup/commit/f56a18f8ee1c6c5ff3f0c154572d840cddc2870b))

## [2.0.1](https://github.com/XayahSuSuSu/Android-DataBackup/compare/2.0.1...v2.0.1) (2024-07-06)


### Features

* :sparkles: Optimized UIs
* :sparkles: Added SFTP by @frknkrc44 in https://github.com/XayahSuSuSu/Android-DataBackup/pull/244
* :bug: Fixed logical error when backing up apps to the cloud ([2da30aa](https://github.com/XayahSuSuSu/Android-DataBackup/commit/2da30aa066a2b62fc7eb9612e0046ca56916fd60))
* :bug: ABI filter bug #247 by @frknkrc44 in https://github.com/XayahSuSuSu/Android-DataBackup/pull/248
* :bug: Filtered some storage entries by @frknkrc44 in https://github.com/XayahSuSuSu/Android-DataBackup/pull/250
* Add release-please ci ([033f341](https://github.com/XayahSuSuSu/Android-DataBackup/commit/033f34139eb8f29032b6d1b24c32a6a74b8499a4))
